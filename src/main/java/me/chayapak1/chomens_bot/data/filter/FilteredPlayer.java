package me.chayapak1.chomens_bot.data.filter;

public record FilteredPlayer(String playerName, String reason, boolean regex, boolean ignoreCase) {
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
