package de.androidcrypto.ntag424sdmfeature;

import static android.content.Context.VIBRATOR_SERVICE;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Utils {

    // this checks if a String sequence is a valid hext string
    public static boolean isHexNumeric(String cadena) {
        if ( cadena.length() == 0 ||
                (cadena.charAt(0) != '-' && Character.digit(cadena.charAt(0), 16) == -1))
            return false;
        if ( cadena.length() == 1 && cadena.charAt(0) == '-' )
            return false;

        for ( int i = 1 ; i < cadena.length() ; i++ )
            if ( Character.digit(cadena.charAt(i), 16) == -1 )
                return false;
        return true;
    }

    public static String removeAllNonAlphaNumeric(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("[^A-Za-z0-9]", "");
    }

    // position is 0 based starting from right to left
    public static byte setBitInByte(byte input, int pos) {
        return (byte) (input | (1 << pos));
    }

    // position is 0 based starting from right to left
    public static byte unsetBitInByte(byte input, int pos) {
        return (byte) (input & ~(1 << pos));
    }

    // https://stackoverflow.com/a/29396837/8166854
    public static boolean testBit(byte b, int n) {
        int mask = 1 << n; // equivalent of 2 to the nth power
        return (b & mask) != 0;
    }

    // https://stackoverflow.com/a/29396837/8166854
    public static boolean testBit(byte[] array, int n) {
        int index = n >>> 3; // divide by 8
        int mask = 1 << (n & 7); // n modulo 8
        return (array[index] & mask) != 0;
    }

    public static String printData(String dataName, byte[] data) {
        int dataLength;
        String dataString = "";
        if (data == null) {
            dataLength = 0;
            dataString = "IS NULL";
        } else {
            dataLength = data.length;
            dataString = bytesToHex(data);
        }
        StringBuilder sb = new StringBuilder();
        sb
                .append(dataName)
                .append(" length: ")
                .append(dataLength)
                .append(" data: ")
                .append(dataString);
        return sb.toString();
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuffer result = new StringBuffer();
        for (byte b : bytes)
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    public static String bytesToHexNpe(byte[] bytes) {
        if (bytes == null) return "";
        StringBuffer result = new StringBuffer();
        for (byte b : bytes)
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    public static String bytesToHexNpeUpperCase(byte[] bytes) {
        if (bytes == null) return "";
        StringBuffer result = new StringBuffer();
        for (byte b : bytes)
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString().toUpperCase();
    }

    public static String bytesToHexNpeUpperCaseBlank(byte[] bytes) {
        if (bytes == null) return "";
        StringBuffer result = new StringBuffer();
        for (byte b : bytes)
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1)).append(" ");
        return result.toString().toUpperCase();
    }

    public static String byteToHex(Byte input) {
        return String.format("%02X", input);
        //return String.format("0x%02X", input);
    }

    public static byte[] hexStringToByteArray(String s) {
        try {
            int len = s.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i + 1), 16));
            }
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * will delete any '-' and " " characters between the hex chars before converting to a byte array
     * @param s
     * @return
     */
    public static byte[] hexStringToByteArrayMinus(String s) {
        String newS = s.replaceAll("-", "").replaceAll(" ", "");
        return hexStringToByteArray(newS);

    }
    public static String getDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result + "";
    }

    public static String printByteBinary(byte bytes){
        byte[] data = new byte[1];
        data[0] = bytes;
        return printByteArrayBinary(data);
    }

    public static String printByteArrayBinary(byte[] bytes){
        String output = "";
        for (byte b1 : bytes){
            String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
            //s1 += " " + Integer.toHexString(b1);
            //s1 += " " + b1;
            output = output + " " + s1;
            //System.out.println(s1);
        }
        return output;
    }

    /**
     * Reverse a byte Array (e.g. Little Endian -> Big Endian).
     * Hmpf! Java has no Array.reverse(). And I don't want to use
     * Commons.Lang (ArrayUtils) from Apache....
     *
     * @param array The array to reverse (in-place).
     */
    public static void reverseByteArrayInPlace(byte[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            byte temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }


    // converts an int to a 3 byte long array
    public static byte[] intTo3ByteArray(int value) {
        return new byte[] {
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)value};
    }

    // converts an int to a 3 byte long array inversed
    public static byte[] intTo3ByteArrayInversed(int value) {
        return new byte[] {
                (byte)value,
                (byte)(value >> 8),
                (byte)(value >> 16)};
    }

    public static int intFrom3ByteArrayInversed(byte[] bytes) {
        return  ((bytes[2] & 0xFF) << 16) |
                ((bytes[1] & 0xFF) << 8 ) |
                ((bytes[0] & 0xFF) << 0 );
    }

    public static int intFrom3ByteArray(byte[] bytes) {
        return  ((bytes[0] & 0xFF) << 16) |
                ((bytes[1] & 0xFF) << 8 ) |
                ((bytes[2] & 0xFF) << 0 );
    }

    private int byteArrayLength3InversedToInt(byte[] data) {
        return (data[2] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[0] & 0xff);
    }

    // converts an int to a 2 byte long array inversed = LSB
    public static byte[] intTo2ByteArrayInversed(int value) {
        return new byte[]{
                (byte) value,
                (byte) (value >> 8)};
    }

    /**
     * Returns a byte array with length = 4
     * @param value
     * @return
     */
    public static byte[] intToByteArray4(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value};
    }

    // Little Endian = LSB order
    public static byte[] intTo4ByteArrayInversed(int myInteger){
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(myInteger).array();
    }

    // packing an array of 4 bytes to an int, big endian, minimal parentheses
    // operator precedence: <<, &, |
    // when operators of equal precedence (here bitwise OR) appear in the same expression, they are evaluated from left to right
    public static int intFromByteArray(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    /// packing an array of 4 bytes to an int, big endian, clean code
    public static int intFromByteArrayV3(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8 ) |
                ((bytes[3] & 0xFF) << 0 );
    }

    public static int byteArrayLength4NonInversedToInt(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    //
    public static int byteArrayLength4InversedToInt(byte[] bytes) {
        return bytes[3] << 24 | (bytes[2] & 0xFF) << 16 | (bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF);
    }

    public static char intToUpperNibble(int input) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        //int v = input & 0xFF; // Cast byte to int, treating as unsigned value
        int v = input;
        return hexArray[v >>> 4]; // Select hex character from upper nibble
    }

    public static char byteToUpperNibble(Byte input) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        int v = input & 0xFF; // Cast byte to int, treating as unsigned value
        return hexArray[v >>> 4]; // Select hex character from upper nibble
    }

    public static char byteToLowerNibble(Byte input) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        int v = input & 0xFF; // Cast byte to int, treating as unsigned value
        return hexArray[v & 0x0F]; // Select hex character from lower nibble
    }

    public static byte nibblesToByte(char upperNibble, char lowerNibble) {
        String data = String.valueOf(upperNibble) + String.valueOf(lowerNibble);
        byte[] byteArray = hexStringToByteArray(data);
        return byteArray[0];
    }

    public static int byteToUpperNibbleInt(Byte input) {
        return (input & 0xF0 ) >> 4;
    }

    public static int byteToLowerNibbleInt(Byte input) {
        return input & 0x0F;
    }

    public static List<Integer> getNibblesFromByteArray(byte[] data) {
        if ((data == null) || (data.length < 1)) {
            return null;
        }
        int length = data.length;
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            byte dataByte = data[i];
            int upperNibbleInt = Utils.byteToUpperNibbleInt(dataByte);
            int loweNibbleInt  = Utils.byteToLowerNibbleInt(dataByte);
            list.add(upperNibbleInt);
            list.add(loweNibbleInt);
        }
        return list;
    }


    /**
     * splits a byte array in chunks
     *
     * @param source
     * @param chunksize
     * @return a List<byte[]> with sets of chunksize
     */
    public static List<byte[]> divideArrayToList(byte[] source, int chunksize) {
        List<byte[]> result = new ArrayList<byte[]>();
        int start = 0;
        while (start < source.length) {
            int end = Math.min(source.length, start + chunksize);
            result.add(Arrays.copyOfRange(source, start, end));
            start += chunksize;
        }
        return result;
    }

    // gives an 19 byte long timestamp yyyy.MM.dd HH:mm:ss
    public static String getTimestamp() {
        // gives a 19 character long string
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ZonedDateTime
                    .now(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("uuuu.MM.dd HH:mm:ss"));
        } else {
            return new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
        }
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

    public static byte[] generateTestData(int length) {
        /**
         * this method will generate a byte array of size 'length' and will hold a byte sequence
         * 00 01 .. FE FF 00 01 ..
         */
        // first generate a basis array
        byte[] basis = new byte[256];
        for (int i = 0; i < 256; i++) {
            basis[i] = (byte) (i & 0xFF);
        }
        // second copying the basis array to the target array
        byte[] target = new byte[length];
        if (length < 256) {
            target = Arrays.copyOfRange(basis, 0, length);
            return target;
        }
        // now length is > 256 so we do need multiple copies
        int numberOfChunks = length / 256;
        int dataLoop = 0;
        for (int i = 0; i < numberOfChunks; i++) {
            System.arraycopy(basis, 0, target, dataLoop, 256);
            dataLoop += 256;
        }
        // if some bytes are missing we are copying now
        if (dataLoop < length) {
            System.arraycopy(basis, 0, target, dataLoop, length - dataLoop);
        }
        return target;
    }

    public static byte[] generateRandomTestData(int length) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] data = new byte[length];
        secureRandom.nextBytes(data);
        return data;
    }

    public static void vibrateShort(Context context) {
        // Make a Sound
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((Vibrator) context.getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(50, 10));
        } else {
            Vibrator v = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            v.vibrate(50);
        }
    }

    /**
     * NFC Forum "URI Record Type Definition"<p>
     * This is a mapping of "URI Identifier Codes" to URI string prefixes,
     * per section 3.2.2 of the NFC Forum URI Record Type Definition document.
     */
    // source: https://github.com/skjolber/ndef-tools-for-android
    private static final String[] URI_PREFIX_MAP = new String[] {
            "", // 0x00
            "http://www.", // 0x01
            "https://www.", // 0x02
            "http://", // 0x03
            "https://", // 0x04
            "tel:", // 0x05
            "mailto:", // 0x06
            "ftp://anonymous:anonymous@", // 0x07
            "ftp://ftp.", // 0x08
            "ftps://", // 0x09
            "sftp://", // 0x0A
            "smb://", // 0x0B
            "nfs://", // 0x0C
            "ftp://", // 0x0D
            "dav://", // 0x0E
            "news:", // 0x0F
            "telnet://", // 0x10
            "imap:", // 0x11
            "rtsp://", // 0x12
            "urn:", // 0x13
            "pop:", // 0x14
            "sip:", // 0x15
            "sips:", // 0x16
            "tftp:", // 0x17
            "btspp://", // 0x18
            "btl2cap://", // 0x19
            "btgoep://", // 0x1A
            "tcpobex://", // 0x1B
            "irdaobex://", // 0x1C
            "file://", // 0x1D
            "urn:epc:id:", // 0x1E
            "urn:epc:tag:", // 0x1F
            "urn:epc:pat:", // 0x20
            "urn:epc:raw:", // 0x21
            "urn:epc:", // 0x22
    };

    public static String parseUriRecordPayload(byte[] ndefPayload) {
        int uriPrefix = Array.getByte(ndefPayload, 0);
        int ndefPayloadLength = ndefPayload.length;
        byte[] message = new byte[ndefPayloadLength - 1];
        System.arraycopy(ndefPayload, 1, message, 0, ndefPayloadLength - 1);
        return URI_PREFIX_MAP[uriPrefix] + new String(message, StandardCharsets.UTF_8);
    }

 }
