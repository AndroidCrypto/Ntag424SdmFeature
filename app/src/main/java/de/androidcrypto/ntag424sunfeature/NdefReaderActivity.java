package de.androidcrypto.ntag424sunfeature;

import static net.bplearning.ntag424.constants.Ntag424.NDEF_FILE_NUMBER;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_EVERYONE;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY0;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_KEY2;
import static net.bplearning.ntag424.constants.Permissions.ACCESS_NONE;

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
import android.nfc.tech.IsoDep;
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

import net.bplearning.ntag424.DnaCommunicator;
import net.bplearning.ntag424.command.ChangeFileSettings;
import net.bplearning.ntag424.command.FileSettings;
import net.bplearning.ntag424.command.GetFileSettings;
import net.bplearning.ntag424.command.WriteData;
import net.bplearning.ntag424.constants.Ntag424;
import net.bplearning.ntag424.encryptionmode.AESEncryptionMode;
import net.bplearning.ntag424.encryptionmode.LRPEncryptionMode;
import net.bplearning.ntag424.sdm.NdefTemplateMaster;
import net.bplearning.ntag424.sdm.PiccData;
import net.bplearning.ntag424.sdm.SDMSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;

public class NdefReaderActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = NdefReaderActivity.class.getSimpleName();
    private com.google.android.material.textfield.TextInputEditText output;
    private RadioButton rbUseDefaultKeys, rbUseCustomKeys;
    private DnaCommunicator dnaC = new DnaCommunicator();
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

/*
                        // for this we are rebuilding the PICC data
                        piccData = new PiccData(uidDecrypted, readCounterDecrypted, isLrpAuthentication);
                        piccData.setMacFileKey(cmacKey);

                        writeToUiAppend(output, printData("decryptedFileData", decryptedFileData));
                        byte[] decryptedFileDataReal = Arrays.copyOf(decryptedFileData, 24);

                        cmacCalc = piccData.performShortCMAC(decryptedFileDataReal); // null if MAC on PICC-only data
                        writeToUiAppend(output, printData("cmacCalc", cmacCalc));
                        isCmacValidated = Arrays.equals(cmacCalc, cmacBytes);
                        if (isCmacValidated) {
                            writeToUiAppend(output, "The CMAC is VALIDATED");
                        } else {
                            writeToUiAppend(output, "The CMAC is VOID");
                        }

                        // test with static data that are working on https://sdm.nfcdeveloper.com
                        byte[] encryptedPiccDataTest = Utils.hexStringToByteArray("4E8D0223F8C17CDCCE5BC24076CFAA0D");
                        String encryptedFileDataStringTest = "B56FED7FF7B23791C0684F17E117C97450723BB5C104E809C8929F0264CB99F9969D07FC32BB2D11995AEF826E355097";
                        byte[] encryptedFileDataTest = Utils.hexStringToByteArray(encryptedFileDataStringTest);
                        byte[] cmacTest = Utils.hexStringToByteArray("5FD76DE4BD942DFC");

                        // step 1: PICC data decryption
                        PiccData decryptedPiccDataTest = PiccData.decodeFromEncryptedBytes(encryptedPiccDataTest, new byte[16], false);
                        byte[] uidDecryptedTest = decryptedPiccDataTest.getUid();
                        int readCounterDecryptedTest = decryptedPiccDataTest.getReadCounter();
                        System.out.println("UID: " + Utils.bytesToHex(uidDecryptedTest));
                        System.out.println("ReadCounter: " + readCounterDecryptedTest);

                        // step 2: decrypt Encrypted File data
                        decryptedPiccDataTest.setMacFileKey(new byte[16]);
                        byte[] decryptedFileDataTest = decryptedPiccDataTest.decryptFileData(encryptedFileDataTest);
                        System.out.println("decryptedFileData: " + Utils.bytesToHex(decryptedFileDataTest));
                        System.out.println("decryptedFileData: " + new String(decryptedFileDataTest, StandardCharsets.UTF_8));
                        */

                        /*
                            UID: 049f50824f1390
                            ReadCounter: 16
                            decryptedFileData: 31392e30352e323032342031323a32323a333323313233342a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a
                            decryptedFileData: 19.05.2024 12:22:33#1234************************
                         */
/*
                        // step 3: validate the CMAC
                        // We need to use the 'encrypted file data' including the following '&cmac=' as input
                        // for the CMAC calculation. Then we have to convert this string into byte[] representation
                        byte[] cmacDataTest = (encryptedFileDataStringTest + "&cmac=").getBytes(StandardCharsets.UTF_8);
                        byte[] cmacCalcTest = decryptedPiccDataTest.performShortCMAC(cmacDataTest);
                        System.out.println("CMAC expected  : " + Utils.bytesToHex(cmacTest));
                        System.out.println("CMAC calculated: " + Utils.bytesToHex(cmacCalcTest));
                        System.out.println("The CMAC is validated: " + Arrays.equals(cmacCalcTest, cmacTest));

 */
                        /*
                            CMAC expected  : 5fd76de4bd942dfc
                            CMAC calculated: 5fd76de4bd942dfc
                            The CMAC is validated: true
                         */
