package land.chipmunk.chayapak.chomens_bot.util;

public class MathUtilities {
    public static int between (int min, int max) {
        return (int) Math.floor(
                Math.random() * (max - min) + min
        );
    }

    public static float clamp (float value, float min, float max) {
        return Math.max(Math.min(value, max), min);
    }

    public static double clamp (double value, double min, double max) {
        return Math.max(Math.min(value, max), min);
    }
}
