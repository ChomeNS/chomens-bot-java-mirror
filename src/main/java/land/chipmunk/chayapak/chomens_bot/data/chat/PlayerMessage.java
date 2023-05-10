package land.chipmunk.chayapak.chomens_bot.data.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.kyori.adventure.text.Component;

@Data
@AllArgsConstructor
public class PlayerMessage {
    private MutablePlayerListEntry sender;
    private Component displayName;
    private Component contents;
}
