package com.certox;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class Core extends JavaPlugin implements org.bukkit.event.Listener
{
  public MySQL db = new MySQL();
  Core plugin = this;
  
  Boolean forceMode = Boolean.valueOf(false);
  String forceModeMsg = "";
  
  Boolean serverListPing = Boolean.valueOf(false);
  String serverListPingMsg = "";
  
  String kickMsg = "";
  
  boolean enabled = true;
  boolean debug = false;
  
  private Map<String, Object> activeBlacklist = new HashMap();
  
  public Core() {}
  
  public void onLoad()
  {
    getConfig().addDefault("AntiJoinBot.MySQL.Offline", Boolean.valueOf(true));
    getConfig().addDefault("AntiJoinBot.MySQL.Host", "localhost");
    getConfig().addDefault("AntiJoinBot.MySQL.Port", Integer.valueOf(3306));
    getConfig().addDefault("AntiJoinBot.MySQL.Database", "ajb_intelligent_blacklist");
    getConfig().addDefault("AntiJoinBot.MySQL.User", "<username>");
    getConfig().addDefault("AntiJoinBot.MySQL.Password", "<password>");
    

    this.activeBlacklist.put("http://www,shroomery,org/ythan/proxycheck,php?ip=", "Y");
    this.activeBlacklist.put("http://www,stopforumspam,com/api?ip=", "yes");
    this.activeBlacklist.put("http://yasb,intuxication,org/api/check,xml?ip=", "<spam>true</spam>");
    
    getConfig().addDefault("AntiJoinBot.Blacklists", this.activeBlacklist);
    

    getConfig().addDefault("AntiJoinBot.ServerListPing.Enabled", Boolean.valueOf(false));
    getConfig().addDefault("AntiJoinBot.ServerListPing.Message", "Don't use a Proxy :P");
    
    getConfig().addDefault("AntiJoinBot.Force.Enabled", Boolean.valueOf(false));
    getConfig().addDefault("AntiJoinBot.Force.Message", "Proxy Check... pls. reJoin!");
    
    getConfig().addDefault("AntiJoinBot.Kick.Message", "Proxy is Detected. (maybe an error please reconnect your Router)");
    
    getConfig().addDefault("AntiJoinBot.Warmup.Enabled", Boolean.valueOf(true));
    getConfig().addDefault("AntiJoinBot.Warmup.Seconds", Integer.valueOf(60));
    
    getConfig().addDefault("AntiJoinBot.Debug.Enabled", Boolean.valueOf(false));
    
    getConfig().options().copyDefaults(true);
    saveConfig();
    reloadConfig();
    
    this.debug = getConfig().getBoolean("AntiJoinBot.Debug.Enabled");
    
    this.serverListPing = Boolean.valueOf(getConfig().getBoolean("AntiJoinBot.ServerListPing.Enabled"));
    this.serverListPingMsg = getConfig().getString("AntiJoinBot.ServerListPing.Message");
    
    this.forceMode = Boolean.valueOf(getConfig().getBoolean("AntiJoinBot.Force.Enabled"));
    this.forceModeMsg = getConfig().getString("AntiJoinBot.Force.Message");
    
    this.kickMsg = getConfig().getString("AntiJoinBot.Kick.Message");
    
    this.activeBlacklist = getConfig().getConfigurationSection("AntiJoinBot.Blacklists").getValues(false);
  }
  

  public void onEnable()
  {
    if (getConfig().getBoolean("AntiJoinBot.Warmup.Enabled")) {
      System.out.println("AntiJoinBot enabled in " + getConfig().getInt("AntiJoinBot.Warmup.Seconds") + " seconds");
      getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
        public void run() {
          System.out.println("AntiJoinBot is enabled");
          Bukkit.getPluginManager().registerEvents(Core.this.plugin, Core.this.plugin); } }, getConfig().getInt("AntiJoinBot.Warmup.Seconds") * 20);

    }
    else
    {

      Bukkit.getPluginManager().registerEvents(this.plugin, this.plugin);
    }
    try {
      this.db.connect(getConfig().getString("AntiJoinBot.MySQL.Host"), getConfig().getInt("AntiJoinBot.MySQL.Port"), getConfig().getString("AntiJoinBot.MySQL.Database"), getConfig().getString("AntiJoinBot.MySQL.User"), getConfig().getString("AntiJoinBot.MySQL.Password"), getConfig().getBoolean("AntiJoinBot.MySQL.Offline"));
      





      this.db.initDB();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (SQLException e) {
      System.out.println(">>>>>>>>>>>> MySQL DRIVER NOT FOUND <<<<<<<<<<<<");
      e.printStackTrace();
    }
    try {
      this.db.loadDBtoRAM();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
  
  public Boolean isProxy(String IP) {
    if ((IP.equals("127.0.0.1")) || (IP.equals("localhost")))
      return Boolean.valueOf(false);
    for (String s : this.activeBlacklist.keySet()) {
      try {
        String res = "";
        Scanner scanner = new Scanner(new URL(s.replace(",", ".") + IP).openStream());
        while (scanner.hasNextLine()) {
          res = res + scanner.nextLine();
        }
        String[] args = ((String)this.activeBlacklist.get(s)).split(",");
        for (String arg : args) {
          if (res.contains(arg)) {
            debug(s.replace(",", ".") + ": (" + IP + " --> true)");
            return Boolean.valueOf(true);
          }
          
          debug(s.replace(",", ".") + ": (" + IP + " --> false)");
        }
      } catch (Exception e) { if (this.debug) {
          e.printStackTrace();
        }
      }
    }
    return Boolean.valueOf(false);
  }
  
  @EventHandler(priority=EventPriority.HIGHEST)
  public void onPlayerJoinEvent(PlayerJoinEvent e) throws Exception {
    if (!this.enabled) {
      return;
    }
    if (e.getPlayer().hasPermission("ajb.bypass")) {
      this.db.setUser(e.getPlayer(), false);
      return;
    }
  }
  
  @EventHandler(priority=EventPriority.HIGHEST)
  public void onServerListPingEvent(ServerListPingEvent e) throws Exception {
    if (!this.serverListPing.booleanValue())
      return;
    if (this.db.ipBlacklist.containsKey(e.getAddress().getHostAddress().toString())) {
      debug("[M] ipBlacklist: " + e.getAddress().getHostAddress().toString() + " --> " + this.db.ipBlacklist.get(e.getAddress().getHostAddress().toString()));
      if (((Boolean)this.db.ipBlacklist.get(e.getAddress().getHostAddress().toString())).booleanValue()) {
        e.setMotd(this.serverListPingMsg);
      }
      return;
    }
    final String IP = e.getAddress().getHostAddress().toString();
    getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
      public void run() {
        try {
          Core.this.plugin.db.setIP(IP, Core.this.isProxy(IP).booleanValue());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
  
  @EventHandler(priority=EventPriority.HIGHEST)
  public void onPlayerLoginEvent(PlayerLoginEvent e) throws Exception
  {
    if (!this.enabled)
      return;
    debug("[M] JOIN: " + e.getAddress().getHostAddress().toString() + " --> " + e.getPlayer().getName());
    if (this.db.userBlacklist.containsKey(e.getPlayer().getName())) {
      debug("[M] userBlacklist: " + e.getPlayer().getName() + " --> " + this.db.userBlacklist.get(e.getPlayer().getName()));
      if (((Boolean)this.db.userBlacklist.get(e.getPlayer().getName())).booleanValue()) {
        e.setKickMessage(this.kickMsg);
        e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
      }
      return;
    }
    if (this.db.ipBlacklist.containsKey(e.getAddress().getHostAddress().toString())) {
      debug("[M] ipBlacklist: " + e.getAddress().getHostAddress().toString() + " --> " + this.db.ipBlacklist.get(e.getAddress().getHostAddress().toString()));
      if (((Boolean)this.db.ipBlacklist.get(e.getAddress().getHostAddress().toString())).booleanValue()) {
        e.setKickMessage(this.kickMsg);
        e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
      }
      return;
    }
    

    if (e.getPlayer().hasPermission("ajb.bypass")) {
      this.db.setUser(e.getPlayer(), false);
      return;
    }
    
    if (this.forceMode.booleanValue()) {
      debug("[M] FORCE: " + e.getAddress().getHostAddress().toString() + " --> " + e.getPlayer().getName());
      final String IP = e.getAddress().getHostAddress().toString();
      getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
        public void run() {
          try {
            Core.this.plugin.db.setIP(IP, Core.this.isProxy(IP).booleanValue());
          } catch (Exception e) {
            e.printStackTrace();
          }
          
        }
      });
      e.setKickMessage(this.forceModeMsg);
      e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
      return;
    }
    if (isProxy(e.getAddress().getHostAddress().toString()).booleanValue()) {
      this.db.setIP(e.getAddress().getHostAddress().toString(), true);
      e.setKickMessage(this.kickMsg);
      e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
    }
    else {
      this.db.setIP(e.getAddress().getHostAddress().toString(), false);
    }
  }
  
  @EventHandler(priority=EventPriority.HIGHEST)
  public void onPlayerLoginEvent(AsyncPlayerPreLoginEvent e) throws Exception { if ((!this.enabled) || (this.forceMode.booleanValue())) {
      return;
    }
    debug("[A] JOIN: " + e.getAddress().getHostAddress().toString() + " --> " + e.getName());
    if (this.db.userBlacklist.containsKey(e.getName())) {
      debug("[A] userBlacklist: " + e.getName() + " --> " + this.db.userBlacklist.get(e.getName()));
      if (((Boolean)this.db.userBlacklist.get(e.getName())).booleanValue()) {
        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, this.kickMsg);
      }
      return;
    }
    if (this.db.ipBlacklist.containsKey(e.getAddress().getHostAddress().toString())) {
      debug("[A] ipBlacklist: " + e.getAddress().getHostAddress().toString() + " --> " + this.db.ipBlacklist.get(e.getAddress().getHostAddress().toString()));
      if (((Boolean)this.db.ipBlacklist.get(e.getAddress().getHostAddress().toString())).booleanValue()) {
        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, this.kickMsg);
      }
      return;
    }
    
    if (isProxy(e.getAddress().getHostAddress().toString()).booleanValue()) {
      this.db.setIP(e.getAddress().getHostAddress().toString(), true);
      e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, this.kickMsg);
    }
    else {
      this.db.setIP(e.getAddress().getHostAddress().toString(), false);
    }
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
    if ((sender.hasPermission("ajb.toggle")) && 
      (cmd.getName().equals("ajb")) && (args.length == 0)) {
      if (this.enabled) {
        this.enabled = false;
      } else {
        this.enabled = true;
      }
      sender.sendMessage("AntiJoinBot Enabled: " + this.enabled);
    }
    
    if (sender.hasPermission("ajb.add")) {
      try {
        if ((this.db.isConnected()) && 
          (cmd.getName().equals("ajb")) && (args.length == 2)) {
          switch (args[0]) {
          case "add": 
            this.db.setUser(args[1], false);
            sender.sendMessage("AntiJoinBot Player add to whitelist: " + args[1]);
            break;
          case "block": 
            sender.sendMessage("AntiJoinBot Player add to blacklist: " + args[1]);
            this.db.setUser(args[1], true);
          }
        }
      } catch (SQLException e) {}
    }
    return false;
  }
  
  public void debug(String text) {
    if (this.debug) {
      System.out.println("AJB: [D] " + text);
    }
  }
}