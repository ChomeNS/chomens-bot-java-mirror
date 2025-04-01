package me.chayapak1.chomens_bot.command;

import net.kyori.adventure.text.Component;

import java.util.Arrays;

public abstract class Command {
    public final String name;
    public final String description;
    public String[] usages;
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

    @Override
    public String toString () {
        return "Command{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", usages=" + Arrays.toString(usages) +
                ", aliases=" + Arrays.toString(aliases) +
                ", trustLevel=" + trustLevel +
                ", consoleOnly=" + consoleOnly +
                '}';
    }
}
