package de.androidcrypto.ntag424sdmfeature;

import static net.bplearning.ntag424.CommandResult.PERMISSION_DENIED;
import static net.bplearning.ntag424.constants.Ntag424.NDEF_FILE_NUMBER;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_EVERYONE;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY0;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY2;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY3;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY4;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_NONE;

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
import net.bplearning.ntag424.command.WriteData;
import net.bplearning.ntag424.constants.Ntag424;
import net.bplearning.ntag424.encryptionmode.AESEncryptionMode;
import net.bplearning.ntag424.encryptionmode.LRPEncryptionMode;
import net.bplearning.ntag424.sdm.NdefTemplateMaster;
import net.bplearning.ntag424.sdm.SDMSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class EncryptedFileSunCustomKeysActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = EncryptedFileSunCustomKeysActivity.class.getSimpleName();
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
        setContentView(R.layout.activity_encrypted_file_sun_custom_keys);
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
        Log.d(TAG, "Encrypted File SUN Custom Keys Activity Worker");
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
                     * These steps are running - assuming that all keys are 'default' keys filled with 16 00h values
                     * 1) Authenticate with Application Key 00h in AES mode
                     * 2) If the authentication in AES mode fails try to authenticate in LRP mode
                     * 3) Write an URL template to file 02 with PICC (Uid and/or Counter) plus CMAC
                     * 4) Get existing file settings for file 02
                     * 5) Save the modified file settings back to the tag
                     */

                    // authentication
                    boolean isLrpAuthenticationMode = false;

                    success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                    if (success) {
                        writeToUiAppend(output, "AES Authentication SUCCESS");
                    } else {
                        // if the returnCode is '919d' = permission denied the tag is in LRP mode authentication
                        if (dnaC.getLastCommandResult().status2 == PERMISSION_DENIED) {
                            // try to run the LRP authentication
                            success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                            if (success) {
                                writeToUiAppend(output, "LRP Authentication SUCCESS");
                                isLrpAuthenticationMode = true;
                            } else {
                                writeToUiAppend(output, "LRP Authentication FAILURE");
                                writeToUiAppend(output, "returnCode is " + Utils.byteToHex(dnaC.getLastCommandResult().status2));
                                writeToUiAppend(output, "Authentication not possible, Operation aborted");
                                return;
                            }
                        } else {
                            // any other error, print the error code and return
                            writeToUiAppend(output, "AES Authentication FAILURE");
                            writeToUiAppend(output, "returnCode is " + Utils.byteToHex(dnaC.getLastCommandResult().status2));
                            return;
                        }
                    }

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
                            if (!isLrpAuthenticationMode) {
                                success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY_RW, Ntag424.FACTORY_KEY);
                            } else {
                                success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY_RW, Ntag424.FACTORY_KEY);
                            }
                            if (!success) {
                                writeToUiAppend(output, "Error on Authentication with key " + ACCESS_KEY_RW  + ", aborted");
                                return;
                            }
                        }
                    }

                    // write URL template to file 02 depending on radio button
                    SDMSettings sdmSettings = new SDMSettings();
                    sdmSettings.sdmEnabled = true; // at this point we are just preparing the templated but do not enable the SUN/SDM feature
                    sdmSettings.sdmMetaReadPerm = ACCESS_KEY3; // Set to a key to get encrypted PICC data
                    sdmSettings.sdmFileReadPerm = ACCESS_KEY4; // Used to create the MAC and Encrypted File data
                    sdmSettings.sdmReadCounterRetrievalPerm = ACCESS_NONE; // Not sure what this is for
                    sdmSettings.sdmOptionEncryptFileData = true;
                    byte[] ndefRecord = null;
                    NdefTemplateMaster master = new NdefTemplateMaster();
                    master.usesLRP = isLrpAuthenticationMode;
                    master.fileDataLength = 32; // encrypted file data available. The timestamp is 19 bytes long, but we need multiples of 16 for this feature
                    if (rbUid.isChecked()) {
                        sdmSettings.sdmOptionUid = true;
                        sdmSettings.sdmOptionReadCounter = false;
                    } else if (rbCounter.isChecked()) {
                        sdmSettings.sdmOptionUid = false;
                        sdmSettings.sdmOptionReadCounter = true;
                    } else {
                        sdmSettings.sdmOptionUid = true;
                        sdmSettings.sdmOptionReadCounter = true;
                    }
                    ndefRecord = master.generateNdefTemplateFromUrlString("https://sdm.nfcdeveloper.com/tag?picc_data={PICC}&enc={FILE}&cmac={MAC}", sdmSettings);
                    try {
                        WriteData.run(dnaC, NDEF_FILE_NUMBER, ndefRecord, 0);
                    } catch (IOException e) {
                        Log.e(TAG, "writeData IOException: " + e.getMessage());
                        writeToUiAppend(output, "File 02h writeDataIOException: " + e.getMessage());
                        writeToUiAppend(output, "Writing the NDEF URL Template FAILURE, Operation aborted");
                        return;
                    }
                    writeToUiAppend(output, "File 02h Writing the NDEF URL Template SUCCESS");
                    //System.out.println("*** NdefRecord: " + new String(ndefRecord, StandardCharsets.UTF_8));
                    // write the timestamp data (19 characters long + 5 characters '#1234'
                    byte[] fileData = (getTimestampLog() + "#1234").getBytes(StandardCharsets.UTF_8);
                    try {
                        if (isLrpAuthenticationMode) {
                            WriteData.run(dnaC, NDEF_FILE_NUMBER, fileData, (87 + 16)); // LRP Encrypted PICC data is 16 bytes longer
                        } else {
                            WriteData.run(dnaC, NDEF_FILE_NUMBER, fileData, 87);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "writeFileData IOException: " + e.getMessage());
                        writeToUiAppend(output, "File 02h writeFileDataIOException: " + e.getMessage());
                        writeToUiAppend(output, "Writing the File Data FAILURE, Operation aborted");
                        return;
                    }
                    writeToUiAppend(output, "File 02h Writing the File Data SUCCESS");

                    // check if we authenticated with the right key - to change the key settings we need the CAR key
                    if (ACCESS_KEY_CAR != ACCESS_KEY_RW) {
                        if (!isLrpAuthenticationMode) {
                            success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY_CAR, Ntag424.FACTORY_KEY);
                        } else {
                            success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY_CAR, Ntag424.FACTORY_KEY);
                        }
                        if (!success) {
                            writeToUiAppend(output, "Error on Authentication with key " + ACCESS_KEY_CAR  + ", aborted");
                            return;
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
                        e.printStackTrace();
                        Log.e(TAG, "ChangeFileSettings IOException: " + e.getMessage());
                        writeToUiAppend(output, "ChangeFileSettings File 02 Error, Operation aborted");
                        return;
                    }
                    writeToUiAppend(output, "File 02h Change File Settings SUCCESS");

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

    // gives an 19 byte long timestamp dd.MM.yyyy HH:mm:ss
    public static String getTimestampLog() {
        // gives a 19 character long string
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ZonedDateTime
                    .now(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm:ss"));
        } else {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        }
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
                Intent intent = new Intent(EncryptedFileSunCustomKeysActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}