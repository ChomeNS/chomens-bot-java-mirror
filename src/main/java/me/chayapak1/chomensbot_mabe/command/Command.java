package me.chayapak1.chomensbot_mabe.command;

import net.kyori.adventure.text.Component;

import java.util.List;

public interface Command {
    String description();
    List<String> usage();
    int trustLevel();

    Component execute(CommandContext context, String[] args, String[] fullArgs) throws Exception;
}
