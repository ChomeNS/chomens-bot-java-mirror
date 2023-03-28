package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.Timer;
import java.util.TimerTask;

public class BruhifyPlugin {
    @Getter @Setter private String bruhifyText = "";

    private int startHue = 0;

    public BruhifyPlugin (Bot bot) {
        final Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (bruhifyText.equals("")) return;

                int hue = startHue;
                String displayName = bruhifyText;
                int increment = (int)(360.0 / Math.max(displayName.length(), 20));

                Component component = Component.empty();

                for (char character : displayName.toCharArray()) {
                    String color = String.format("#%06x", ColorUtilities.hsvToRgb(hue, 100, 100));
                    component = component.append(Component.text(character).color(TextColor.fromHexString(color)));
                    hue = (hue + increment) % 360;
                }

                bot.core().run("minecraft:title @a actionbar " + GsonComponentSerializer.gson().serialize(component));

                startHue = (startHue + increment) % 360;
            }
        }, 50, 100);
    }
}
