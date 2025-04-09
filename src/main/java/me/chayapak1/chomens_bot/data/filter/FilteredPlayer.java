package me.chayapak1.chomens_bot.data.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FilteredPlayer {
    public final String playerName;
    public final String reason;
    public final boolean regex;
    public final boolean ignoreCase;

    @JsonCreator
    public FilteredPlayer (
            @JsonProperty("playerName") final String playerName,
            @JsonProperty("reason") final String reason,
            @JsonProperty("regex") final boolean regex,
            @JsonProperty("ignoreCase") final boolean ignoreCase
    ) {
        this.playerName = playerName;
        this.reason = reason;
        this.regex = regex;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public String toString () {
        return "FilteredPlayer{" +
                "playerName='" + playerName + '\'' +
                ", reason='" + reason + '\'' +
                ", regex=" + regex +
                ", ignoreCase=" + ignoreCase +
                '}';
    }
}
