package de.androidcrypto.ntag424sunfeature;

import static de.androidcrypto.ntag424sunfeature.Constants.APPLICATION_KEY_3;
import static de.androidcrypto.ntag424sunfeature.Constants.APPLICATION_KEY_4;
import static de.androidcrypto.ntag424sunfeature.Utils.DOUBLE_DIVIDER;
import static de.androidcrypto.ntag424sunfeature.Utils.SINGLE_DIVIDER;
import static de.androidcrypto.ntag424sunfeature.Utils.printData;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
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

import net.bplearning.ntag424.sdm.PiccData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NdefReaderActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = NdefReaderActivity.class.getSimpleName();
    private com.google.android.material.textfield.TextInputEditText output;
    private RadioButton rbUseDefaultKeys, rbUseCustomKeys;
    private NfcAdapter mNfcAdapter;
    private Ndef ndef;
    private NdefMessage ndefMessage;
    private NdefRecord[] ndefRecords;
    private byte[] tagIdByte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ndef_reader);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        output = findViewById(R.id.etOutput);
        rbUseDefaultKeys = findViewById(R.id.rbUseDefaultKeys);
        rbUseCustomKeys = findViewById(R.id.rbUseCustomKeys);

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

        // here we are using the NDEF technology to work with a NDEF message directly
        ndef = null;
        try {
            ndef = Ndef.get(tag);
            if (ndef != null) {
                // Make a Vibration
                vibrateShort();

                runOnUiThread(() -> {
                    output.setText("");
                });

                ndefMessage = ndef.getCachedNdefMessage();
                if (ndefMessage == null) {
                    writeToUiAppend(output, "Could not read the NDEF message, aborted");
                    ndef.close();
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
            // to get the data of the tag after reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            //NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
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
        Log.d(TAG, "NDEF Reader Activity Worker");
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {

                /**
                 * These steps are running - assuming that all keys are 'default' keys filled with 16 00h values
                 * 1) Authenticate with Application Key 00h in AES mode
                 * 2) If the authentication in AES mode fails try to authenticate in LRP mode
                 * 3) Write an URL template to file 02 with PICC (Uid and/or Counter) plus CMAC
                 * 4) Get existing file settings for file 02
                 * 5) Save the modified file settings back to the tag
                 */

                writeToUiAppend(output, DOUBLE_DIVIDER);

                // the NDEF message is in ndefMessage
                ndefRecords = ndefMessage.getRecords();

                if (ndefRecords.length < 1) {
                    writeToUiAppend(output, "Could not find any NDEF record, aborted");
                    return;
                }

                // we expect only 1 record in the NDEF message
                NdefRecord ndefRecord = ndefRecords[0];
                // we expect a NDEF record of 'Well known type URI'
                byte[] ndefType = ndefRecord.getType();
                short ndefTnf = ndefRecord.getTnf();
                byte[] ndefPayload = ndefRecord.getPayload();
                if (ndefTnf == NdefRecord.TNF_WELL_KNOWN &&
                        Arrays.equals(ndefType, NdefRecord.RTD_URI)) {
                    // nothing to do here
                } else {
                    // not of the correct type
                    writeToUiAppend(output, "I did not find a NDEF record of 'Well Know URI Type', aborted");
                    return;
                }

                String ndefText = Utils.parseUriRecordPayload(ndefPayload);
                writeToUiAppend(output, "NDEF message: " + ndefText);
                writeToUiAppend(output, SINGLE_DIVIDER);

                // we need to know if the tag runs with AES or LRP authentication
                // todo use a checkButton
                boolean isLrpAuthentication = false; // todo this is STATIC

                // key usage depends on radio button - use default or custom keys
                byte[] cmacKey = null;
                byte[] encryptedFileDataKey = null;
                if (rbUseDefaultKeys.isChecked()) {
                    // We need a key for CMAC validation - this was defined during SUN setup:
                    // sdmSettings.sdmFileReadPerm = ACCESS_KEY2; // Used to create the MAC and Encrypt FileData
                    // The backend server needs to know this key:
                    cmacKey = new byte[16]; // here we are using the default AES-128 key

                    // We need a key for Encrypted File Data (if used in the NDEF message) - this was used during SUN setup as well:
                     encryptedFileDataKey = new byte[16]; // here we are using the default AES-128 key
                } else {
                    cmacKey = APPLICATION_KEY_4.clone();
                    encryptedFileDataKey = APPLICATION_KEY_3.clone();
                }

                // now we are trying to parse the content of the message
                // we expecting a header 'https://sdm.nfcdeveloper.com/'
                // followed by 'tag?' or 'tagpt?'
                String fullPayload = "";
                if (ndefText.startsWith("https://sdm.nfcdeveloper.com/")) {
                    fullPayload = ndefText.replace("https://sdm.nfcdeveloper.com/", "");
                } else {
                    writeToUiAppend(output, "The Backend Server URL is not matching, aborted");
                    return;
                }

                // are we expecting plaintext or encrypted data ?
                boolean isEncrypted = false;
                if (fullPayload.startsWith("tagpt?")) {
                    // plaintext data
                    isEncrypted = false;
                } else if (fullPayload.startsWith("tag?")) {
                    isEncrypted = true;
                } else {
                    // not a matching api
                    writeToUiAppend(output, "The Backend Server URL is not matching an API, aborted");
                    return;
                }

                // start parsing the payload
                int fullPayloadLength = fullPayload.length();
                String uid = "";
                String counter = "";
                String cmac = "";
                String encryptedPiccData = "";
                String encryptedFileData = "";
                boolean isUid = false;
                boolean isCounter = false;
                boolean isCmac = false;
                boolean isPiccData = false;
                boolean isFileData = false;
                boolean isCmacValidated = false;
                int startIndex = 0;
                // note: this is trying to read the data with the expected length
                // if the data is shorter we will receive errors
                if (fullPayload.contains("uid=")) {
                    isUid = true;
                    // the uid is 14 characters hex data
                    startIndex = fullPayload.indexOf("uid=") + 4;
                    uid = fullPayload.substring(startIndex, startIndex + 14);
                }
                if (fullPayload.contains("ctr=")) {
                    isCounter = true;
                    // the counter is 6 characters hex data
                    startIndex = fullPayload.indexOf("ctr=") + 4;
                    counter = fullPayload.substring(startIndex, startIndex + 6);
                }
                if (fullPayload.contains("cmac=")) {
                    isCmac = true;
                    // the cmac is 16 characters hex data
                    startIndex = fullPayload.indexOf("cmac=") + 5;
                    cmac = fullPayload.substring(startIndex, startIndex + 16);
                }
                if (fullPayload.contains("picc_data=")) {
                    isPiccData = true;
                    // the picc data is 32 characters hex data
                    startIndex = fullPayload.indexOf("picc_data=") + 10;
                    encryptedPiccData = fullPayload.substring(startIndex, startIndex + 32);
                }
                if (fullPayload.contains("enc=")) {
                    isFileData = true;
                    // the encrypted file data is 96 characters hex data in our example
                    // setting length = 48: 6AA3587B6651DB460F2129AEC9E9C558CF540826B87D3008D9507013ABD80B7FFA4B8F18D22917237CD27590F0397FBB
                    // setting length = 32: 3FF6D3C1B1E33F0B4E8AD272957DA63A890C80730EB5F37DD8642511824A720B 20.05.2024 11:31:05#1234********
                    startIndex = fullPayload.indexOf("enc=") + 4;
                    System.out.println("fullPayload:" + fullPayload+ "#");
                    //encryptedFileData = fullPayload.substring(startIndex, startIndex + 96);
                    encryptedFileData = fullPayload.substring(startIndex, startIndex + 64);
                }

                // I do not check that the payload has all necessary fields here
                PiccData piccData;
                byte[] uidBytes = new byte[0];
                int readCounterInt;
                if (!isEncrypted) {
                    // plaintext data
                    if (isUid) {
                        uidBytes = Utils.hexStringToByteArray(uid);
                    }
                    if (isCounter) {
                        readCounterInt = Utils.intFrom3ByteArray(Utils.hexStringToByteArray(counter));
                    } else {
                        readCounterInt = 0;
                    }
                    piccData = new PiccData(uidBytes, readCounterInt, isLrpAuthentication);
                    piccData.setMacFileKey(cmacKey);
                    byte[] cmacCalc = piccData.performShortCMAC(new byte[0]);// MAC on PICC-only data
                    byte[] cmacBytes;
                    if (isCmac) {
                        cmacBytes = Utils.hexStringToByteArray(cmac);
                    } else {
                        writeToUiAppend(output, "Could not find CMAC data, aborted");
                        return;
                    }
                    isCmacValidated = Arrays.equals(cmacCalc, cmacBytes);

                    writeToUiAppend(output, "UID:" + uid);
                    writeToUiAppend(output, "Counter:" + counter);
                    writeToUiAppend(output, "CMAC:" + cmac);
                    if (isCmacValidated) {
                        writeToUiAppend(output, "The CMAC is VALIDATED");
                    } else {
                        writeToUiAppend(output, "The CMAC is VOID");
                    }
                } else {
                    // encrypted PICC data
                    // expecting encryptedPiccData
                    byte[] encryptedPiccDataBytes;
                    if (isPiccData) {
                        encryptedPiccDataBytes = Utils.hexStringToByteArray(encryptedPiccData);
                    } else {
                        writeToUiAppend(output, "Could not find PICC data, aborted");
                        return;
                    }
                    piccData = PiccData.decodeFromEncryptedBytes(encryptedPiccDataBytes, encryptedFileDataKey, isLrpAuthentication);
                    byte[] uidDecrypted = piccData.getUid();
                    int readCounterDecrypted = piccData.getReadCounter();
                    piccData.setMacFileKey(cmacKey);
                    byte[] cmacCalc = piccData.performShortCMAC(null);// null if MAC on PICC-only data
                    byte[] cmacBytes;
                    if (isCmac) {
                        cmacBytes = Utils.hexStringToByteArray(cmac);
                    } else {
                        writeToUiAppend(output, "Could not find CMAC data, aborted");
                        return;
                    }

                    writeToUiAppend(output, "PICC:" + encryptedPiccData);
                    writeToUiAppend(output, "UID:" + Utils.bytesToHex(uidDecrypted));
                    writeToUiAppend(output, "Counter:" + readCounterDecrypted);
                    writeToUiAppend(output, "CMAC:" + cmac);

                    if (!isFileData) {
                        isCmacValidated = Arrays.equals(cmacCalc, cmacBytes);
                        if (isCmacValidated) {
                            writeToUiAppend(output, "The CMAC is VALIDATED");
                        } else {
                            writeToUiAppend(output, "The CMAC is VOID");
                        }
                    } else {
                        // we do have encrypted file data as well
                        writeToUiAppend(output, "Encrypted File Data:\n" + encryptedFileData);
                        // at this point we know the UID and ReadCounter data from decrypted PICC data. Now we can decrypt the encrypted file data using the (personalized ?) fileKey

                        //PiccData piccDataEnc = new PiccData(uidDecrypted, readCounterDecrypted, isLrpAuthentication);
                        byte[] encryptedFileDataBytes = Utils.hexStringToByteArray(encryptedFileData);
                        //piccDataEnc.setMacFileKey(encryptedFileDataKey);
                        //byte[] decryptedFileData = piccDataEnc.decryptFileData(encryptedFileDataBytes);
                        byte[] decryptedFileData = piccData.decryptFileData(encryptedFileDataBytes);
                        writeToUiAppend(output, "Decrypted File data:\n" + new String(decryptedFileData, StandardCharsets.UTF_8));

                        // validate the CMAC over all elements
                        // The CMAC is calculated over the encrypted file data STRING (upper case hex characters) and the
                        // following '&cmac=' string. After concatenating both the string is converted to a byte[]
                        byte[] encryptedFileDataForCmac = (encryptedFileData + "&cmac=").getBytes(StandardCharsets.UTF_8);
                        cmacCalc = piccData.performShortCMAC(encryptedFileDataForCmac); // null if MAC on PICC-only data
                        writeToUiAppend(output, printData("cmacCalc", cmacCalc));
                        isCmacValidated = Arrays.equals(cmacCalc, cmacBytes);
                        if (isCmacValidated) {
                            writeToUiAppend(output, "The CMAC is VALIDATED");
                        } else {
                            writeToUiAppend(output, "The CMAC is VOID");
                        }
                    }
                }
                writeToUiAppend(output, "== FINISHED ==");
                Utils.vibrateShort(getApplicationContext());
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
                Intent intent = new Intent(NdefReaderActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}