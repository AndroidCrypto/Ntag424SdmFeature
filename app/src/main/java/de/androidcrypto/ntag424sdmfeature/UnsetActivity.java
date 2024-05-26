package de.androidcrypto.ntag424sdmfeature;

import static net.bplearning.ntag424.CommandResult.PERMISSION_DENIED;
import static net.bplearning.ntag424.constants.Ntag424.CC_FILE_NUMBER;
import static net.bplearning.ntag424.constants.Ntag424.NDEF_FILE_NUMBER;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_EVERYONE;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY0;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY3;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY4;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_NONE;

import static de.androidcrypto.ntag424sdmfeature.Constants.APPLICATION_KEY_3;
import static de.androidcrypto.ntag424sdmfeature.Constants.APPLICATION_KEY_4;
import static de.androidcrypto.ntag424sdmfeature.Constants.APPLICATION_KEY_DEFAULT;
import static de.androidcrypto.ntag424sdmfeature.Constants.APPLICATION_KEY_VERSION_DEFAULT;
import static de.androidcrypto.ntag424sdmfeature.Constants.MASTER_APPLICATION_KEY_FOR_DIVERSIFYING;
import static de.androidcrypto.ntag424sdmfeature.Constants.NDEF_FILE_01_CAPABILITY_CONTAINER_DEFAULT;
import static de.androidcrypto.ntag424sdmfeature.Constants.PERMISSION_DENIED_ERROR;
import static de.androidcrypto.ntag424sdmfeature.Constants.SYSTEM_IDENTIFIER_FOR_DIVERSIFYING;

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
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.bplearning.ntag424.DnaCommunicator;
import net.bplearning.ntag424.card.KeyInfo;
import net.bplearning.ntag424.command.ChangeFileSettings;
import net.bplearning.ntag424.command.ChangeKey;
import net.bplearning.ntag424.command.FileSettings;
import net.bplearning.ntag424.command.GetCardUid;
import net.bplearning.ntag424.command.GetFileSettings;
import net.bplearning.ntag424.command.WriteData;
import net.bplearning.ntag424.constants.Ntag424;
import net.bplearning.ntag424.encryptionmode.AESEncryptionMode;
import net.bplearning.ntag424.encryptionmode.LRPEncryptionMode;
import net.bplearning.ntag424.exception.ProtocolException;
import net.bplearning.ntag424.sdm.SDMSettings;

import java.io.IOException;
import java.util.Arrays;

