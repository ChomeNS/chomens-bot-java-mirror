package me.chayapak1.chomensbot_mabe.command;

import net.kyori.adventure.text.Component;

import java.util.List;

public interface Command {
    String description();
    List<String> usage();
    Component execute(CommandContext context, String[] args) throws Exception;
}
