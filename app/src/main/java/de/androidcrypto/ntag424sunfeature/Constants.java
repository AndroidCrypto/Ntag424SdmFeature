package de.androidcrypto.ntag424sunfeature;

public class Constants {
    public static final String SINGLE_DIVIDER = "----------------------------";
    public static final String DOUBLE_DIVIDER = "============================";

    // application keys are AES-128 = 16 bytes long values
    public static final byte[] APPLICATION_KEY_DEFAULT = Utils.hexStringToByteArray("00000000000000000000000000000000");
    public static final byte[] APPLICATION_KEY_1 = Utils.hexStringToByteArray("A1000000000000000000000000000000");
    public static final byte[] APPLICATION_KEY_2 = Utils.hexStringToByteArray("A2000000000000000000000000000000");
    public static final byte[] APPLICATION_KEY_3 = Utils.hexStringToByteArray("A3000000000000000000000000000000");
    public static final byte[] APPLICATION_KEY_4 = Utils.hexStringToByteArray("A4000000000000000000000000000000");
    public static final int APPLICATION_KEY_VERSION_DEFAULT = 0;
    public static final int APPLICATION_KEY_VERSION_NEW = 1;

    // capability container in file 01
    public static final byte[] NDEF_FILE_01_CAPABILITY_CONTAINER_DEFAULT = Utils.hexStringToByteArray("001720010000FF0406E104010000000506E10500808283000000000000000000"); // Free Read & Write Access
    public static final byte[] NDEF_FILE_01_CAPABILITY_CONTAINER_R = Utils.hexStringToByteArray("000F20003A00340406E104010000FF"); // Free Read Access only, no Write Access

    // returnCode from DnaCommunicator
    // byte[] retCode = dnaC.returnCode.clone();
    public static final byte[] PERMISSION_DENIED_ERROR = Utils.hexStringToByteArray("919d");

}
