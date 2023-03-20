package me.chayapak1.chomensbot_mabe.util;

public class NumberUtilities {
    public static int between (int min, int max) {
        return (int) Math.floor(
                Math.random() * (max - min) + min
        );
    }
}
