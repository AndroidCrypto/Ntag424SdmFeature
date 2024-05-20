package de.androidcrypto.ntag424sunfeature;

public class Constants {

    // application keys are AES-128 = 16 bytes long values
    public static final byte[] APPLICATION_KEY_DEFAULT = Utils.hexStringToByteArray("00000000000000000000000000000000");
    public static final byte[] APPLICATION_KEY_3 = Utils.hexStringToByteArray("A3000000000000000000000000000000");
    public static final byte[] APPLICATION_KEY_4 = Utils.hexStringToByteArray("A4000000000000000000000000000000");
    public static final int APPLICATION_KEY_VERSION_DEFAULT = 0;
    public static final int APPLICATION_KEY_VERSION_NEW = 1;


}
