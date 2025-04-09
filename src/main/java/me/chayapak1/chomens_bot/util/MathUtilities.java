package me.chayapak1.chomens_bot.util;

public class MathUtilities {
    public static int between (final int min, final int max) {
        return (int) Math.floor(
                Math.random() * (max - min) + min
        );
    }

    public static int clamp (final int value, final int min, final int max) {
        return Math.max(Math.min(value, max), min);
    }

    public static float clamp (final float value, final float min, final float max) {
        return Math.max(Math.min(value, max), min);
    }

    public static double clamp (final double value, final double min, final double max) {
        return Math.max(Math.min(value, max), min);
    }
}
