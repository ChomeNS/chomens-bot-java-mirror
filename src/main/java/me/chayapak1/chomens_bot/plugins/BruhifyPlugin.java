package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class BruhifyPlugin extends TickPlugin.Listener {
    private final Bot bot;

    public String bruhifyText = "";

    private int startHue = 0;

    public BruhifyPlugin (Bot bot) {
        this.bot = bot;

        bot.tick.addListener(this);
    }

    @Override
    public void onTick() {
        if (bruhifyText.isEmpty()) return;

        int hue = startHue;
        String displayName = bruhifyText;
        int increment = 360 / Math.max(displayName.length(), 20);

        Component component = Component.empty();

        for (char character : displayName.toCharArray()) {
            String color = String.format("#%06x", ColorUtilities.hsvToRgb(hue, 100, 100));
            component = component.append(Component.text(character).color(TextColor.fromHexString(color)));
            hue = (hue + increment) % 360;
        }

        bot.chat.actionBar(component);

        startHue = (startHue + increment) % 360;
    }
}
