package de.androidcrypto.ntag424sunfeature;

import static net.bplearning.ntag424.constants.Ntag424.CC_FILE_NUMBER;
import static net.bplearning.ntag424.constants.Ntag424.DATA_FILE_NUMBER;
import static net.bplearning.ntag424.constants.Ntag424.NDEF_FILE_NUMBER;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_EVERYONE;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY0;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY1;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY2;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY3;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY4;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_NONE;

import static de.androidcrypto.ntag424sunfeature.Utils.printData;

import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.bplearning.ntag424.DnaCommunicator;
import net.bplearning.ntag424.command.ChangeFileSettings;
import net.bplearning.ntag424.command.FileSettings;
import net.bplearning.ntag424.command.GetFileSettings;
import net.bplearning.ntag424.command.ReadData;
import net.bplearning.ntag424.command.WriteData;
import net.bplearning.ntag424.constants.Ntag424;
import net.bplearning.ntag424.encryptionmode.AESEncryptionMode;
import net.bplearning.ntag424.encryptionmode.LRPEncryptionMode;
import net.bplearning.ntag424.sdm.NdefTemplateMaster;
import net.bplearning.ntag424.sdm.SDMSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TagOverviewActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = TagOverviewActivity.class.getSimpleName();
    private com.google.android.material.textfield.TextInputEditText output;
    private RadioButton rbUid, rbCounter, rbUidCounter;
    private DnaCommunicator dnaC = new DnaCommunicator();
    private NfcAdapter mNfcAdapter;
    private IsoDep isoDep;
    private byte[] tagIdByte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tag_overview);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        output = findViewById(R.id.etOutput);
        rbUid = findViewById(R.id.rbFieldUid);
        rbCounter = findViewById(R.id.rbFieldCounter);
        rbUidCounter = findViewById(R.id.rbFieldUidCounter);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    /**
     * section for UI handling
     */

    private void writeToUiAppend(TextView textView, String message) {
        runOnUiThread(() -> {
            String oldString = textView.getText().toString();
            if (TextUtils.isEmpty(oldString)) {
                textView.setText(message);
            } else {
                String newString = message + "\n" + oldString;
                textView.setText(newString);
                System.out.println(message);
            }
        });
    }

    private void vibrateShort() {
        // Make a Sound
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(50, 10));
        } else {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(50);
        }
    }

    /**
     * NFC tag handling section
     * These methods are running in another thread when a card is discovered and
     * cannot direct interact with the UI Thread.
     * Use `runOnUiThread` method to change the UI from these methods
     */

    @Override
    public void onTagDiscovered(Tag tag) {

        writeToUiAppend(output, "NFC tag discovered");

        isoDep = null;
        try {
            isoDep = IsoDep.get(tag);
            if (isoDep != null) {
                // Make a Vibration
                vibrateShort();

                runOnUiThread(() -> {
                    output.setText("");
                });

                isoDep.connect();
                if (!isoDep.isConnected()) {
                    writeToUiAppend(output, "Could not connect to the tag, aborted");
                    isoDep.close();
                    return;
                }

                // get tag ID
                tagIdByte = tag.getId();
                writeToUiAppend(output, "Tag ID: " + Utils.bytesToHex(tagIdByte));
                Log.d(TAG, "tag id: " + Utils.bytesToHex(tagIdByte));
                writeToUiAppend(output, "NFC tag connected");

                runWorker();
            }

        } catch (IOException e) {
            writeToUiAppend(output, "ERROR: IOException " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            writeToUiAppend(output, "ERROR: Exception " + e.getMessage());
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for NFC A card type and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is set
            // so the reader won't try to get a NDEF message
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(this);
        }
    }

    private void runWorker() {
        Log.d(TAG, "Tag Overview Activity Worker");
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                try {
                    dnaC = new DnaCommunicator();
                    try {
                        dnaC.setTransceiver((bytesToSend) -> isoDep.transceive(bytesToSend));
                    } catch (NullPointerException npe) {
                        writeToUiAppend(output, "Please tap a tag before running any tests, aborted");
                        return;
                    }
                    dnaC.setLogger((info) -> Log.d(TAG, "Communicator: " + info));
                    dnaC.beginCommunication();

                    /**
                     * These steps are running - this activity tries to get an overview about the tag
                     *
                     * assuming that all keys are 'default' keys filled with 16 00h values
                     * 1) Authenticate with Application Key 00h in AES mode
                     * 2) If the authentication in AES mode fails try to authenticate in LRP mode
                     * 3) Write an URL template to file 02 with Uid and/or Counter plus CMAC
                     * 4) Get existing file settings for file 02
                     * 5) Save the modified file settings back to the tag
                     */

                    /**
                     * Note: the library version has an issue in retrieving the file settings:
                     * it should work without previous authentication but actually needs an authentication with any key.
                     * I'm using the AUTH_KEY0 for this task, get the file settings for file 2 and then run the
                     * authentication again with the RW key.
                     */

                    writeToUiAppend(output, Constants.DOUBLE_DIVIDER);
                    // authentication
                    boolean isLrpAuthenticationMode = false;

                    writeToUiAppend(output, "Authentication with FACTORY ACCESS_KEY 0");
                    // what happens when we choose the wrong authentication scheme ?
                    success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                    if (success) {
                        writeToUiAppend(output, "AES Authentication SUCCESS");
                    } else {
                        // if the returnCode is '919d' = permission denied the tag is in LRP mode authentication
                        if (Arrays.equals(dnaC.returnCode, Constants.PERMISSION_DENIED_ERROR)) {
                            // try to run the LRP authentication
                            success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                            if (success) {
                                writeToUiAppend(output, "LRP Authentication SUCCESS");
                                isLrpAuthenticationMode = true;
                            } else {
                                writeToUiAppend(output, "LRP Authentication FAILURE");
                                writeToUiAppend(output, Utils.printData("returnCode is", dnaC.returnCode));
                                writeToUiAppend(output, "Authentication not possible, Operation aborted");
                                return;
                            }
                        } else {
                            // any other error, print the error code and return
                            writeToUiAppend(output, "AES Authentication FAILURE");
                            writeToUiAppend(output, Utils.printData("returnCode is", dnaC.returnCode));
                            return;
                        }
                    }

                    writeToUiAppend(output, Constants.SINGLE_DIVIDER);
                    // check all other application keys (1..4) if they are FACTORY or CUSTOM
                    int key1State = 0; // 0 = no auth, 1 = FACTORY key SUCCESS, 2 = CUSTOM key SUCCESS, 3 = UNKNOWN key, failure
                    int key2State = 0;
                    int key3State = 0;
                    int key4State = 0;
                    if (!isLrpAuthenticationMode) {
                        // app key 1
                        success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY1, Ntag424.FACTORY_KEY);
                        if (success) {
                            writeToUiAppend(output, "App Key 1 is FACTORY key");
                            key1State = 1;
                        } else {
                            // try to authenticate with custom key
                            success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY1, Constants.APPLICATION_KEY_1);
                            if (success) {
                                writeToUiAppend(output, "App Key 1 is CUSTOM key");
                                key1State = 2;
                            } else {
                                writeToUiAppend(output, "App Key 1 has UNKNOWN key");
                                key1State = 3;
                            }
                        }
                        // app key 2
                        success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY2, Ntag424.FACTORY_KEY);
                        if (success) {
                            writeToUiAppend(output, "App Key 2 is FACTORY key");
                            key2State = 1;
                        } else {
                            // try to authenticate with custom key
                            success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY2, Constants.APPLICATION_KEY_2);
                            if (success) {
                                writeToUiAppend(output, "App Key 2 is CUSTOM key");
                                key2State = 2;
                            } else {
                                writeToUiAppend(output, "App Key 2 has UNKNOWN key");
                                key2State = 3;
                            }
                        }
                        // app key 3
                        success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY3, Ntag424.FACTORY_KEY);
                        if (success) {
                            writeToUiAppend(output, "App Key 3 is FACTORY key");
                            key3State = 1;
                        } else {
                            // try to authenticate with custom key
                            success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY3, Constants.APPLICATION_KEY_3);
                            if (success) {
                                writeToUiAppend(output, "App Key 3 is CUSTOM key");
                                key3State = 2;
                            } else {
                                writeToUiAppend(output, "App Key 3 has UNKNOWN key");
                                key3State = 3;
                            }
                        }
                        // app key 4
                        success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY4, Ntag424.FACTORY_KEY);
                        if (success) {
                            writeToUiAppend(output, "App Key 4 is FACTORY key");
                            key4State = 1;
                        } else {
                            // try to authenticate with custom key
                            success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY4, Constants.APPLICATION_KEY_4);
                            if (success) {
                                writeToUiAppend(output, "App Key 4 is CUSTOM key");
                                key4State = 2;
                            } else {
                                writeToUiAppend(output, "App Key 4 has UNKNOWN key");
                                key4State = 3;
                            }
                        }
                    } else {
                        // app key 1
                        success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY1, Ntag424.FACTORY_KEY);
                        if (success) {
                            writeToUiAppend(output, "App Key 1 is FACTORY key");
                            key1State = 1;
                        } else {
                            // try to authenticate with custom key
                            success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY1, Constants.APPLICATION_KEY_1);
                            if (success) {
                                writeToUiAppend(output, "App Key 1 is CUSTOM key");
                                key1State = 2;
                            } else {
                                writeToUiAppend(output, "App Key 1 has UNKNOWN key");
                                key1State = 3;
                            }
                        }
                        // app key 2
                        success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY2, Ntag424.FACTORY_KEY);
                        if (success) {
                            writeToUiAppend(output, "App Key 2 is FACTORY key");
                            key2State = 1;
                        } else {
                            // try to authenticate with custom key
                            success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY2, Constants.APPLICATION_KEY_2);
                            if (success) {
                                writeToUiAppend(output, "App Key 2 is CUSTOM key");
                                key2State = 2;
                            } else {
                                writeToUiAppend(output, "App Key 2 has UNKNOWN key");
                                key2State = 3;
                            }
                        }
                        // app key 3
                        success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY3, Ntag424.FACTORY_KEY);
                        if (success) {
                            writeToUiAppend(output, "App Key 3 is FACTORY key");
                            key3State = 1;
                        } else {
                            // try to authenticate with custom key
                            success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY3, Constants.APPLICATION_KEY_3);
                            if (success) {
                                writeToUiAppend(output, "App Key 3 is CUSTOM key");
                                key3State = 2;
                            } else {
                                writeToUiAppend(output, "App Key 3 has UNKNOWN key");
                                key3State = 3;
                            }
                        }
                        // app key 4
                        success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY4, Ntag424.FACTORY_KEY);
                        if (success) {
                            writeToUiAppend(output, "App Key 4 is FACTORY key");
                            key4State = 1;
                        } else {
                            // try to authenticate with custom key
                            success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY4, Constants.APPLICATION_KEY_4);
                            if (success) {
                                writeToUiAppend(output, "App Key 4 is CUSTOM key");
                                key4State = 2;
                            } else {
                                writeToUiAppend(output, "App Key 4 has UNKNOWN key");
                                key4State = 3;
                            }
                        }
                    }
                    writeToUiAppend(output, Constants.DOUBLE_DIVIDER);

                    // silent authenticate with Access Key 0, should work
                    if (!isLrpAuthenticationMode) {
                        success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                    } else {
                        success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                    }
                    if (!success) {
                        writeToUiAppend(output, "Error on Authentication with ACCESS KEY 0, aborted");
                        return;
                    }
                    int lastAuthKeyNumber = 0;

                    // get the file settings
                    writeToUiAppend(output, "Get the File Settings");
                    FileSettings fileSettings01;
                    try {
                        fileSettings01 = GetFileSettings.run(dnaC, CC_FILE_NUMBER);
                    } catch (Exception e) {
                        Log.e(TAG, "getFileSettings File 01 Exception: " + e.getMessage());
                        writeToUiAppend(output, "getFileSettings File 01 Exception: " + e.getMessage());
                        return;
                    }
                    writeToUiAppend(output, DnacFileSettingsDumper.run(CC_FILE_NUMBER, fileSettings01));
                    writeToUiAppend(output, Constants.SINGLE_DIVIDER);

                    FileSettings fileSettings02;
                    try {
                        fileSettings02 = GetFileSettings.run(dnaC, NDEF_FILE_NUMBER);
                    } catch (Exception e) {
                        Log.e(TAG, "getFileSettings File 02 Exception: " + e.getMessage());
                        writeToUiAppend(output, "getFileSettings File 02 Exception: " + e.getMessage());
                        return;
                    }
                    writeToUiAppend(output, DnacFileSettingsDumper.run(NDEF_FILE_NUMBER, fileSettings02));
                    writeToUiAppend(output, Constants.SINGLE_DIVIDER);

                    FileSettings fileSettings03;
                    try {
                        fileSettings03 = GetFileSettings.run(dnaC, DATA_FILE_NUMBER);
                    } catch (Exception e) {
                        Log.e(TAG, "getFileSettings File 03 Exception: " + e.getMessage());
                        writeToUiAppend(output, "getFileSettings File 03 Exception: " + e.getMessage());
                        return;
                    }
                    writeToUiAppend(output, DnacFileSettingsDumper.run(DATA_FILE_NUMBER, fileSettings03));
                    writeToUiAppend(output, Constants.DOUBLE_DIVIDER);

                    // read the content of each file
                    // check which key in required to read the file
                    int file01RAccess = fileSettings01.readPerm;
                    if (file01RAccess == ACCESS_EVERYONE) {
                        // do not need to run any authentication
                    } else {
                        // authenticate with file01RAccess key
                        if (file01RAccess != lastAuthKeyNumber) {
                            // the requested key is different from the last auth key
                            // did we had a successful authentication with this key ? with FACTORY or CUSTOM key ?

                            // todo check for keyXState 1/2/3

                            if (!isLrpAuthenticationMode) {
                                success = AESEncryptionMode.authenticateEV2(dnaC, file01RAccess, Ntag424.FACTORY_KEY);
                            } else {
                                success = LRPEncryptionMode.authenticateLRP(dnaC, file01RAccess, Ntag424.FACTORY_KEY);
                            }
                            if (!success) {
                                writeToUiAppend(output, "Error on Authentication with key " + file01RAccess  + ", aborted");
                                return;
                            }
                            lastAuthKeyNumber = file01RAccess;
                        }
                    }
                    byte[] fileContent01 = runReadData(CC_FILE_NUMBER, 0, 32);
                    writeToUiAppend(output, Utils.printData("content of file 01", fileContent01));
                    writeToUiAppend(output, Constants.SINGLE_DIVIDER);

                    // check which key in required to read the file
                    int file02RAccess = fileSettings02.readPerm;
                    if (file02RAccess == ACCESS_EVERYONE) {
                        // do not need to run any authentication
                    } else {
                        // authenticate with file02RAccess key
                        if (file02RAccess != lastAuthKeyNumber) {
                            // the requested key is different from the last auth key
                            // did we had a successful authentication with this key ? with FACTORY or CUSTOM key ?

                            // todo check for keyXState 1/2/3

                            if (!isLrpAuthenticationMode) {
                                success = AESEncryptionMode.authenticateEV2(dnaC, file02RAccess, Ntag424.FACTORY_KEY);
                            } else {
                                success = LRPEncryptionMode.authenticateLRP(dnaC, file02RAccess, Ntag424.FACTORY_KEY);
                            }
                            if (!success) {
                                writeToUiAppend(output, "Error on Authentication with key " + file02RAccess  + ", aborted");
                                return;
                            }
                            lastAuthKeyNumber = file02RAccess;
                        }
                    }
                    byte[] fileContent02 = runReadData(NDEF_FILE_NUMBER, 0, 256);
                    writeToUiAppend(output, Utils.printData("content of file 02", fileContent02));
                    writeToUiAppend(output,"");
                    writeToUiAppend(output, "ASCII Data: " + new String(fileContent02, StandardCharsets.UTF_8));
                    writeToUiAppend(output, Constants.SINGLE_DIVIDER);

                    // check which key in required to read the file
                    int file03RAccess = fileSettings03.readPerm;
                    if (file03RAccess == ACCESS_EVERYONE) {
                        // do not need to run any authentication
                    } else {
                        // authenticate with file03RAccess key
                        if (file03RAccess != lastAuthKeyNumber) {
                            // the requested key is different from the last auth key
                            // did we had a successful authentication with this key ? with FACTORY or CUSTOM key ?

                            // todo check for keyXState 1/2/3

                            if (!isLrpAuthenticationMode) {
                                success = AESEncryptionMode.authenticateEV2(dnaC, file03RAccess, Ntag424.FACTORY_KEY);
                            } else {
                                success = LRPEncryptionMode.authenticateLRP(dnaC, file03RAccess, Ntag424.FACTORY_KEY);
                            }
                            if (!success) {
                                writeToUiAppend(output, "Error on Authentication with key " + file03RAccess  + ", aborted");
                                return;
                            }
                            lastAuthKeyNumber = file03RAccess;
                        }
                    }
                    byte[] fileContent03 = runReadData( DATA_FILE_NUMBER, 0, 128);
                    writeToUiAppend(output, Utils.printData("content of file 03", fileContent03));
                    writeToUiAppend(output, Constants.SINGLE_DIVIDER);



