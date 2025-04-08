package me.chayapak1.chomens_bot.chomeNSMod;

import me.chayapak1.chomens_bot.util.Ascii85;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

// inspired from smp encryption plugin
public class Encryptor {
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    public static String encrypt (byte[] data, String password) throws Exception {
        final byte[] salt = generateRandomBytes(SALT_LENGTH);
        final SecretKey key = deriveKey(password, salt);

        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        final byte[] iv = generateRandomBytes(IV_LENGTH);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

        final byte[] encrypted = cipher.doFinal(data);

        final byte[] combined = new byte[salt.length + iv.length + encrypted.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);

        return Ascii85.encode(combined);
    }

    public static byte[] decrypt (String ascii85Data, String password) throws Exception {
        final byte[] combined = Ascii85.decode(ascii85Data);

        final byte[] salt = new byte[SALT_LENGTH];
        final byte[] iv = new byte[IV_LENGTH];
        final byte[] encrypted = new byte[combined.length - SALT_LENGTH - IV_LENGTH];

        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
        System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH);
        System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, encrypted, 0, encrypted.length);

        final SecretKey key = deriveKey(password, salt);
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        return cipher.doFinal(encrypted);
    }

    private static SecretKey deriveKey (String password, byte[] salt) throws Exception {
        final SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        final KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        final SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    private static byte[] generateRandomBytes (int length) {
        final byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
