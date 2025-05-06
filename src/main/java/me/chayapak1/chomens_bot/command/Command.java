package me.chayapak1.chomens_bot.command;

import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import net.kyori.adventure.text.Component;

import java.util.Arrays;

public abstract class Command {
    public final String name;
    public String[] usages;
    public final String[] aliases;
    public final TrustLevel trustLevel;
    public final boolean consoleOnly;
    public ChatPacketType[] disallowedPacketTypes;

    public Command (
            final String name,
            final String[] usages,
            final String[] aliases,
            final TrustLevel trustLevel
    ) {
        this.name = name;
        this.usages = usages;
        this.aliases = aliases;
        this.trustLevel = trustLevel;
        this.consoleOnly = false;
    }

    public Command (
            final String name,
            final String[] usages,
            final String[] aliases,
            final TrustLevel trustLevel,
            final boolean consoleOnly
    ) {
        this.name = name;
        this.usages = usages;
        this.aliases = aliases;
        this.trustLevel = trustLevel;
        this.consoleOnly = consoleOnly;
    }

    public Command (
            final String name,
            final String[] usages,
            final String[] aliases,
            final TrustLevel trustLevel,
            final boolean consoleOnly,
            final ChatPacketType[] disallowedPacketTypes
    ) {
        this.name = name;
        this.usages = usages;
        this.aliases = aliases;
        this.trustLevel = trustLevel;
        this.consoleOnly = consoleOnly;
        this.disallowedPacketTypes = disallowedPacketTypes;
    }

    public abstract Component execute (CommandContext context) throws Exception;

    @Override
    public String toString () {
        return "Command{" +
                "name='" + name + '\'' +
                ", usages=" + Arrays.toString(usages) +
                ", aliases=" + Arrays.toString(aliases) +
                ", trustLevel=" + trustLevel +
                ", consoleOnly=" + consoleOnly +
                ", disallowedPacketTypes=" + Arrays.toString(disallowedPacketTypes) +
                '}';
    }
}