/*
                    // get File Settings for File 2 to get the key number necessary for writing (key 0 or key 2 ?)
                    FileSettings fileSettings02 = null;
                    try {
                        fileSettings02 = GetFileSettings.run(dnaC, NDEF_FILE_NUMBER);
                    } catch (Exception e) {
                        Log.e(TAG, "getFileSettings File 02 Exception: " + e.getMessage());
                        writeToUiAppend(output, "getFileSettings File 02 Exception: " + e.getMessage());
                    }
                    if (fileSettings02 == null) {
                        Log.e(TAG, "getFileSettings File 02 Error, Operation aborted");
                        writeToUiAppend(output, "getFileSettings File 02 Error, Operation aborted");
                        return;
                    }
                    int ACCESS_KEY_RW = fileSettings02.readWritePerm;
                    int ACCESS_KEY_CAR = fileSettings02.changePerm; // we do need this information later when changing the file settings
                    writeToUiAppend(output, "getFileSettings File 02 AUTH-KEY RW Is: " + ACCESS_KEY_RW);

                    // in fabric settings or after unset the RW Access Key is 'Eh' = 14 meaning free read and write access
                    // we have to skip an authentication with this key as it does not exist !
                    if (ACCESS_KEY_RW == 14) {
                        // do nothing, skip authentication
                    } else {
                        if (ACCESS_KEY_RW != ACCESS_KEY0) {
                            //success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                            success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY_RW, Ntag424.FACTORY_KEY);
                            //success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY4, Ntag424.FACTORY_KEY);
                            if (success) {
                                writeToUiAppend(output, "AES Authentication SUCCESS");
                            } else {
                                writeToUiAppend(output, "AES Authentication FAILURE");
                                writeToUiAppend(output, "Trying to authenticate in LRP mode");
                                //success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                                success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY_RW, Ntag424.FACTORY_KEY);
                                if (success) {
                                    writeToUiAppend(output, "LRP Authentication SUCCESS");
                                    isLrpAuthenticationMode = true;
                                } else {
                                    writeToUiAppend(output, "LRP Authentication FAILURE");
                                    writeToUiAppend(output, "Authentication not possible, Operation aborted");
                                    return;
                                }
                            }
                        }
                    }

                    // write URL template to file 02 depending on radio button
                    SDMSettings sdmSettings = new SDMSettings();
                    sdmSettings.sdmEnabled = true; // at this point we are just preparing the templated but do not enable the SUN/SDM feature
                    sdmSettings.sdmMetaReadPerm = ACCESS_EVERYONE; // Set to a key to get encrypted PICC data
                    sdmSettings.sdmFileReadPerm = ACCESS_KEY2;     // Used to create the MAC and Encrypted File data
                    sdmSettings.sdmReadCounterRetrievalPerm = ACCESS_NONE; // Not sure what this is for
                    sdmSettings.sdmOptionEncryptFileData = false;
                    byte[] ndefRecord = null;
                    NdefTemplateMaster master = new NdefTemplateMaster();
                    master.usesLRP = isLrpAuthenticationMode;
                    master.fileDataLength = 0; // no (encrypted) file data
                    if (rbUid.isChecked()) {
                        ndefRecord = master.generateNdefTemplateFromUrlString("https://sdm.nfcdeveloper.com/tagpt?uid={UID}&cmac={MAC}", sdmSettings);
                    } else if (rbCounter.isChecked()) {
                        ndefRecord = master.generateNdefTemplateFromUrlString("https://sdm.nfcdeveloper.com/tagpt?ctr={COUNTER}&cmac={MAC}", sdmSettings);
                    } else {
                        ndefRecord = master.generateNdefTemplateFromUrlString("https://sdm.nfcdeveloper.com/tagpt?uid={UID}&ctr={COUNTER}&cmac={MAC}", sdmSettings);
                    }
                    try {
                        WriteData.run(dnaC, NDEF_FILE_NUMBER, ndefRecord, 0);
                    } catch (IOException e) {
                        Log.e(TAG, "writeData IOException: " + e.getMessage());
                        writeToUiAppend(output, "File 02h writeDataIOException: " + e.getMessage());
                        writeToUiAppend(output, "Writing the NDEF URL Template FAILURE, Operation aborted");
                        return;
                    }
                    writeToUiAppend(output, "File 02h Writing the NDEF URL Template SUCCESS");

                    // check if we authenticated with the right key - here we need the CAR key
                    if (ACCESS_KEY_CAR != ACCESS_KEY_RW) {
                        success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY_CAR, Ntag424.FACTORY_KEY);
                        //success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY4, Ntag424.FACTORY_KEY);
                        if (success) {
                            writeToUiAppend(output, "AES Authentication SUCCESS");
                        } else {
                            writeToUiAppend(output, "AES Authentication FAILURE");
                            writeToUiAppend(output, "Trying to authenticate in LRP mode");
                            //success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                            success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY_CAR, Ntag424.FACTORY_KEY);
                            if (success) {
                                writeToUiAppend(output, "LRP Authentication SUCCESS");
                                isLrpAuthenticationMode = true;
                            } else {
                                writeToUiAppend(output, "LRP Authentication FAILURE");
                                writeToUiAppend(output, "Authentication not possible, Operation aborted");
                                return;
                            }
                        }
                    }

                    // change the auth key settings
                    fileSettings02.sdmSettings = sdmSettings;
                    fileSettings02.readWritePerm = ACCESS_KEY2;
                    fileSettings02.changePerm = ACCESS_KEY0;
                    fileSettings02.readPerm = ACCESS_EVERYONE;
                    fileSettings02.writePerm = ACCESS_KEY2;
                    try {
                        ChangeFileSettings.run(dnaC, NDEF_FILE_NUMBER, fileSettings02);
                    } catch (IOException e) {
                        Log.e(TAG, "ChangeFileSettings IOException: " + e.getMessage());
                        writeToUiAppend(output, "ChangeFileSettings File 02 Error, Operation aborted");
                        return;
                    }
                    writeToUiAppend(output, "File 02h Change File Settings SUCCESS");

 */

                } catch (IOException e) {
                    Log.e(TAG, "Exception: " + e.getMessage());
                    writeToUiAppend(output, "Exception: " + e.getMessage());
                }
                writeToUiAppend(output, "== FINISHED ==");
                vibrateShort();
            }
        });
        worker.start();
    }

    private byte[] runReadData(int fileNum, int offset, int length) {
        byte[] data = null;
        try {
            data = ReadData.run(dnaC, fileNum, offset, length);
        } catch (IOException e) {
            Log.e(TAG, "readData IOException: " + e.getMessage());
            writeToUiAppend(output, "readData IOException: " + e.getMessage());
        }
        return data;
    }

    /**
     * section for options menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_return_home, menu);

        MenuItem mReturnHome = menu.findItem(R.id.action_return_home);
        mReturnHome.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(TagOverviewActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}