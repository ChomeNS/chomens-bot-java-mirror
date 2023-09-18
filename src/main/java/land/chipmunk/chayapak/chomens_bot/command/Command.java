package land.chipmunk.chayapak.chomens_bot.command;

import net.kyori.adventure.text.Component;

public abstract class Command {
    public final String name;
    public final String description;
    public final String[] usages;
    public final String[] aliases;
    public final TrustLevel trustLevel;
    public final boolean consoleOnly;

    public Command (
            String name,
            String description,
            String[] usages,
            String[] aliases,
            TrustLevel trustLevel,
            boolean consoleOnly
    ) {
        this.name = name;
        this.description = description;
        this.usages = usages;
        this.aliases = aliases;
        this.trustLevel = trustLevel;
        this.consoleOnly = consoleOnly;
    }

    public abstract Component execute (CommandContext context) throws Exception;
}
