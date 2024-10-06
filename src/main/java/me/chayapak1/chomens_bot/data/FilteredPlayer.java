package me.chayapak1.chomens_bot.data;

public class FilteredPlayer {
    public final String playerName;
    public final boolean regex;
    public final boolean ignoreCase;

    public FilteredPlayer (
            String playerName,
            boolean regex,
            boolean ignoreCase
    ) {
        this.playerName = playerName;
        this.regex = regex;
        this.ignoreCase = ignoreCase;
    }
}