public class UnsetActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = UnsetActivity.class.getSimpleName();
    private com.google.android.material.textfield.TextInputEditText output;

    private DnaCommunicator dnaC = new DnaCommunicator();
    private NfcAdapter mNfcAdapter;
    private IsoDep isoDep;
    private byte[] tagIdByte;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_unset);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        output = findViewById(R.id.etOutput);

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

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
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
        Log.d(TAG, "UnsetActivity Worker");
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
                     * 3) Write the default Capability Container content to file 01
                     * 4) Clear the file 02 (fill the 256 bytes with 00h)
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

                    // write CC to file 01
                    try {
                        WriteData.run(dnaC, CC_FILE_NUMBER, NDEF_FILE_01_CAPABILITY_CONTAINER_DEFAULT, 0);
                    } catch (IOException e) {
                        Log.e(TAG, "writeData IOException: " + e.getMessage());
                        writeToUiAppend(output, "File 01h writeDataIOException: " + e.getMessage());
                        writeToUiAppend(output, "Writing the Capability Container FAILURE, Operation aborted");
                        return;
                    }
                    writeToUiAppend(output, "File 01h Writing the Capability Container SUCCESS");

                    // Clear the file 02 (fill the 256 bytes with 00h)
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
                    // new settings
                    SDMSettings sdmSettings = new SDMSettings();
                    sdmSettings.sdmEnabled = false; // at this point we are just preparing the templated but do not enable the SUN/SDM feature
                    sdmSettings.sdmMetaReadPerm = ACCESS_NONE; // Set to a key to get encrypted PICC data
                    sdmSettings.sdmFileReadPerm = ACCESS_NONE;  // Used to create the MAC and Encrypt FileData
                    sdmSettings.sdmReadCounterRetrievalPerm = ACCESS_NONE; // Not sure what this is for
                    sdmSettings.sdmOptionEncryptFileData = false;
                    fileSettings02.sdmSettings = sdmSettings;
                    fileSettings02.readWritePerm = ACCESS_EVERYONE;
                    fileSettings02.changePerm = ACCESS_KEY0;
                    fileSettings02.readPerm = ACCESS_EVERYONE;
                    fileSettings02.writePerm = ACCESS_EVERYONE;

                    try {
                    ChangeFileSettings.run(dnaC, NDEF_FILE_NUMBER, fileSettings02);
                    } catch (IOException e) {
                        Log.e(TAG, "ChangeFileSettings IOException: " + e.getMessage());
                        writeToUiAppend(output, "ChangeFileSettings File 02 Error, Operation aborted");
                        return;
                    }

                    // writing blanks to the file to clear, running in 6 writing sequences
                    byte[] bytes51Blank = new byte[51];
                    byte[] bytes01Blank = new byte[1];
                    try {
                        WriteData.run(dnaC, NDEF_FILE_NUMBER, bytes51Blank.clone(), 51 * 0);
                        Log.d(TAG, "Clearing File 02 done part 1");
                        WriteData.run(dnaC, NDEF_FILE_NUMBER, bytes51Blank.clone(), 51 * 1);
                        Log.d(TAG, "Clearing File 02 done part 2");
                        WriteData.run(dnaC, NDEF_FILE_NUMBER, bytes51Blank.clone(), 51 * 2);
                        Log.d(TAG, "Clearing File 02 done part 3");
                        WriteData.run(dnaC, NDEF_FILE_NUMBER, bytes51Blank.clone(), 51 * 3);
                        Log.d(TAG, "Clearing File 02 done part 4");
                        WriteData.run(dnaC, NDEF_FILE_NUMBER, bytes51Blank.clone(), 51 * 4);
                        Log.d(TAG, "Clearing File 02 done part 5");
                        WriteData.run(dnaC, NDEF_FILE_NUMBER, bytes01Blank.clone(), 51 * 5);
                        Log.d(TAG, "Clearing File 02 done part 6");
                    } catch (IOException e) {
                        Log.e(TAG, "writeData IOException: " + e.getMessage());
                        writeToUiAppend(output, "File 02h Clearing writeDataIOException: " + e.getMessage());
                        writeToUiAppend(output, "Clearing the File 02 FAILURE, Operation aborted");
                        return;
                    }
                    writeToUiAppend(output, "File 02h Clearing SUCCESS");

                    // change the application keys 3 + 4 from custom back to default keys
                    // to change the keys we need an authentication with application key 0 = master application key
                    // authentication
                    if (!isLrpAuthenticationMode) {
                        success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                    } else {
                        success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                    }
                    if (success) {
                        writeToUiAppend(output, "Authentication SUCCESS");
                    } else {
                        writeToUiAppend(output, "Authentication FAILURE");
                        writeToUiAppend(output, "Authentication not possible, Operation aborted");
                        return;
                    }

                    // change application key 3
                    success = false;
                    try {
                        ChangeKey.run(dnaC, ACCESS_KEY3, APPLICATION_KEY_3, APPLICATION_KEY_DEFAULT, APPLICATION_KEY_VERSION_DEFAULT);
                        success = true;
                    } catch (IOException e) {
                        Log.e(TAG, "ChangeKey 3 IOException: " + e.getMessage());
                    }
                    if (success) {
                        writeToUiAppend(output, "Change Application Key 3 SUCCESS");
                    } else {
                        writeToUiAppend(output, "Change Application Key 3 FAILURE (maybe the key is already the FACTORY key ?)");
                        // silent authenticate with Access Key 0 as we had a failure
                        if (!isLrpAuthenticationMode) {
                            success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                        } else {
                            success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                        }
                        if (!success) {
                            writeToUiAppend(output, "Error on Authentication with ACCESS KEY 0, aborted");
                            return;
                        }
                    }

                    // change application key 4
                    // this key can be static or diversified
                    success = false;
                    try {
                        ChangeKey.run(dnaC, ACCESS_KEY4, APPLICATION_KEY_4, APPLICATION_KEY_DEFAULT, APPLICATION_KEY_VERSION_DEFAULT);
                        success = true;
                    } catch (IOException e) {
                        Log.e(TAG, "ChangeKey 4 IOException: " + e.getMessage());
                    }
                    if (success) {
                        writeToUiAppend(output, "Change Application Key 4 SUCCESS");
                    } else {
                        writeToUiAppend(output, "Change Application Key 4 FAILURE (maybe the key is already the FACTORY or DIVERSED key ?)");
                    }

                    // if no success try with the diversified key, but first authenticate again
                    if (!success) {
                        // silent authenticate with Access Key 0 as we had a failure
                        if (!isLrpAuthenticationMode) {
                            success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                        } else {
                            success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                        }
                        if (!success) {
                            writeToUiAppend(output, "Error on Authentication with ACCESS KEY 0, aborted");
                            return;
                        }
                        // now get the real tag UID
                        // get the real card UID
                        byte[] realTagUid = null;
                        try {
                            realTagUid = GetCardUid.run(dnaC);
                            Log.d(TAG, Utils.printData("real Tag UID", realTagUid));
                        } catch (ProtocolException e) {
                            writeToUiAppend(output, "Could not read the real Tag UID, aborted");
                            writeToUiAppend(output, "returnCode is " + Utils.byteToHex(dnaC.getLastCommandResult().status2));
                            return;
                        }
                        // derive the Master Application key with real Tag UID
                        KeyInfo keyInfo = new KeyInfo();
                        keyInfo.diversifyKeys = true;
                        keyInfo.key = MASTER_APPLICATION_KEY_FOR_DIVERSIFYING.clone();
                        keyInfo.systemIdentifier = SYSTEM_IDENTIFIER_FOR_DIVERSIFYING; // static value for this application
                        byte[] diversifiedKey = keyInfo.generateKeyForCardUid(realTagUid);
                        Log.d(TAG, Utils.printData("diversifiedKey", diversifiedKey));
                        success = false;
                        try {
                            ChangeKey.run(dnaC, ACCESS_KEY4, diversifiedKey, APPLICATION_KEY_DEFAULT, APPLICATION_KEY_VERSION_DEFAULT);
                            success = true;
                        } catch (IOException e) {
                            Log.e(TAG, "ChangeKey 4 IOException: " + e.getMessage());
                        }
                        if (success) {
                            writeToUiAppend(output, "Change Application Key 4 SUCCESS");
                        } else {
                            writeToUiAppend(output, "Change Application Key 4 FAILURE (UNKNOWN key)");
                        }
                    }
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
                Intent intent = new Intent(UnsetActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}