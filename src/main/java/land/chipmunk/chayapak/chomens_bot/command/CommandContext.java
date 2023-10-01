package land.chipmunk.chayapak.chomens_bot.command;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Arrays;

public class CommandContext {
    public static final Component UNKNOWN_ARGUMENT_COMPONENT = Component.text("???").style(Style.style(TextDecoration.UNDERLINED));

    public final Bot bot;

    public final String prefix;

    public final PlayerEntry sender;

    public final boolean inGame;

    public String commandName = null;
    public String userInputCommandName = null;

    public String[] fullArgs;
    public String[] args;

    public CommandContext(Bot bot, String prefix, PlayerEntry sender, boolean inGame) {
        this.bot = bot;
        this.prefix = prefix;
        this.sender = sender;
        this.inGame = inGame;
    }

    public Component displayName () { return Component.empty(); }
    public void sendOutput (Component component) {}

    private int argsPosition = 0;

    public String getString (boolean greedy, boolean required) throws CommandException { return getString(greedy, required, "string"); }
    private String getString (boolean greedy, boolean required, String type) throws CommandException {
        if (argsPosition >= args.length || args[argsPosition] == null) {
            if (required) {
                throw new CommandException(
                        Component.translatable(
                                "Expected %s at position %s (%s %s)",
                                Component.text(type),
                                Component.text(argsPosition),
                                Component.text(prefix + userInputCommandName),
                                argsPosition == 0 ?
                                        UNKNOWN_ARGUMENT_COMPONENT :
                                        Component
                                                .text(String.join(" ", args))
                                                .append(Component.space())
                                                .append(UNKNOWN_ARGUMENT_COMPONENT)
                        )
                );
            } else {
                return "";
            }
        }

        final String string = greedy ?
                String.join(" ", Arrays.copyOfRange(args, argsPosition, args.length)) :
                args[argsPosition];

        argsPosition++;

        return string;
    }

    public Integer getInteger (boolean required) throws CommandException {
        final String string = getString(false, required, "integer");

        if (string.isEmpty()) return null;

        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new CommandException(Component.text("Invalid integer"));
        }
    }

    public Double getDouble (boolean required) throws CommandException {
        final String string = getString(false, required, "double");

        if (string.isEmpty()) return null;

        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            throw new CommandException(Component.text("Invalid double"));
        }
    }

    public Float getFloat (boolean required) throws CommandException {
        final String string = getString(false, required, "float");

        if (string.isEmpty()) return null;

        try {
            return Float.parseFloat(string);
        } catch (NumberFormatException e) {
            throw new CommandException(Component.text("Invalid float"));
        }
    }

    public Boolean getBoolean (boolean required) throws CommandException {
        final String string = getString(false, required, "boolean");

        if (string.isEmpty()) return null;

        return switch (string) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new CommandException(Component.text("Invalid boolean"));
        };
    }

    public <T extends Enum<T>> T getEnum (Class<T> enumClass) throws CommandException {
        final String string = getString(false, true, enumClass.getSimpleName());

        try {
            return Enum.valueOf(enumClass, string.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CommandException(Component.text("Invalid enum"));
        }
    }
}
