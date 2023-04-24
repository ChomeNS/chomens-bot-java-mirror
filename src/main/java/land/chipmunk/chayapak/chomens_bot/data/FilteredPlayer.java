package land.chipmunk.chayapak.chomens_bot.data;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FilteredPlayer {
    public String playerName;
    public boolean regex;
    public boolean ignoreCase;
}
