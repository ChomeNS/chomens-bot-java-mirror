package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
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

    public ScreensharePlugin (Bot bot) {
        this.bot = bot;

        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void start (Vector3i position) {
        screen = new Screen(bot, width, height, position);

        screen.update();

//        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("/tmp/rick.mp4")) {
//            this.grabber = grabber;
//        } catch (Exception e) {
//            e.printStackTrace();
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
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

        BufferedImage capture = robot.createScreenCapture(screenRect);

//        try (Java2DFrameConverter frameConverter = new Java2DFrameConverter()) {
//            final Frame grabbed = grabber.grab();

//            final BufferedImage capture = frameConverter.convert(grabbed);

            if (capture == null) return;

            BufferedImage resized = resize(capture, screen.width, screen.height);

            for (int y = 0; y < resized.getHeight(); y++) {
                for (int x = 0; x < resized.getWidth(); x++) {
                    int rgba = resized.getRGB(x, y);
                    int red = (rgba >> 16) & 255;
                    int green = (rgba >> 8) & 255;
                    int blue = rgba & 255;

                    screen.screen[x][y] = String.format("#%02x%02x%02x", red, green, blue);
                }
            }

            screen.draw();
//        } catch (Exception e) {
//            System.err.println("EXCEPTION ::::");
//            e.printStackTrace();
//        }
    }

    // move this to util?
    private BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    public static class Screen {
        private final Bot bot;

        public String[][] screen;
        public int width;
        public int height;
        public Vector3i pos;

        public ArrayList<String> tags = new ArrayList<>();

        public Screen(Bot bot, int width, int height, Vector3i pos) {
            screen = new String[width][height];

            this.bot = bot;

            this.width = width;
            this.height = height;

            this.pos = pos;
        }

        public void draw () {
            final ArrayList<Component> names = new ArrayList<>();

            for (int y = 0; y < height; y++) {
                Component name = Component.empty();

                for (int x = 0; x < width; x++) {
                    final Component pixel = Component.text("█").color(TextColor.fromHexString(screen[x][y]));

                    name = name.append(pixel);
                }

                names.add(name);
            }

            for (int i = 0; i < names.size(); i++) {
                bot.core.run("minecraft:data merge entity @e[tag=" + tags.get(i) + ",limit=1] {text:'" + GsonComponentSerializer.gson().serialize(names.get(i)) + "'}");
            }
        }

        public void kill () {
            for(String i : tags) {
                bot.core.run("minecraft:kill @e[tag=" + i + "]");
            }

            tags.clear();
        }

        public void update() {
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

        public void setPixel(String hexColor, int x, int y) { screen[x][y] = hexColor; }

        public void setRow(String[] hexColor, int row) {
            for (int x = 0; x < width; x++) {
                screen[x][row] = hexColor[x];
            }
        }
    }
}