/*
                        byte[] uidTest = Utils.hexStringToByteArray("049f50824f1390");
                        int readCounterTest = 16;
                        byte[] fileDataTest = Utils.hexStringToByteArray("31392e30352e323032342031323a32323a333323313233342a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a");

                        PiccData piccTest = new PiccData(uidTest, readCounterTest, false);
                        piccTest.setMacFileKey(new byte[16]);

                        writeToUiAppend(output, printData("CMAC Test", cmacTest));
                        writeToUiAppend(output, printData("CMAC Calc", cmacCalcTest));
*/
/*
https://sdm.nfcdeveloper.com/tag?picc_data=4E8D0223F8C17CDCCE5BC24076CFAA0D&enc=B56FED7FF7B23791C0684F17E117C97450723BB5C104E809C8929F0264CB99F9969D07FC32BB2D11995AEF826E355097&cmac=5FD76DE4BD942DFC

Secure Dynamic Messaging Backend Server Demo
Cryptographic signature validated.

Encryption mode: AES
PICC Data Tag: c7
NFC TAG UID: 049f50824f1390
Read counter: 16
File data (hex): 31392e30352e323032342031323a32323a333323313233342a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a
File data (UTF-8): 19.05.2024 12:22:33#1234************************
Back to the main page
 */






                        /*
                        piccData = PiccData.decodeFromEncryptedBytes(encryptedFileDataBytes, cmacKey, isLrpAuthentication);
                        piccData.setMacFileKey(cmacKey);
                        byte[] finalCmac = piccData.performShortCMAC(decryptedFileData);
                        isCmacValidated = Arrays.equals(finalCmac, cmacBytes);
                        if (isCmacValidated) {
                            writeToUiAppend(output, "The CMAC is VALIDATED");
                        } else {
                            writeToUiAppend(output, "The CMAC is VOID");
                        }

                         */
                    }
                }

                Utils.vibrateShort(getApplicationContext());
/*
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



                    // authentication
                    boolean isLrpAuthenticationMode = false;
                    success = AESEncryptionMode.authenticateEV2(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                    if (success) {
                        writeToUiAppend(output, "AES Authentication SUCCESS");
                    } else {
                        writeToUiAppend(output, "AES Authentication FAILURE");
                        writeToUiAppend(output, "Trying to authenticate in LRP mode");
                        success = LRPEncryptionMode.authenticateLRP(dnaC, ACCESS_KEY0, Ntag424.FACTORY_KEY);
                        if (success) {
                            writeToUiAppend(output, "LRP Authentication SUCCESS");
                            isLrpAuthenticationMode = true;
                        } else {
                            writeToUiAppend(output, "LRP Authentication FAILURE");
                            writeToUiAppend(output, "Authentication not possible, Operation aborted");
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

                    // write URL template to file 02 depending on radio button
                    SDMSettings sdmSettings = new SDMSettings();
                    sdmSettings.sdmEnabled = true; // at this point we are just preparing the templated but do not enable the SUN/SDM feature
                    sdmSettings.sdmMetaReadPerm = ACCESS_KEY2; // Set to a key to get encrypted PICC data
                    sdmSettings.sdmFileReadPerm = ACCESS_KEY2;     // Used to create the MAC and Encrypt FileData
                    sdmSettings.sdmReadCounterRetrievalPerm = ACCESS_NONE; // Not sure what this is for
                    sdmSettings.sdmOptionEncryptFileData = true;
                    byte[] ndefRecord = null;
                    NdefTemplateMaster master = new NdefTemplateMaster();
                    master.usesLRP = isLrpAuthenticationMode;
                    master.fileDataLength = 48; // encrypted file data available. The timestamp is 19 bytes long, but we need multiples of 16 for this feature
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

                    // write the timestamp data (19 characters long + 5 characters '#1234'
                    byte[] fileData = (getTimestampLog() + "#1234").getBytes(StandardCharsets.UTF_8);
                    try {
                        WriteData.run(dnaC, NDEF_FILE_NUMBER, fileData, 87);
                    } catch (IOException e) {
                        Log.e(TAG, "writeFileData IOException: " + e.getMessage());
                        writeToUiAppend(output, "File 02h writeFileDataIOException: " + e.getMessage());
                        writeToUiAppend(output, "Writing the File Data FAILURE, Operation aborted");
                        return;
                    }
                    writeToUiAppend(output, "File 02h Writing the File Data SUCCESS");

                    // check if we authenticated with the right key - to change the key settings we need the CAR key
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

                } catch (IOException e) {
                    Log.e(TAG, "Exception: " + e.getMessage());
                    writeToUiAppend(output, "Exception: " + e.getMessage());
                }
                */
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