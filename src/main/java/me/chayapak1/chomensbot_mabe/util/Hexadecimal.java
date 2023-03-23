package me.chayapak1.chomensbot_mabe.util;

public interface Hexadecimal {
    static String encode (byte b) {
        return "" + Character.forDigit((b >> 4) & 0xF, 16) + Character.forDigit((b & 0xF), 16);
    }

    static String encode (byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) sb.append(encode(array[i]));
        return sb.toString();
    }

    // TODO: Decode
}

