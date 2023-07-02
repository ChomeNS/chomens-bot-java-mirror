package land.chipmunk.chayapak.chomens_bot.data;

public class FilteredPlayer {
    public String playerName;
    public boolean regex;
    public boolean ignoreCase;

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
