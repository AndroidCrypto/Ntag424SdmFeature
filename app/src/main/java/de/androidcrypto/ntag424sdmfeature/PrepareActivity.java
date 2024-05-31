package de.androidcrypto.ntag424sdmfeature;

import static net.bplearning.ntag424.CommandResult.PERMISSION_DENIED;
import static net.bplearning.ntag424.constants.Ntag424.CC_FILE_NUMBER;
import static net.bplearning.ntag424.constants.Ntag424.NDEF_FILE_NUMBER;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_EVERYONE;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY0;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY2;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY3;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY4;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_NONE;

import static de.androidcrypto.ntag424sdmfeature.Constants.APPLICATION_KEY_3;
import static de.androidcrypto.ntag424sdmfeature.Constants.APPLICATION_KEY_4;
import static de.androidcrypto.ntag424sdmfeature.Constants.APPLICATION_KEY_DEFAULT;
import static de.androidcrypto.ntag424sdmfeature.Constants.APPLICATION_KEY_VERSION_NEW;
import static de.androidcrypto.ntag424sdmfeature.Constants.MASTER_APPLICATION_KEY_FOR_DIVERSIFYING;
import static de.androidcrypto.ntag424sdmfeature.Constants.NDEF_FILE_01_CAPABILITY_CONTAINER_R;
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
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.bplearning.ntag424.DnaCommunicator;
import net.bplearning.ntag424.card.KeyInfo;
import net.bplearning.ntag424.command.ChangeKey;
import net.bplearning.ntag424.command.GetCardUid;
import net.bplearning.ntag424.command.WriteData;
import net.bplearning.ntag424.constants.Ntag424;
import net.bplearning.ntag424.encryptionmode.AESEncryptionMode;
import net.bplearning.ntag424.encryptionmode.LRPEncryptionMode;
import net.bplearning.ntag424.exception.ProtocolException;
import net.bplearning.ntag424.sdm.NdefTemplateMaster;
import net.bplearning.ntag424.sdm.SDMSettings;

import java.io.IOException;
import java.util.Arrays;

public class PrepareActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = PrepareActivity.class.getSimpleName();
    private RadioButton rbKey4Static, rbKey4Derived;
    private com.google.android.material.textfield.TextInputEditText output;

    private DnaCommunicator dnaC = new DnaCommunicator();
    private NfcAdapter mNfcAdapter;
    private IsoDep isoDep;
    private byte[] tagIdByte;

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_prepare);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        rbKey4Static = findViewById(R.id.rbKey4Static);
        rbKey4Derived = findViewById(R.id.rbKey4Derived);
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
        Log.d(TAG, "Prepare Activity Worker");
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
                     * 3) Write the modified Capability Container content to file 01 (Read Only Access to file 02 = NDEF file)
                     * 4) Write an URL template to file 02
                     * 5) Change the application keys 3 and 4
                     * 6) If rbKey4Derived.isChecked diversify key 4 depending on tag UID instead of a static key
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
                        WriteData.run(dnaC, CC_FILE_NUMBER, NDEF_FILE_01_CAPABILITY_CONTAINER_R, 0);
                    } catch (IOException e) {
                        Log.e(TAG, "writeData IOException: " + e.getMessage());
                        writeToUiAppend(output, "File 01h writeDataIOException: " + e.getMessage());
                        writeToUiAppend(output, "Writing the Capability Container FAILURE, Operation aborted");
                        return;
                    }
                    writeToUiAppend(output, "File 01h Writing the Capability Container SUCCESS");

                    // write URL template to file 02
                    SDMSettings sdmSettings = new SDMSettings();
                    sdmSettings.sdmEnabled = false; // at this point we are just preparing the templated but do not enable the SUN/SDM feature
                    sdmSettings.sdmMetaReadPerm = ACCESS_EVERYONE; // Set to a key to get encrypted PICC data
                    sdmSettings.sdmFileReadPerm = ACCESS_KEY2;     // Used to create the MAC and Encrypt FileData
                    sdmSettings.sdmReadCounterRetrievalPerm = ACCESS_NONE; // Not sure what this is for
                    sdmSettings.sdmOptionEncryptFileData = false;
                    byte[] ndefRecord;
                    NdefTemplateMaster master = new NdefTemplateMaster();
                    master.usesLRP = isLrpAuthenticationMode;
                    master.fileDataLength = 0; // no (encrypted) file data
                    ndefRecord = master.generateNdefTemplateFromUrlString("https://sdm.nfcdeveloper.com/tagpt?uid={UID}&ctr={COUNTER}&cmac={MAC}", sdmSettings);
                    try {
                        WriteData.run(dnaC, NDEF_FILE_NUMBER, ndefRecord, 0);
                    } catch (IOException e) {
                        Log.e(TAG, "writeData IOException: " + e.getMessage());
                        writeToUiAppend(output, "File 02h writeDataIOException: " + e.getMessage());
                        writeToUiAppend(output, "Writing the NDEF URL Template FAILURE, Operation aborted");
                        return;
                    }
                    writeToUiAppend(output, "File 02h Writing the NDEF URL Template SUCCESS");

                    // we are changing the application keys 3 and 4 to work with custom keys
                    // to change the keys we need an authentication with application key 0 = master application key
                    // silent authentication
                    if (!isLrpAuthenticationMode) {
                        success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                    } else {
                        success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                    }
                    if (!success) {
                        writeToUiAppend(output, "Error on Authentication with ACCESS KEY 0, aborted");
                        return;
                    }

                    // change application key 3
                    success = false;
                    try {
                        ChangeKey.run(dnaC, ACCESS_KEY3, APPLICATION_KEY_DEFAULT , APPLICATION_KEY_3, APPLICATION_KEY_VERSION_NEW);
                        success = true;
                    } catch (IOException e) {
                        Log.e(TAG, "ChangeKey 3 IOException: " + e.getMessage());
                    }
                    if (success) {
                        writeToUiAppend(output, "Change Application Key 3 SUCCESS");
                    } else {
                        writeToUiAppend(output, "Change Application Key 3 FAILURE, Operation aborted");
                        return;
                    }

                    // change application key 4
                    // the key source depends on the radio button, either use a static Key 4 or a derived Key 4 (tag UID)

                    byte[] newKey4 = null;
                    if (rbKey4Derived.isChecked()) {
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
                        newKey4  = keyInfo.generateKeyForCardUid(realTagUid);
                        Log.d(TAG, Utils.printData("Using a DIVERSIFED Key 4", newKey4));
                    } else {
                        newKey4 = APPLICATION_KEY_4.clone();
                        Log.d(TAG, Utils.printData("Using a STATIC Key 4", newKey4));
                    }
                    success = false;
                    try {
                        ChangeKey.run(dnaC, ACCESS_KEY4, APPLICATION_KEY_DEFAULT , newKey4, APPLICATION_KEY_VERSION_NEW);
                        success = true;
                    } catch (IOException e) {
                        Log.e(TAG, "ChangeKey 4 IOException: " + e.getMessage());
                    }
                    if (success) {
                        if (rbKey4Derived.isChecked()) {
                            writeToUiAppend(output, "Change Application Key 4 to DIVERSIFIED key SUCCESS");
                        } else {
                            writeToUiAppend(output, "Change Application Key 4 to CUSTOM key SUCCESS");
                        }
                    } else {
                        writeToUiAppend(output, "Change Application Key 4 FAILURE, Operation aborted");
                        return;
                    }
                    // todo: change file settings for files 01 and 02 for Read Access from "free access" to key 0
                    // todo: undo the change in "Unset.."
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
                Intent intent = new Intent(PrepareActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}