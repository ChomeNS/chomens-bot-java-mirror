package me.chayapak1.chomens_bot.util;

public class NumberUtilities {
    public static int between (int min, int max) {
        return (int) Math.floor(
                Math.random() * (max - min) + min
        );
    }

    public static float clamp (float value, float min, float max) {
        if (value < min) return min;
        return Math.min(value, max);
    }
}
