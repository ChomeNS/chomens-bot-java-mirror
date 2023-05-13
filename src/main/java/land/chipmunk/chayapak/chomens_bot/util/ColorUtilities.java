package land.chipmunk.chayapak.chomens_bot.util;

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
}
