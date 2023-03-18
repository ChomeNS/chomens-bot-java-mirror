package me.chayapak1.chomensbot_mabe.commands;

import com.github.ricksbrown.cowsay.plugin.CowExecutor;
import me.chayapak1.chomensbot_mabe.command.Command;
import me.chayapak1.chomensbot_mabe.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CowsayCommand implements Command {
    public String description() {
        return "Moo";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<cow> <message>");

        return usages;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final String cow = args[0];
        final String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        final CowExecutor cowExecutor = new CowExecutor();
        cowExecutor.setCowfile(cow);
        cowExecutor.setMessage(message);

        final String result = cowExecutor.execute();

        context.sendOutput(Component.text(result));

        return Component.text("success");
    }
}
