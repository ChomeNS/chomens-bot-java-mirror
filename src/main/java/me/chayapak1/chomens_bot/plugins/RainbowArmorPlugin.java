package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class RainbowArmorPlugin implements Listener {
    private static final int[] LEATHER_ARMORS = new int[] {
            896, // helmet
            897, // chestplate
            898, // leggings
            899 // boots
    };

    private final Bot bot;

    private float rainbowHue = 0F;

    public RainbowArmorPlugin (final Bot bot) {
        this.bot = bot;

        if (!bot.config.rainbowArmor) return;

        bot.listener.addListener(this);
    }

    @Override
    public void onTick () {
        if (!bot.config.rainbowArmor || !bot.selfCare.visible) return;

        final int increment = 360 / 20;
        final Color color = Color.getHSBColor(rainbowHue / 360.0f, 1, 1);
        final int rgbColor = color.getRGB() & 0xFFFFFF;
        rainbowHue = (rainbowHue + increment) % 360;

        final Map<DataComponentType<?>, DataComponent<?, ?>> map = new HashMap<>();

        final IntComponentType type = DataComponentTypes.DYED_COLOR;
        final IntComponentType.IntDataComponentFactory factory =
                (IntComponentType.IntDataComponentFactory) type.getDataComponentFactory();

        map.put(
                type,
                        factory.createPrimitive(
                                type,
                                rgbColor
                        )
        );

        final DataComponents dataComponents = new DataComponents(map);

        for (int i = 0; i < 4; i++) {
            bot.session.send(
                    new ServerboundSetCreativeModeSlotPacket(
                            (short) (i + 5), // 5, 6, 7, 8 are armors
                            new ItemStack(LEATHER_ARMORS[i], 1, dataComponents)
                    )
            );
        }
    }
}
