package com.winthier.countdown;

import com.cavetale.core.font.Emoji;
import com.cavetale.core.font.GlyphPolicy;
import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class CountdownPlugin extends JavaPlugin implements Listener {
    protected long startTime;
    protected long endTime;
    protected BukkitRunnable task;
    protected int seconds = 0;
    // Config
    protected boolean enabled;
    protected final List<String> messages = new ArrayList<>();

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

    protected void start() {
        configure();
        startTask();
    }

    protected void stop() {
        stopTask();
    }

    protected void restart() {
        stop();
        start();
    }

    protected void startTask() {
        if (task != null) return;
        task = new BukkitRunnable() {
                @Override public void run() {
                    onSecondPassed();
                }
            };
        task.runTaskTimer(this, 20, 20);
    }

    protected void stopTask() {
        if (task == null) return;
        try {
            task.cancel();
        } catch (IllegalStateException ise) {
            ise.printStackTrace();
        }
        task = null;
    }

    protected void onSecondPassed() {
        if (enabled) {
            updateTimer();
        }
        if (seconds++ > 10) {
            configure();
            seconds = 0;
        }
    }

    protected void configure() {
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
        messages.clear();
        String messagesKey = getConfig().getString("MessagesKey");
        if (messagesKey == null) messagesKey = "Messages";
        for (String line : getConfig().getStringList(messagesKey)) {
            messages.add(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    protected void updateTimer() {
        long now = System.currentTimeMillis();
        if (now >= endTime) {
            enabled = false;
            return;
        }
    }

    @EventHandler
    public void onPlayerSidebar(PlayerSidebarEvent event) {
        if (!enabled) return;
        long timeLeft = startTime - System.currentTimeMillis();
        String timeFormat = timeLeft < 0
            ? "NOW"
            : formatTime(timeLeft);
        List<Component> lines = messages.stream()
            .map(s -> s.replace("{time}", timeFormat))
            .map(s -> Emoji.replaceText(s, GlyphPolicy.HIDDEN, false).asComponent())
            .collect(Collectors.toList());
        event.add(this, Priority.DEFAULT, lines);
    }

    protected String formatTime(long timeLeft) {
        long secs = timeLeft / 1000;
        long minutes = secs / 60;
        long hours = minutes / 60;
        return String.format("%dh %dm %ds", hours, minutes % 60, secs % 60);
    }
}
