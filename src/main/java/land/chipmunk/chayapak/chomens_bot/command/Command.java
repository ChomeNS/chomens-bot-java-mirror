package land.chipmunk.chayapak.chomens_bot.command;

import net.kyori.adventure.text.Component;

public abstract class Command {
    public final String name;
    public final String description;
    public final String[] usages;
    public final String[] aliases;
    public final TrustLevel trustLevel;

    public Command (
            String name,
            String description,
            String[] usages,
            String[] aliases,
            TrustLevel trustLevel
    ) {
        this.name = name;
        this.description = description;
        this.usages = usages;
        this.aliases = aliases;
        this.trustLevel = trustLevel;
    }

    public abstract Component execute (CommandContext context, String[] args, String[] fullArgs) throws Exception;
}
