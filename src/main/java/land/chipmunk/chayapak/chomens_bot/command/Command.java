package land.chipmunk.chayapak.chomens_bot.command;

import net.kyori.adventure.text.Component;

import java.util.List;

public interface Command {
    String name();
    String description();
    List<String> usage();
    List<String> alias();
    TrustLevel trustLevel();

    Component execute(CommandContext context, String[] args, String[] fullArgs) throws Exception;
}
