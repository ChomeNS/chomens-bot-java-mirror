package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.SNBTUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.cloudburstmc.math.vector.Vector3i;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScreensharePlugin {
    private final Bot bot;

    private ScheduledFuture<?> future;

    public int fps = 15;

    public int width = 35;
    public int height = 18;

    public Screen screen;

    public Robot robot;

    //    public FFmpegFrameGrabber grabber;

    public ScreensharePlugin (final Bot bot) {
        this.bot = bot;

        try {
            robot = new Robot();
        } catch (final AWTException e) {
            bot.logger.error(e);
        }
    }

    public void start (final Vector3i position) {
        screen = new Screen(bot, width, height, position);

        screen.update();

        //        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("/tmp/rick.mp4")) {
        //            this.grabber = grabber;
        //        } catch (Exception e) {
        //            bot.logger.error(e);
        //        }
        //
        //        try { grabber.start(); } catch (Exception ignored) {}

        future = bot.executor.scheduleAtFixedRate(this::drawScreen, 0, 1000 / fps, TimeUnit.MILLISECONDS); // frame. per. second.
    }

    public void stop () {
        future.cancel(false);

        screen.kill();
    }

    private void drawScreen () {
        final Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

        final BufferedImage capture = robot.createScreenCapture(screenRect);

        //        try (Java2DFrameConverter frameConverter = new Java2DFrameConverter()) {
        //            final Frame grabbed = grabber.grab();

        //            final BufferedImage capture = frameConverter.convert(grabbed);

        if (capture == null) return;

        final BufferedImage resized = resize(capture, screen.width, screen.height);

        for (int y = 0; y < resized.getHeight(); y++) {
            for (int x = 0; x < resized.getWidth(); x++) {
                final int rgba = resized.getRGB(x, y);
                final int red = (rgba >> 16) & 255;
                final int green = (rgba >> 8) & 255;
                final int blue = rgba & 255;

                screen.screen[x][y] = String.format("#%02x%02x%02x", red, green, blue);
            }
        }

        screen.draw();
        //        } catch (Exception e) {
        //            System.err.println("EXCEPTION ::::");
        //            bot.logger.error(e);
        //        }
    }

    // move this to util?
    private BufferedImage resize (final BufferedImage img, final int newW, final int newH) {
        final Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        final BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    public static class Screen {
        private final Bot bot;

        public String[][] screen;
        public final int width;
        public final int height;
        public final Vector3i pos;

        public final ArrayList<String> tags = new ArrayList<>();

        public Screen (final Bot bot, final int width, final int height, final Vector3i pos) {
            screen = new String[width][height];

            this.bot = bot;

            this.width = width;
            this.height = height;

            this.pos = pos;
        }

        public void draw () {
            final ArrayList<Component> names = new ArrayList<>();

            for (int y = 0; y < height; y++) {
                final TextComponent.Builder name = Component.text();

                for (int x = 0; x < width; x++) {
                    final Component pixel = Component.text("█", TextColor.fromHexString(screen[x][y]));

                    name.append(pixel);
                }

                names.add(name.build());
            }

            for (int i = 0; i < names.size(); i++) {
                bot.core.run("minecraft:data merge entity @e[tag=" + tags.get(i) + ",limit=1] {text:" + SNBTUtilities.fromComponent(bot.options.useSNBTComponents, names.get(i)) + "}");
            }
        }

        public void kill () {
            for (final String i : tags) {
                bot.core.run("minecraft:kill @e[tag=" + i + "]");
            }

            tags.clear();
        }

        public void update () {
            double startY = pos.getY();

            kill();

            for (int i = 0; i < this.height; i++) {
                final String actualTag = "chomens_bot_" + Math.random();

                tags.add(actualTag);
                startY -= 0.3f;

                bot.core.run(
                        String.format(
                                "minecraft:summon minecraft:text_display %s %s %s %s",
                                pos.getX(),
                                startY,
                                pos.getZ(),
                                "{Tags:[\"" + actualTag + "\"],text:'\"\"',line_width:32767}"
                        )
                );
            }
        }

        public void setPixel (final String hexColor, final int x, final int y) { screen[x][y] = hexColor; }

        public void setRow (final String[] hexColor, final int row) {
            for (int x = 0; x < width; x++) {
                screen[x][row] = hexColor[x];
            }
        }
    }
}
