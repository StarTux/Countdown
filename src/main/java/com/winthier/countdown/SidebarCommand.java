package com.winthier.countdown;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class SidebarCommand implements CommandExecutor {
    final CountdownPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        if (args.length == 1) {
            if ("on".equalsIgnoreCase(args[0])) {
                plugin.setIgnore(player, false);
                player.sendMessage(plugin.format("&bTurned sidebar on"));
            } else if ("off".equalsIgnoreCase(args[0])) {
                plugin.setIgnore(player, true);
                player.sendMessage(plugin.format("&bTurned sidebar off"));
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
}
