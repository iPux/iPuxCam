package org.camera.viewer.android.phoebemicro;

import java.io.UnsupportedEncodingException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

public class SecureUtility {
    private static final char[] DIGITS_UPPER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F'};

    private static final String KEY_ALGORITHM = "DESede";
    private static final String DEFAULT_CIPHER_ALGORITHM = "DESede/ECB/ISO10126Padding";

    private static Key toKey(byte[] key) throws Exception {
        DESedeKeySpec dks = new DESedeKeySpec(key);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        SecretKey secretKey = skf.generateSecret(dks);
        return secretKey;
    }

    private static byte[] encrypt(byte[] data, Key key) throws Exception {
        return encrypt(data, key, DEFAULT_CIPHER_ALGORITHM);
    }

    private static byte[] encrypt(byte[] data, Key key, String cipherAlgorithm) throws Exception {
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    private static byte[] decrypt(byte[] data, byte[] key) throws Exception {
        return decrypt(data, key, DEFAULT_CIPHER_ALGORITHM);
    }

    private static byte[] decrypt(byte[] data, byte[] key, String cipherAlgorithm) throws Exception {
        Key k = toKey(key);
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, k);
        return cipher.doFinal(data);

    }

    public static String encode(String data, String str) {
        String encodeString = null;

        try {
            byte[] key = str.getBytes("UTF8");

            java.security.Key secretKey = null;
            byte[] encryptData = null;

            try {
                secretKey = toKey(key);
                encryptData = encrypt(data.getBytes("UTF8"), secretKey);
            } catch (Exception e) {
                e.printStackTrace();
            }

            encodeString = encodeHex(encryptData);
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        return encodeString;
    }

    public static String decode(String data, String str) {
        byte[] decodeBytes = decodeHex(data.toCharArray());
        byte[] decryptData = null;
        String decodeString = null;

        try {
            decryptData = decrypt(decodeBytes, str.getBytes("UTF8"));
            decodeString = new String(decryptData);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return decodeString;
    }

    private static byte[] decodeHex(char[] data) {

        int len = data.length;

        if ((len & 0x01) != 0) {
            throw new RuntimeException("Odd number of characters.");
        }

        byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f = f | toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    private static int toDigit(char ch, int index) {
        int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new RuntimeException("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }

    private static String encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS_UPPER[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS_UPPER[0x0F & data[i]];
        }

        return new String(out);
    }

    public static void main(String[] args) {
        String encodeString = encode("admin", "900150983CD24FB0D6963F7D28E17F72");

        System.out.println(encodeString);

        String decodeString = decode(encodeString, "900150983CD24FB0D6963F7D28E17F72");

        System.out.println(decodeString);
    }
}
