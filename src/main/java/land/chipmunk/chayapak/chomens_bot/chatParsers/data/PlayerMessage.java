package land.chipmunk.chayapak.chomens_bot.chatParsers.data;

import lombok.Data;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;

import java.util.Map;

@Data
@AllArgsConstructor
public class PlayerMessage {
    private Map<String, Component> parameters;
    private MutablePlayerListEntry sender;
}
