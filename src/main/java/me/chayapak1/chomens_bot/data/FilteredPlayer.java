package me.chayapak1.chomens_bot.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FilteredPlayer {
    public final String playerName;
    public final boolean regex;
    public final boolean ignoreCase;

    @JsonCreator
    public FilteredPlayer (
            @JsonProperty("playerName") String playerName,
            @JsonProperty("regex") boolean regex,
            @JsonProperty("ignoreCase") boolean ignoreCase
    ) {
        this.playerName = playerName;
        this.regex = regex;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public String toString() {
        return "FilteredPlayer{" +
                "playerName='" + playerName + '\'' +
                ", regex=" + regex +
                ", ignoreCase=" + ignoreCase +
                '}';
    }
}
