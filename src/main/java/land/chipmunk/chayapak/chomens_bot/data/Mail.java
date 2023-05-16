package land.chipmunk.chayapak.chomens_bot.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.time.DateTime;

@AllArgsConstructor
public class Mail {
    @Getter private final String sentBy;
    @Getter private final String sentTo;
    @Getter private final DateTime timeSent;
    @Getter private final String server;
    @Getter private final String contents;
}
