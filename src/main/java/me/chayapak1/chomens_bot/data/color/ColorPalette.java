package me.chayapak1.chomens_bot.data.color;

import me.chayapak1.chomens_bot.Configuration;
import net.kyori.adventure.text.format.TextColor;

import static me.chayapak1.chomens_bot.util.ColorUtilities.getColorByString;

public class ColorPalette {
    public final TextColor primary;
    public final TextColor secondary;
    public final TextColor defaultColor;
    public final TextColor username;
    public final TextColor uuid;
    public final TextColor string;
    public final TextColor number;
    public final TextColor ownerName;

    public ColorPalette (final Configuration.ColorPalette configColorPalette) {
        this.primary = getColorByString(configColorPalette.primary);
        this.secondary = getColorByString(configColorPalette.secondary);
        this.defaultColor = getColorByString(configColorPalette.defaultColor);
        this.username = getColorByString(configColorPalette.username);
        this.uuid = getColorByString(configColorPalette.uuid);
        this.string = getColorByString(configColorPalette.string);
        this.number = getColorByString(configColorPalette.number);
        this.ownerName = getColorByString(configColorPalette.ownerName);
    }
}