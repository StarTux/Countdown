package com.winthier.countdown;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class CountdownPlugin extends JavaPlugin implements Listener
{
    // Internal
    Scoreboard scoreboard;
    Objective sidebar;
    long startTime, endTime;
    BukkitRunnable task;
    Set<UUID> ignorers = new HashSet<UUID>();
    int seconds = 0;
    // Config
    boolean enabled;
    String titlePrefix, titleSuffix;
    List<String> messages;
    
    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        getCommand("countdown").setExecutor(new CountdownCommand());
        getCommand("sidebar").setExecutor(new SidebarCommand());
        start();
    }
    
    @Override
    public void onDisable()
    {
        stop();
    }


    class CountdownCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
        {
            if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
                restart();
                sender.sendMessage("Configuration reloaded");
            } else {
                return false;
            }
            return true;
        }
    }

    class SidebarCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
        {
            Player player = sender instanceof Player ? (Player)sender : null;
            if (player == null) {
                sender.sendMessage("Player expected");
                return true;
            }
            if (args.length == 1) {
                if ("on".equalsIgnoreCase(args[0])) {
                    setIgnore(player, false);
                    player.sendMessage(format("&bTurned sidebar on"));
                } else if ("off".equalsIgnoreCase(args[0])) {
                    setIgnore(player, true);
                    player.sendMessage(format("&bTurned sidebar off"));
                } else {
                    return false;
                }
            } else {
                return false;
            }
            return true;
        } 
    }

    void setIgnore(Player player, boolean value)
    {
        if (value) {
            ignorers.add(player.getUniqueId());
            if (enabled && player.getScoreboard() == scoreboard) {
                player.setScoreboard(getServer().getScoreboardManager().getMainScoreboard());
            }
        } else {
            ignorers.remove(player.getUniqueId());
            if (enabled && player.getScoreboard() == getServer().getScoreboardManager().getMainScoreboard()) {
                player.setScoreboard(scoreboard);
            }
        }
    }
    
    void start()
    {
        configure();
        if (enabled) {
            setupScoreboard();
            setupPlayers();
            getServer().getPluginManager().registerEvents(this, this);
        }
        startTask();
    }

    void stop()
    {
        resetPlayers();
        stopTask();
    }

    void restart()
    {
        stop();
        start();
    }

    void startTask()
    {
        if (task != null) return;
        task = new BukkitRunnable() {
            @Override public void run() {
                onSecondPassed();
            }
        };
        task.runTaskTimer(this, 20, 20);
    }

    void stopTask()
    {
        if (task == null) return;
        try {
            task.cancel();
        } catch (IllegalStateException ise) {
            ise.printStackTrace();
        }
        task = null;
    }

    void onSecondPassed()
    {
        if (enabled) {
            updateTimer();
            if (seconds++ > 10) {
                seconds = 0;
                setupPlayers();
            }
        } else {
            if (seconds++ > 60) {
                seconds = 0;
                start();
            }
        }
    }

    void configure()
    {
        reloadConfig();
        enabled = getConfig().getBoolean("Enabled");
        if (!enabled) return;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        try {
            startTime = df.parse(getConfig().getString("StartTime")).getTime();
            endTime = df.parse(getConfig().getString("EndTime")).getTime();
        } catch (ParseException pe) {
            pe.printStackTrace();
            startTime = 0;
            endTime = 0;
        }
        if (endTime <= System.currentTimeMillis()) {
            enabled = false;
            return;
        }
        titlePrefix = format(getConfig().getString("TitlePrefix"));
        titleSuffix = format(getConfig().getString("TitleSuffix"));
        messages = getConfig().getStringList("Messages");
        for (int i = 0; i < messages.size(); ++i) messages.set(i, format(messages.get(i)));
    }

    void setupScoreboard()
    {
        scoreboard = getServer().getScoreboardManager().getNewScoreboard();
        sidebar = scoreboard.registerNewObjective("countdown", "dummy");
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int i = 0; i < messages.size(); ++i) {
            sidebar.getScore(messages.get(messages.size() - 1 - i)).setScore(i);
        }
        updateTimer();
    }

    void updateTimer()
    {
        long now = System.currentTimeMillis();
        if (now >= endTime) {
            enabled = false;
            resetPlayers();
            return;
        }
        long timeLeft = startTime - System.currentTimeMillis();
        if (timeLeft < 0) {
            timeLeft = 0;
            String title = format("%s&cUnderway&r%s", titlePrefix, titleSuffix);
            sidebar.setDisplayName(title);
        } else {
            long seconds = timeLeft / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            String title = format("%s&f%02d&3:&f%02d&3:&f%02d%s", titlePrefix, hours % 24, minutes % 60, seconds % 60, titleSuffix);
            sidebar.setDisplayName(title);
        }
    }

    void setupPlayers()
    {
        for (Player player : getServer().getOnlinePlayers()) {
            if (!ignorers.contains(player.getUniqueId())) {
                if (player.getScoreboard() == getServer().getScoreboardManager().getMainScoreboard()) {
                    player.setScoreboard(scoreboard);
                }
            }
        }
    }

    void resetPlayers()
    {
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getScoreboard() == scoreboard) {
                player.setScoreboard(getServer().getScoreboardManager().getMainScoreboard());
            }
        }
    }

    String format(String msg, Object... args)
    {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) msg = String.format(msg, args);
        return msg;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        if (!enabled) return;
        if (!ignorers.contains(event.getPlayer().getUniqueId())) {
            event.getPlayer().setScoreboard(scoreboard);
        }
    }
}
