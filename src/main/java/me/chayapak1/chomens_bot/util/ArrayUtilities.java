package me.chayapak1.chomens_bot.util;

public class ArrayUtilities {
    public static boolean isAllTrue (boolean[] array) {
        for (boolean value : array) if (!value) return false;

        return true;
    }
}
