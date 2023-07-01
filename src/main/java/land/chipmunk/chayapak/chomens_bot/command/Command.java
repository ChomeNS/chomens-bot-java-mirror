package land.chipmunk.chayapak.chomens_bot.command;

import lombok.Getter;
import net.kyori.adventure.text.Component;

public abstract class Command {
    @Getter private final String name;
    @Getter private final String description;
    @Getter private final String[] usages;
    @Getter private final String[] aliases;
    @Getter private final TrustLevel trustLevel;

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
