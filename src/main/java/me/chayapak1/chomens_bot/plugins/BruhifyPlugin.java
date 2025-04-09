package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;

public class BruhifyPlugin implements TickPlugin.Listener {
    private final Bot bot;

    public String bruhifyText = "";

    private int startHue = 0;

    public BruhifyPlugin (final Bot bot) {
        this.bot = bot;

        bot.tick.addListener(this);
    }

    @Override
    public void onTick () {
        if (bruhifyText.isEmpty()) return;

        int hue = startHue;
        final String displayName = bruhifyText;
        final int increment = 360 / Math.max(displayName.length(), 20);

        Component component = Component.empty();

        for (final char character : displayName.toCharArray()) {
            component = component.append(Component.text(character)
                                                 .color(TextColor.color(HSVLike.hsvLike(hue / 360.0f, 1, 1))));
            hue = (hue + increment) % 360;
        }

        bot.chat.actionBar(component);

        startHue = (startHue + increment) % 360;
    }
}
