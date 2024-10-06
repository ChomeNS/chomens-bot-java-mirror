package me.chayapak1.chomens_bot.util;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.awt.*;

public class ColorUtilities {
    public static TextColor getColorByString (String _color) {
        final String color = _color.toLowerCase();

        if (color.startsWith("#")) return TextColor.fromHexString(color);
        else {
            // am i reinventing the wheel here?
            return switch (color) {
                case "black" -> NamedTextColor.BLACK;
                case "dark_blue" -> NamedTextColor.DARK_BLUE;
                case "dark_green" -> NamedTextColor.DARK_GREEN;
                case "dark_aqua" -> NamedTextColor.DARK_AQUA;
                case "dark_red" -> NamedTextColor.DARK_RED;
                case "dark_purple" -> NamedTextColor.DARK_PURPLE;
                case "gold" -> NamedTextColor.GOLD;
                case "gray" -> NamedTextColor.GRAY;
                case "dark_gray" -> NamedTextColor.DARK_GRAY;
                case "blue" -> NamedTextColor.BLUE;
                case "green" -> NamedTextColor.GREEN;
                case "aqua" -> NamedTextColor.AQUA;
                case "red" -> NamedTextColor.RED;
                case "light_purple" -> NamedTextColor.LIGHT_PURPLE;
                case "yellow" -> NamedTextColor.YELLOW;
                default -> NamedTextColor.WHITE;
            };
        }
    }

    // Author: ChatGPT
    public static int hsvToRgb (int hue, int saturation, int value) {
        Color color = Color.getHSBColor(hue / 360.0f, saturation / 100.0f, value / 100.0f);
        return color.getRGB() & 0xFFFFFF;
    }

    private static final ChatColor[] COLORS = {
            new ChatColor("0", 0x000000),
            new ChatColor("1", 0x0000aa),
            new ChatColor("2", 0x00aa00),
            new ChatColor("3", 0x00aaaa),
            new ChatColor("4", 0xaa0000),
            new ChatColor("5", 0xaa00aa),
            new ChatColor("6", 0xffaa00),
            new ChatColor("7", 0xaaaaaa),
            new ChatColor("8", 0x555555),
            new ChatColor("9", 0x5555ff),
            new ChatColor("a", 0x55ff55),
            new ChatColor("b", 0x55ffff),
            new ChatColor("c", 0xff5555),
            new ChatColor("d", 0xff55ff),
            new ChatColor("e", 0xffff55),
            new ChatColor("f", 0xffffff)
    };

    public static String getClosestChatColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        ChatColor closest = null;
        int smallestDiff = 0;

        for (ChatColor color : COLORS) {
            if (color.rgb == rgb) {
                return color.colorName;
            }

            // Check by the greatest diff of the 3 values
            int rAverage = (color.r + r) / 2;
            int rDiff = color.r - r;
            int gDiff = color.g - g;
            int bDiff = color.b - b;
            int diff = ((2 + (rAverage >> 8)) * rDiff * rDiff)
                    + (4 * gDiff * gDiff)
                    + ((2 + ((255 - rAverage) >> 8)) * bDiff * bDiff);
            if (closest == null || diff < smallestDiff) {
                closest = color;
                smallestDiff = diff;
            }
        }
        return closest.colorName;
    }

    public static final class ChatColor {

        private final String colorName;
        private final int rgb;
        private final int r, g, b;

        public ChatColor(String colorName, int rgb) {
            this.colorName = colorName;
            this.rgb = rgb;
            r = (rgb >> 16) & 0xFF;
            g = (rgb >> 8) & 0xFF;
            b = rgb & 0xFF;
        }
    }
}
