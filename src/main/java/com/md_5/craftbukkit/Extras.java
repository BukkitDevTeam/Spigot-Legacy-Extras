package com.md_5.craftbukkit;

import java.io.File;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import net.minecraft.server.NetworkListenThread;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Extras extends JavaPlugin implements Listener {

    private String restartScriptLocation;
    private int timeoutTime;
    private boolean restartOnCrash;
    private boolean filterUnsafeIps;
    private String whitelistMessage;

    @Override
    public void onEnable() {
        FileConfiguration conf = getConfig();
        conf.options().copyDefaults(true);
        saveConfig();
        restartScriptLocation = conf.getString("restart-script-location");
        timeoutTime = conf.getInt("timeout-time");
        restartOnCrash = conf.getBoolean("restart-on-crash");
        filterUnsafeIps = conf.getBoolean("filter-unsafe-ips");
        whitelistMessage = conf.getString("whitelist-message");
        //
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        restart();
        return true;
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (filterUnsafeIps) {
            try {
                String ip = event.getAddress().getHostAddress();
                String[] split = ip.split("\\.");
                StringBuilder lookup = new StringBuilder();
                for (int i = split.length - 1; i >= 0; i--) {
                    lookup.append(split[i]);
                    lookup.append(".");
                }
                lookup.append("xbl.spamhaus.org.");
                if (InetAddress.getByName(lookup.toString()) != null) {
                    event.disallow(PlayerPreLoginEvent.Result.KICK_OTHER, "Your IP address is flagged as unsafe by spamhaus.org/xbl");
                }
            } catch (UnknownHostException ex) {
                //
            }
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!event.getPlayer().isWhitelisted()) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, whitelistMessage);
        }
    }

    public void restart() {
        try {
            File file = new File(restartScriptLocation);
            if (file.exists() && !file.isDirectory()) {
                System.out.println("Attempting to restart with " + restartScriptLocation);
                //
                for (Player p : getServer().getOnlinePlayers()) {
                    p.kickPlayer("Server is restarting");
                }
                //
                NetworkListenThread listenThread = ((CraftServer) getServer()).getHandle().server.networkListenThread;
                listenThread.b = false;
                //
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    //
                }
                //
                ((Socket) listenThread.getClass().getDeclaredField("d").get(listenThread)).close();
                //
                try {
                    getServer().shutdown();
                } catch (Throwable t) {
                    //
                }
                //
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("cmd /c start " + file.getPath());
                } else {
                    Runtime.getRuntime().exec(file.getPath());
                }
                System.exit(0);
            } else {
                System.out.println("Startup script '" + restartScriptLocation + "' does not exist!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
