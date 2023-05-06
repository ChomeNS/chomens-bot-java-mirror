package land.chipmunk.chayapak.chomens_bot.util;

import java.awt.*;

// Author: ChatGPT
public class ColorUtilities {
    public static int hsvToRgb (int hue, int saturation, int value) {
        Color color = Color.getHSBColor(hue / 360.0f, saturation / 100.0f, value / 100.0f);
        return color.getRGB() & 0xFFFFFF;
    }
}
