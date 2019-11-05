package com.winthier.countdown;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

@RequiredArgsConstructor
class CountdownCommand implements CommandExecutor {
    final CountdownPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            plugin.restart();
            sender.sendMessage("Configuration reloaded");
        } else {
            return false;
        }
        return true;
    }
}
