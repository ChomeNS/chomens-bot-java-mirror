package land.chipmunk.chayapak.chomens_bot.data;

public enum BossBarStyle {
    NOTCHED_10("notched_10"),
    NOTCHED_12("notched_12"),
    NOTCHED_20("notched_20"),
    NOTCHED_6("notched_6"),
    PROGRESS("progress");

    public final String style;

    BossBarStyle (String style) {
        this.style = style;
    }
}
