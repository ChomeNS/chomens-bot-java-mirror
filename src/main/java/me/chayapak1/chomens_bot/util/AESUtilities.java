package me.chayapak1.chomens_bot.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

public class AESUtilities {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CIPHER = "AES/CBC/PKCS5Padding";

    public static byte[] getBytesFromUUID (final UUID uuid) {
        final ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private static byte[] generateIV () {
        final byte[] iv = new byte[16];
        RANDOM.nextBytes(iv);
        return iv;
    }

    private static SecretKeySpec createKeySpec (final UUID secret) {
        return new SecretKeySpec(getBytesFromUUID(secret), "AES");
    }

    public static byte[] encrypt (final UUID secret, final byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        final byte[] iv = generateIV();
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, createKeySpec(secret), ivSpec);
        final byte[] enc = cipher.doFinal(data);
        final byte[] payload = new byte[iv.length + enc.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(enc, 0, payload, iv.length, enc.length);
        return payload;
    }

    public static byte[] decrypt (final UUID secret, final byte[] payload) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        final byte[] iv = new byte[16];
        System.arraycopy(payload, 0, iv, 0, iv.length);
        final byte[] data = new byte[payload.length - iv.length];
        System.arraycopy(payload, iv.length, data, 0, data.length);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, createKeySpec(secret), ivSpec);
        return cipher.doFinal(data);
    }

}
