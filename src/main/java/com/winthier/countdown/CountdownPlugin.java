package com.winthier.countdown;

import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class CountdownPlugin extends JavaPlugin implements Listener {
    long startTime;
    long endTime;
    BukkitRunnable task;
    int seconds = 0;
    // Config
    boolean enabled;
    String titlePrefix;
    String titleSuffix;
    List<String> messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("countdown").setExecutor(new CountdownCommand(this));
        start();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        stop();
    }

    void start() {
        configure();
        startTask();
    }

    void stop() {
        stopTask();
    }

    void restart() {
        stop();
        start();
    }

    void startTask() {
        if (task != null) return;
        task = new BukkitRunnable() {
                @Override public void run() {
                    onSecondPassed();
                }
            };
        task.runTaskTimer(this, 20, 20);
    }

    void stopTask() {
        if (task == null) return;
        try {
            task.cancel();
        } catch (IllegalStateException ise) {
            ise.printStackTrace();
        }
        task = null;
    }

    void onSecondPassed() {
        if (enabled) {
            updateTimer();
        }
        if (seconds++ > 10) {
            configure();
            seconds = 0;
        }
    }

    void configure() {
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

    void updateTimer() {
        long now = System.currentTimeMillis();
        if (now >= endTime) {
            enabled = false;
            return;
        }
    }

    static String format(String msg, Object... args) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) msg = String.format(msg, args);
        return msg;
    }

    @EventHandler
    public void onPlayerSidebar(PlayerSidebarEvent event) {
        if (!enabled) return;
        long timeLeft = startTime - System.currentTimeMillis();
        List<String> lines = new ArrayList<>();
        if (timeLeft < 0) {
            timeLeft = 0;
            String title = format("%s&cUnderway&r%s", titlePrefix, titleSuffix);
            lines.add(title);
        } else {
            long secs = timeLeft / 1000;
            long minutes = secs / 60;
            long hours = minutes / 60;
            String title = format("%s&f%02d&3:&f%02d&3:&f%02d%s", titlePrefix,
                                  hours, minutes % 60, secs % 60, titleSuffix);
            lines.add(title);
        }
        lines.addAll(messages);
        event.addLines(this, Priority.DEFAULT, lines);
    }
}
