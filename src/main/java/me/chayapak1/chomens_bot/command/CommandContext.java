package me.chayapak1.chomens_bot.command;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandContext {
    public static final Component UNKNOWN_ARGUMENT_COMPONENT = Component
            .text("???")
            .style(Style.style(TextDecoration.UNDERLINED));
    private static final Pattern FLAGS_PATTERN = Pattern
            .compile("^\\s*(?:--|-)([^\\s0-9]\\S*)"); // modified from HBot

    public final Bot bot;

    public final String prefix;

    public final PlayerEntry sender;

    public final boolean inGame;

    public TrustLevel trustLevel = TrustLevel.PUBLIC;

    public String commandName = null;
    public String userInputCommandName = null;

    public String[] fullArgs;
    public String[] args;

    public CommandContext (final Bot bot, final String prefix, final PlayerEntry sender, final boolean inGame) {
        this.bot = bot;
        this.prefix = prefix;
        this.sender = sender;
        this.inGame = inGame;
    }

    public Component displayName () { return Component.empty(); }

    public void sendOutput (final Component component) { }

    // args parsing stuff
    private int argsPosition = 0;

    public String getString (final boolean greedy, final boolean required) throws CommandException { return getString(greedy, required, "string"); }

    public String getString (final boolean greedy, final boolean required, final boolean returnLowerCase) throws CommandException { return getString(greedy, returnLowerCase, required, "string"); }

    private String getString (final boolean greedy, final boolean required, final String type) throws CommandException { return getString(greedy, false, required, type); }

    private String getString (final boolean greedy, final boolean returnLowerCase, final boolean required, final String type) throws CommandException {
        if (argsPosition >= args.length || args[argsPosition] == null) {
            if (required) {
                throw new CommandException(
                        Component.translatable(
                                "arguments_parsing.error.expected_string",
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
                                                                                .translatable("arguments_parsing.hover.usages")
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

        final String greedyString = String.join(" ", Arrays.copyOfRange(args, argsPosition, args.length));

        final StringBuilder string = new StringBuilder();

        if (greedy) {
            string.append(greedyString);
        } else if (
                greedyString.length() > 1 &&
                        (greedyString.startsWith("'") || greedyString.startsWith("\""))
        ) {
            // parses arguments with quotes

            final char quote = greedyString.charAt(0);

            int pointer = 1; // skips quote

            while (true) {
                if (pointer >= greedyString.length()) {
                    if (greedyString.charAt(pointer - 1) != quote) {
                        throw new CommandException(
                                Component
                                        .translatable("arguments_parsing.error.unterminated_quote")
                                        .arguments(
                                                Component.text(greedyString, bot.colorPalette.string),
                                                Component.text(quote, NamedTextColor.YELLOW)
                                        )
                        );
                    }

                    break;
                }

                final char character = greedyString.charAt(pointer);

                pointer++;

                if (character == ' ') {
                    argsPosition++;
                }

                if (character == '\\') {
                    if (pointer >= greedyString.length()) {
                        throw new CommandException(
                                Component
                                        .translatable("arguments_parsing.error.unterminated_escape")
                                        .arguments(
                                                Component
                                                        .text(greedyString)
                                                        .color(bot.colorPalette.string)
                                        )
                        );
                    }

                    final char nextCharacter = greedyString.charAt(pointer); // pointer is already incremented above

                    final char toAdd = switch (nextCharacter) {
                        case 'n' -> '\n';
                        case 't' -> '\t';
                        case 'r' -> '\r';
                        default -> nextCharacter;
                    };

                    string.append(toAdd);

                    pointer++;
                } else if (character == quote) {
                    break;
                } else {
                    string.append(character);
                }
            }
        } else {
            // else just get the current argument
            string.append(args[argsPosition]);
        }

        argsPosition++;

        final String result = string.toString();

        return returnLowerCase ? result.toLowerCase() : result;
    }

    public String getAction () throws CommandException {
        return getString(false, true, true, "action");
    }

    public List<String> getFlags (final String... allowedFlags) throws CommandException { return getFlags(false, allowedFlags); }

    public List<String> getFlags (final boolean returnLowerCase, final String... allowedFlags) throws CommandException {
        final List<String> flags = new ArrayList<>();

        String flag = getFlag(returnLowerCase, allowedFlags);

        while (flag != null) {
            flags.add(flag);
            flag = getFlag(returnLowerCase, allowedFlags);
        }

        return flags;
    }

    private String getFlag (final boolean returnLowerCase, final String[] allowedFlagsArray) throws CommandException {
        final List<String> allowedFlags = Arrays.asList(allowedFlagsArray);

        final String string = getString(false, false, returnLowerCase);

        if (string.isBlank()) return null;

        final Matcher matcher = FLAGS_PATTERN.matcher(string);

        if (matcher.find()) {
            final String match = matcher.group(1);

            if (allowedFlags.contains(match)) return match;
        }

        argsPosition--; // getString incremented argsPosition

        return null;
    }

    public Integer getInteger (final boolean required) throws CommandException {
        final String string = getString(false, required, "integer");

        if (string.isEmpty()) return null;

        try {
            return Integer.parseInt(string);
        } catch (final NumberFormatException e) {
            throw new CommandException(Component.translatable("arguments_parsing.error.invalid_type", Component.text("integer")));
        }
    }

    public Long getLong (final boolean required) throws CommandException {
        final String string = getString(false, required, "long");

        if (string.isEmpty()) return null;

        try {
            return Long.parseLong(string);
        } catch (final NumberFormatException e) {
            throw new CommandException(Component.translatable("arguments_parsing.error.invalid_type", Component.text("long")));
        }
    }

    public Double getDouble (final boolean required, final boolean allowInfinite) throws CommandException {
        final String string = getString(false, required, "double");

        if (string.isEmpty()) return null;

        try {
            final double parsedDouble = Double.parseDouble(string);

            if (!Double.isFinite(parsedDouble) && !allowInfinite) throw new NumberFormatException();
            else return parsedDouble;
        } catch (final NumberFormatException e) {
            throw new CommandException(Component.translatable("arguments_parsing.error.invalid_type", Component.text("double")));
        }
    }

    public Float getFloat (final boolean required, final boolean allowInfinite) throws CommandException {
        final String string = getString(false, required, "float");

        if (string.isEmpty()) return null;

        try {
            final float parsedFloat = Float.parseFloat(string);

            if (!Float.isFinite(parsedFloat) && !allowInfinite) throw new NumberFormatException();
            else return parsedFloat;
        } catch (final NumberFormatException e) {
            throw new CommandException(Component.translatable("arguments_parsing.error.invalid_type", Component.text("float")));
        }
    }

    public Boolean getBoolean (final boolean required) throws CommandException {
        final String string = getString(false, required, "boolean");

        if (string.isEmpty()) return null;

        return switch (string) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new CommandException(Component.translatable("arguments_parsing.error.invalid_type", Component.text("boolean")));
        };
    }

    public <T extends Enum<T>> T getEnum (final Class<T> enumClass) throws CommandException {
        final String string = getString(false, true, enumClass.getSimpleName());

        try {
            return Enum.valueOf(enumClass, string.toUpperCase());
        } catch (final IllegalArgumentException | NullPointerException e) {
            final T[] values = enumClass.getEnumConstants();

            throw new CommandException(
                    Component.translatable(
                            "arguments_parsing.error.invalid_enum",
                            Component.text(enumClass.getSimpleName()),
                            Component.text(Arrays.toString(values))
                    )
            );
        }
    }

    public void checkOverloadArgs (final int maximumArgs) throws CommandException {
        final String joined = String.join(" ", args);

        final String quotesReplaced = joined.replaceAll("([\"'])(?:\\.|(?!\1).)*\1", "i");

        final int count = quotesReplaced.isBlank() ?
                0 :
                quotesReplaced.split("\\s+").length;

        if (count > maximumArgs) throw new CommandException(
                Component.translatable(
                        "arguments_parsing.error.too_many_arguments",
                        Component.text(maximumArgs)
                )
        );
    }
}
