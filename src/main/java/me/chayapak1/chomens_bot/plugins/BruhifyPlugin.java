package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BruhifyPlugin implements Listener {
    private final Bot bot;

    public String bruhifyText = "";

    private int startHue = 0;

    public BruhifyPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    @Override
    public void onTick () {
        if (bruhifyText.isBlank()) return;

        final int increment = 360 / Math.max(bruhifyText.length(), 20);

        final List<Component> components = new ArrayList<>();

        final AtomicInteger hue = new AtomicInteger(startHue);
        bruhifyText.codePoints()
                .forEach(
                        character -> {
                            components.add(Component.text(new String(Character.toChars(character)))
                                                    .color(TextColor.color(HSVLike.hsvLike(hue.get() / 360.0f, 1, 1))));
                            hue.set((hue.get() + increment) % 360);
                        }
                );

        bot.chat.actionBar(
                Component.join(JoinConfiguration.noSeparators(), components)
        );

        startHue = (startHue + increment) % 360;
    }
}
