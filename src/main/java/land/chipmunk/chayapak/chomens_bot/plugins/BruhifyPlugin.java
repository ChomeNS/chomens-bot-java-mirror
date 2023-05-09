package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.concurrent.TimeUnit;

public class BruhifyPlugin {
    @Getter @Setter private String bruhifyText = "";

    private int startHue = 0;

    public BruhifyPlugin (Bot bot) {
        bot.executor().scheduleAtFixedRate(() -> {
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

            bot.chat().actionBar(component);

            startHue = (startHue + increment) % 360;
        }, 50, 100, TimeUnit.MILLISECONDS);
    }
}
