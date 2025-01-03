package me.chayapak1.chomens_bot.command;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Arrays;

public class CommandContext {
    public static final Component UNKNOWN_ARGUMENT_COMPONENT = Component.text("???").style(Style.style(TextDecoration.UNDERLINED));

    public final Bot bot;

    public final String prefix;

    public final PlayerEntry sender;

    public final boolean inGame;

    public TrustLevel trustLevel = TrustLevel.PUBLIC;

    public String commandName = null;
    public String userInputCommandName = null;

    public String[] fullArgs;
    public String[] args;

    public CommandContext (Bot bot, String prefix, PlayerEntry sender, boolean inGame) {
        this.bot = bot;
        this.prefix = prefix;
        this.sender = sender;
        this.inGame = inGame;
    }

    public Component displayName () { return Component.empty(); }
    public void sendOutput (Component component) {}

    // args parsing stuff
    private int argsPosition = 0;

    public String getString (boolean greedy, boolean required) throws CommandException { return getString(greedy, required, "string"); }
    public String getString (boolean greedy, boolean required, boolean returnLowerCase) throws CommandException { return getString(greedy, returnLowerCase, required, "string"); }
    private String getString (boolean greedy, boolean required, String type) throws CommandException { return getString(greedy, false, required, type); }
    private String getString (boolean greedy, boolean returnLowerCase, boolean required, String type) throws CommandException {
        if (argsPosition >= args.length || args[argsPosition] == null) {
            if (required) {
                throw new CommandException(
                        Component.translatable(
                                "Expected %s at position %s (%s %s)%s",
                                Component.text(type),
                                Component.text(argsPosition),
                                Component.text(prefix + userInputCommandName),
                                argsPosition == 0 ?
                                        UNKNOWN_ARGUMENT_COMPONENT :
                                        Component
                                                .text(String.join(" ", args))
                                                .append(Component.space())
                                                .append(UNKNOWN_ARGUMENT_COMPONENT),
                                inGame ?
                                        Component
                                                .space()
                                                .append(
                                                        Component.translatable("[%s]")
                                                                .arguments(
                                                                        Component
                                                                                .text("click for usages")
                                                                                .clickEvent(ClickEvent.suggestCommand(prefix + "help " + this.commandName))
                                                                )
                                                ) :
                                        Component.empty()
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

        return returnLowerCase ? string.toLowerCase() : string;
    }

    public String getAction () throws CommandException {
        return getString(false, true, true, "action");
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

    public void checkOverloadArgs (int maximumArgs) throws CommandException {
        if (args.length > maximumArgs) throw new CommandException(
                Component.translatable(
                        "Too many arguments, expected %s max",
                        Component.text(maximumArgs)
                )
        );
    }
}
