package de.androidcrypto.ntag424sdmfeature;

import static de.androidcrypto.ntag424sdmfeature.Constants.APPLICATION_KEY_3;
import static de.androidcrypto.ntag424sdmfeature.Constants.APPLICATION_KEY_4;
import static de.androidcrypto.ntag424sdmfeature.Constants.DOUBLE_DIVIDER;
import static de.androidcrypto.ntag424sdmfeature.Constants.MASTER_APPLICATION_KEY_FOR_DIVERSIFYING;
import static de.androidcrypto.ntag424sdmfeature.Constants.SINGLE_DIVIDER;
import static de.androidcrypto.ntag424sdmfeature.Constants.SYSTEM_IDENTIFIER_FOR_DIVERSIFYING;
import static de.androidcrypto.ntag424sdmfeature.Utils.printData;

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

import net.bplearning.ntag424.card.KeyInfo;
import net.bplearning.ntag424.sdm.PiccData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NdefReaderActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = NdefReaderActivity.class.getSimpleName();
    private com.google.android.material.textfield.TextInputEditText output;
    private RadioButton rbUseDefaultKeys, rbUseCustomKeys, rbUseDiversifiedKeys;
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
        rbUseDiversifiedKeys = findViewById(R.id.rbUseDiversifiedKeys);

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

        runOnUiThread(() -> {
            output.setText("");
        });

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
            } else {
                writeToUiAppend(output, "The tag does not have a NDEF message");
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
                 * These steps are running
                 * 1) Wait that a tag is tapped to the reader and then reading the NDEF message
                 * 2) Parse the payload fortagpt = Plaintext or tag = Encrypted PICC data
                 * 3) if Plaintext data: validate the CMAC using keys depending on radio button default/custom/diversified
                 * 4) if Encrypted PICC data: decrypt the PICC data and validate the CMAC depending on radio button (see 3)
                 * 5)                         check for Encrypted File data and decrypt the data (see 3 for radio button)
                 * The activity is using AES authentication first, if this fails the LRP scheme is used
                 * If the authentication in AES mode fails try to authenticate in LRP mode
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
                boolean isLrpAuthentication =false;

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
                } else if (rbUseCustomKeys.isChecked()) {
                    cmacKey = APPLICATION_KEY_4.clone();
                    encryptedFileDataKey = APPLICATION_KEY_3.clone();
                } else {
                    // using a diversified key
                    // for this we need to decrypt the (encrypted) PICC data first, retrieve
                    // the real Tag UID, diversify the key number 4 from the Master Application Key
                    cmacKey = null;
                    encryptedFileDataKey = APPLICATION_KEY_3.clone(); // this key is NOT diversified, just the CUSTOM key
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
                String uid = "";
                String counter = "";
                String cmac = "";
                String encryptedPiccData = "";
                String encryptedFileData = "";
                // PiccData length is 32 when AES and 48 when LRP encryption
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
                    // the picc data is 32 characters hex data (AES) or 48 characters (LRP)
                    startIndex = fullPayload.indexOf("picc_data=") + 10;
                    // in the first step I'm trying to read 48 characters and convert them to a bytes array. If there are field dividers included in the string like
                    // '&cmac' the conversion will fail, then I'm trying to use 32 characters instead
                    encryptedPiccData = fullPayload.substring(startIndex, startIndex + 48); // LRP
                    boolean isHexNumeric = Utils.isHexNumeric(encryptedPiccData);
                    if (isHexNumeric == false) {
                        // try with 32 characters for AES
                        encryptedPiccData = fullPayload.substring(startIndex, startIndex + 32); // AES
                        isHexNumeric = Utils.isHexNumeric(encryptedPiccData);
                        if (isHexNumeric == false) {
                            Log.e(TAG, "Tried to find encrypted PICC data but none is matching with LRP (48 characters) or AES (32 characters), operation aborted");
                            writeToUiAppend(output, "Tried to find encrypted PICC data but none is matching with LRP (48 characters) or AES (32 characters), operation aborted");
                            return;
                        } else {
                            Log.d(TAG, "Encrypted PICC Data found for AES");
                            isLrpAuthentication = false;
                        }
                    } else {
                        Log.d(TAG, "Encrypted PICC Data found for LRP");
                        isLrpAuthentication = true;
                    }
                }
                if (fullPayload.contains("enc=")) {
                    isFileData = true;
                    // the encrypted file data is 64 characters hex data in our example
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
                        writeToUiAppend(output, "The CMAC is VALIDATED (AES)");
                    } else {
                        //writeToUiAppend(output, "The CMAC is VOID (AES)");
                        // try to run LRP mode
                        piccData = new PiccData(uidBytes, readCounterInt, true);
                        piccData.setMacFileKey(cmacKey);
                        cmacCalc = piccData.performShortCMAC(new byte[0]);// MAC on PICC-only data
                        isCmacValidated = Arrays.equals(cmacCalc, cmacBytes);
                        if (isCmacValidated) {
                            writeToUiAppend(output, "The CMAC is VALIDATED (LRP)");
                        } else {
                            writeToUiAppend(output, "The CMAC is VOID (LRP)");
                        }
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
                    //System.out.println(Utils.printData("encryptedPiccDataBytes", encryptedPiccDataBytes) + " isLrp: " + isLrpAuthentication);
                    piccData = PiccData.decodeFromEncryptedBytes(encryptedPiccDataBytes, encryptedFileDataKey, isLrpAuthentication);
                    byte[] uidDecrypted = piccData.getUid();
                    int readCounterDecrypted = piccData.getReadCounter();

                    // if the Application Key 4 was diversified we need to run the diversification again with the decrypted tag UID
                    if (rbUseDiversifiedKeys.isChecked()) {
                        if ((uidDecrypted == null) || (uidDecrypted.length != 7)) {
                            writeToUiAppend(output, "Working with DIVERSIFIED keys");
                            writeToUiAppend(output, "Could not decrypt the Tag UID, so I cannot diversify the key for validating CMAC data.");
                            return;
                        }
                        // diversify the Master Application key with real Tag UID
                        KeyInfo keyInfo = new KeyInfo();
                        keyInfo.diversifyKeys = true;
                        keyInfo.key = MASTER_APPLICATION_KEY_FOR_DIVERSIFYING.clone();
                        keyInfo.systemIdentifier = SYSTEM_IDENTIFIER_FOR_DIVERSIFYING; // static value for this application
                        cmacKey = keyInfo.generateKeyForCardUid(uidDecrypted);
                        Log.d(TAG, Utils.printData("diversifiedKey", cmacKey));
                    }

                    piccData.setMacFileKey(cmacKey);
                    byte[] cmacCalc = piccData.performShortCMAC(null); // null if MAC on PICC-only data
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
                            // try to run LRP mode
                            piccData = PiccData.decodeFromEncryptedBytes(encryptedPiccDataBytes, encryptedFileDataKey, true);
                            piccData.setMacFileKey(cmacKey);
                            cmacCalc = piccData.performShortCMAC(new byte[0]);// MAC on PICC-only data
                            isCmacValidated = Arrays.equals(cmacCalc, cmacBytes);
                            if (isCmacValidated) {
                                writeToUiAppend(output, "The CMAC is VALIDATED (LRP)");
                            } else {
                                writeToUiAppend(output, "The CMAC is VOID (LRP)");
                            }
                        }
                    } else {
                        // we do have encrypted file data as well
                        writeToUiAppend(output, "Encrypted File Data:\n" + encryptedFileData);
                        // at this point we know the UID and ReadCounter data from decrypted PICC data. Now we can decrypt the encrypted file data using the (personalized ?) fileKey
                        byte[] encryptedFileDataBytes = Utils.hexStringToByteArray(encryptedFileData);
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