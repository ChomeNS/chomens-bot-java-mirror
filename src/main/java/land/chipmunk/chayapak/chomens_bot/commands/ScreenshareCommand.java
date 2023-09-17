package land.chipmunk.chayapak.chomens_bot.commands;

public class ScreenshareCommand { // extends Command {
//    public ScreenshareCommand () {
//        super(
//                "screenshare",
//                "Shares my screen",
//                new String[] {
//                        "<start> <x> <y> <z>",
//                        "<stop>",
//                        "<setres> <width> <height>",
//                        "<setfps> <fps>"
//                },
//                new String[] {},
//                TrustLevel.TRUSTED,
//                false
//        );
//    }

//    @Override
//    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
//        final Bot bot = context.bot;
//
//        try {
//            switch (args[0]) {
//                case "start" -> {
//                    final int x = Integer.parseInt(args[1]);
//                    final int y = Integer.parseInt(args[2]);
//                    final int z = Integer.parseInt(args[3]);
//
//                    bot.screenshare.start(Vector3i.from(x, y, z));
//
//                    return Component
//                            .text("Started screen sharing")
//                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
//                }
//                case "stop" -> {
//                    bot.screenshare.stop();
//
//                    return Component
//                            .text("Stopped screen sharing")
//                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
//                }
//                case "setres" -> {
//                    final int width = Integer.parseInt(args[1]);
//                    final int height = Integer.parseInt(args[2]);
//
//                    bot.screenshare.width = width;
//                    bot.screenshare.height = height;
//
//                    bot.screenshare.screen.screen = new String[width][height];
//
//                    return Component
//                            .text("Set the resolution to ")
//                            .append(Component.text(width + "x" + height).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)))
//                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
//                }
//                case "setfps" -> {
//                    final int fps = Integer.parseInt(args[1]);
//
//                    bot.screenshare.fps = fps;
//
//                    return Component
//                            .text("Set the FPS to ")
//                            .append(Component.text(fps).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)))
//                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
//                }
//                default -> {
//                    return Component.text("Invalid action").color(NamedTextColor.RED);
//                }
//            }
//        } catch (NumberFormatException e) {
//            return Component.text("Invalid integer").color(NamedTextColor.RED);
//        }
//    }
}
