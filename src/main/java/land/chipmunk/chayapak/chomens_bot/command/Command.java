package land.chipmunk.chayapak.chomens_bot.command;

import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Collections;
import java.util.List;

public class Command {
    public String name = "unnamed" + RandomStringUtils.randomNumeric(4);
    public String description = "No Description";
    public List<String> usage () {
        return Collections.emptyList();
    }
    public List<String> alias () {
        return Collections.emptyList();
    }
    public int trustLevel = 0;

    public Component execute(CommandContext context, String[] args, String[] fullArgs) throws Exception {
        return null;
    }
}
