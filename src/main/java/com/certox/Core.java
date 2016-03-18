package com.certox;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import org.mcstats.Metrics;

import com.evilmidget38.UUIDFetcher;
 
 public class Core extends JavaPlugin implements org.bukkit.event.Listener
 {
   public MySQL db = new MySQL();
   Core plugin = this;
   
   Boolean forceMode = Boolean.valueOf(false);
   String forceModeMsg = "";
   
   Boolean DownloadBlacklist = Boolean.valueOf(true);
   
   Boolean serverListPing = Boolean.valueOf(false);
   String serverListPingMsg = "";
   
   Boolean offlineMode = Boolean.valueOf(true);
   
   String kickMsg = "";
   
   boolean enabled = true;
   boolean debug = false;
   
   private Map<String, Object> activeBlacklist = new HashMap<String, Object>();
   
   public Core() {}
   
   public void onLoad()
   {
     getConfig().addDefault("AntiJoinBot.MySQL.Offline", Boolean.valueOf(true));
     getConfig().addDefault("AntiJoinBot.MySQL.Host", "localhost");
     getConfig().addDefault("AntiJoinBot.MySQL.Port", Integer.valueOf(3306));
     getConfig().addDefault("AntiJoinBot.MySQL.Database", "ajb_intelligent_blacklist");
     getConfig().addDefault("AntiJoinBot.MySQL.User", "<username>");
     getConfig().addDefault("AntiJoinBot.MySQL.Password", "<password>");
 
     this.activeBlacklist.put("http://www,stopforumspam,com/api?ip=", "yes");
     this.activeBlacklist.put("http://www,shroomery,org/ythan/proxycheck,php?ip=", "Y");
     
     getConfig().addDefault("AntiJoinBot.Blacklists", this.activeBlacklist);
     getConfig().addDefault("AntiJoinBot.DownloadBlacklist.Enabled", Boolean.valueOf(true));
 
     getConfig().addDefault("AntiJoinBot.ServerListPing.Enabled", Boolean.valueOf(false));
     getConfig().addDefault("AntiJoinBot.ServerListPing.Message", "Don't use a Proxy :P");
     
     getConfig().addDefault("AntiJoinBot.Force.Enabled", Boolean.valueOf(false));
     getConfig().addDefault("AntiJoinBot.Force.Message", "Proxy Check... pls. reJoin!");
     
     getConfig().addDefault("AntiJoinBot.Kick.Message", "Proxy is Detected. (maybe an error please reconnect your Router)");
     
     getConfig().addDefault("AntiJoinBot.Warmup.Enabled", Boolean.valueOf(true));
     getConfig().addDefault("AntiJoinBot.Warmup.Seconds", Integer.valueOf(60));
     
     getConfig().addDefault("AntiJoinBot.OfflineMode.Enabled", Boolean.valueOf(true));
     getConfig().addDefault("AntiJoinBot.OfflineMode.MigrateUUID", Boolean.valueOf(false));
     
     getConfig().addDefault("AntiJoinBot.Debug.Enabled", Boolean.valueOf(false));
     
     getConfig().options().copyDefaults(true);
     saveConfig();
     reloadConfig();
     
     this.debug = getConfig().getBoolean("AntiJoinBot.Debug.Enabled");
     
     this.offlineMode = Boolean.valueOf(getConfig().getBoolean("AntiJoinBot.OfflineMode.Enabled"));
     
     this.serverListPing = Boolean.valueOf(getConfig().getBoolean("AntiJoinBot.ServerListPing.Enabled"));
     this.serverListPingMsg = getConfig().getString("AntiJoinBot.ServerListPing.Message");
     
     this.forceMode = Boolean.valueOf(getConfig().getBoolean("AntiJoinBot.Force.Enabled"));
     this.forceModeMsg = getConfig().getString("AntiJoinBot.Force.Message");
     
     this.kickMsg = getConfig().getString("AntiJoinBot.Kick.Message");
     
     this.activeBlacklist = getConfig().getConfigurationSection("AntiJoinBot.Blacklists").getValues(false);
     this.DownloadBlacklist = Boolean.valueOf(getConfig().getBoolean("AntiJoinBot.DownloadBlacklist.Enabled"));
   }
   
 
   public void onEnable()
   {
	     try {
	         this.db.connect(getConfig().getString("AntiJoinBot.MySQL.Host"), getConfig().getInt("AntiJoinBot.MySQL.Port"), getConfig().getString("AntiJoinBot.MySQL.Database"), getConfig().getString("AntiJoinBot.MySQL.User"), getConfig().getString("AntiJoinBot.MySQL.Password"), getConfig().getBoolean("AntiJoinBot.MySQL.Offline"));
	         if (this.offlineMode.booleanValue()) {
	         this.db.initDBOffline();
	         } else {
	        	 this.db.initDB();
	         }
	       } catch (ClassNotFoundException e) {
		     System.out.println(">>>>>>>>>>>> MySQL DRIVER NOT FOUND <<<<<<<<<<<<");
	         e.printStackTrace();
	       } catch (SQLException e) {
	         e.printStackTrace();
	       }
   
	 if (getConfig().getBoolean("AntiJoinBot.OfflineMode.MigrateUUID") && !this.offlineMode.booleanValue()) {
		 try {
		 this.db.alterUUID();
	 } catch (Exception e){ if (this.debug) {
		 debug("[MG] Error while changing database structure:");
		 e.printStackTrace();
	 		}
	 	}
		 try {
		this.db.migrateUUID();
		this.db.closeConnection();
		this.db.connect(getConfig().getString("AntiJoinBot.MySQL.Host"), getConfig().getInt("AntiJoinBot.MySQL.Port"), getConfig().getString("AntiJoinBot.MySQL.Database"), getConfig().getString("AntiJoinBot.MySQL.User"), getConfig().getString("AntiJoinBot.MySQL.Password"), getConfig().getBoolean("AntiJoinBot.MySQL.Offline"));
        this.db.initDB();
		} catch (Exception e){ if (this.debug) {
			debug("[MG] Error while migrating database:");
			e.printStackTrace();
		} else {
			System.out.println("[AJB] Failed migrating database. Activate Debug for more informations");
		 	}
		}
		System.out.println("[AJB] Database migrated successfully.");
		this.getConfig().set("AntiJoinBot.OfflineMode.MigrateUUID", false);
		this.saveConfig();
		 }
	 
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
    	 if (this.offlineMode.booleanValue()) {
       this.db.loadDBOfflinetoRAM();
    	 } else {
    		 this.db.loadDBtoRAM();
    	 }
     } catch (SQLException e) {
       e.printStackTrace();
     }
     
	 if (this.DownloadBlacklist.booleanValue()) {
	       try {
	           Scanner Blacklist = new Scanner(new URL("http://myip.ms/files/blacklist/csf/latest_blacklist.txt").openStream());
	           System.out.println("[AJB] Downloading Blacklist...");
	           this.db.startTransaction();
	           while (Blacklist.hasNextLine()) {
	            	 String IP = Blacklist.nextLine();
		           	if (IP.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}") && !this.db.ipBlacklist.containsKey(IP)) {
		           		try {
		           			this.db.setIP(IP, true);
		           		} catch (Exception e) {
		           			debug("[DL] SQL Error");
		           			e.printStackTrace();
		           		}
		           	}
	           }
	           this.db.commit();
	           Blacklist.close();
	           System.out.println("[AJB] Blacklist successfully Downloaded");
	         } catch (Exception e) { if (this.debug) {
	        	 debug("[DL] Error Downloading the Blacklist:");
	             e.printStackTrace();
	           } else {
	        	   System.out.println("[AJB] Error Downloading the Blacklist");
	         }
	         
	       }
	     
	 }
     
     try {
         Metrics metrics = new Metrics(this);
         metrics.start();
     } catch (IOException e) {}
   }
   
   public Boolean isProxy(String IP) {
     if ((IP.equals("127.0.0.1")) || (IP.equals("localhost")) || (IP.matches("192\\.168\\.[01]{1}\\.[0-9]{1,3}")))
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
           if (res.matches(arg)) {
             debug(s.replace(",", ".") + ": (" + IP + " --> true)");
             return Boolean.valueOf(true);
           }
           
           debug(s.replace(",", ".") + ": (" + IP + " --> false)");
           scanner.close();
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
	 if (!this.db.isConnected())
		 this.db.connect(getConfig().getString("AntiJoinBot.MySQL.Host"), getConfig().getInt("AntiJoinBot.MySQL.Port"), getConfig().getString("AntiJoinBot.MySQL.Database"), getConfig().getString("AntiJoinBot.MySQL.User"), getConfig().getString("AntiJoinBot.MySQL.Password"), getConfig().getBoolean("AntiJoinBot.MySQL.Offline"));
     if (e.getPlayer().hasPermission("ajb.bypass")) {
    	 if (this.offlineMode.booleanValue()) {
    	 if (!this.db.userBlacklist.containsKey(e.getPlayer().getName()))
         this.db.setUserName(e.getPlayer(), false);
    	 } else {
    		 if (!this.db.userBlacklist.containsKey(e.getPlayer().getUniqueId().toString()))
    		 this.db.setUser(e.getPlayer(), false);
    	 }
       return;
     }
   }
   
   @EventHandler(priority=EventPriority.HIGHEST)
   public void onServerListPingEvent(ServerListPingEvent e) throws Exception {
     if (!this.serverListPing.booleanValue())
       return;
	 if (!this.db.isConnected())
		 this.db.connect(getConfig().getString("AntiJoinBot.MySQL.Host"), getConfig().getInt("AntiJoinBot.MySQL.Port"), getConfig().getString("AntiJoinBot.MySQL.Database"), getConfig().getString("AntiJoinBot.MySQL.User"), getConfig().getString("AntiJoinBot.MySQL.Password"), getConfig().getBoolean("AntiJoinBot.MySQL.Offline"));
     if (this.db.ipBlacklist.containsKey(e.getAddress().getHostAddress())) {
       debug("[M] ipBlacklist: " + e.getAddress().getHostAddress() + " --> " + this.db.ipBlacklist.get(e.getAddress().getHostAddress()));
       if (((Boolean)this.db.ipBlacklist.get(e.getAddress().getHostAddress())).booleanValue()) {
         e.setMotd(this.serverListPingMsg);
       }
       return;
     }
     final String IP = e.getAddress().getHostAddress();
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
	 if (!this.db.isConnected())
		 this.db.connect(getConfig().getString("AntiJoinBot.MySQL.Host"), getConfig().getInt("AntiJoinBot.MySQL.Port"), getConfig().getString("AntiJoinBot.MySQL.Database"), getConfig().getString("AntiJoinBot.MySQL.User"), getConfig().getString("AntiJoinBot.MySQL.Password"), getConfig().getBoolean("AntiJoinBot.MySQL.Offline"));
     debug("[M] JOIN: " + e.getAddress().getHostAddress() + " --> " + e.getPlayer().getName());
     if (this.offlineMode.booleanValue()) {
         if (this.db.userBlacklist.containsKey(e.getPlayer().getName())) {
             debug("[M] userBlacklist: " + e.getPlayer().getName() + " --> " + this.db.userBlacklist.get(e.getPlayer().getName()));
             if (((Boolean)this.db.userBlacklist.get(e.getPlayer().getName())).booleanValue()) {
               e.disallow(PlayerLoginEvent.Result.KICK_OTHER, this.kickMsg);
             }
             return;
         }
     } else {
     if (this.db.userBlacklist.containsKey(e.getPlayer().getUniqueId().toString())) {
       debug("[M] userBlacklist: " + e.getPlayer().getName() + " --> " + this.db.userBlacklist.get(e.getPlayer().getUniqueId().toString()));
       if (((Boolean)this.db.userBlacklist.get(e.getPlayer().getUniqueId().toString())).booleanValue()) {
         e.disallow(PlayerLoginEvent.Result.KICK_OTHER, this.kickMsg);
         }
       return;
       }
     }
     if (this.db.ipBlacklist.containsKey(e.getAddress().getHostAddress())) {
       debug("[M] ipBlacklist: " + e.getAddress().getHostAddress() + " --> " + this.db.ipBlacklist.get(e.getAddress().getHostAddress()));
       if (((Boolean)this.db.ipBlacklist.get(e.getAddress().getHostAddress())).booleanValue()) {
         e.disallow(PlayerLoginEvent.Result.KICK_OTHER, this.kickMsg);
       }
       return;
     }
     
     if (this.forceMode.booleanValue()) {
       debug("[M] FORCE: " + e.getAddress().getHostAddress() + " --> " + e.getPlayer().getName());
       final String IP = e.getAddress().getHostAddress();
       getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
         public void run() {
           try {
             Core.this.plugin.db.setIP(IP, Core.this.isProxy(IP).booleanValue());
           } catch (Exception e) {
             e.printStackTrace();
           }
           
         }
       });
       e.disallow(PlayerLoginEvent.Result.KICK_OTHER, this.forceModeMsg);
       return;
     }
     if (isProxy(e.getAddress().getHostAddress()).booleanValue()) {
       this.db.setIP(e.getAddress().getHostAddress(), true);
       e.disallow(PlayerLoginEvent.Result.KICK_OTHER, this.kickMsg);
     }
     else {
       this.db.setIP(e.getAddress().getHostAddress(), false);
     }
   }
   
   @EventHandler(priority=EventPriority.HIGHEST)
   public void onPlayerLoginEvent(AsyncPlayerPreLoginEvent e) throws Exception {
	   if ((!this.enabled) || (this.forceMode.booleanValue())) {
		   return;
	   }
  	 if (!this.db.isConnected())
		 this.db.connect(getConfig().getString("AntiJoinBot.MySQL.Host"), getConfig().getInt("AntiJoinBot.MySQL.Port"), getConfig().getString("AntiJoinBot.MySQL.Database"), getConfig().getString("AntiJoinBot.MySQL.User"), getConfig().getString("AntiJoinBot.MySQL.Password"), getConfig().getBoolean("AntiJoinBot.MySQL.Offline"));
     debug("[A] JOIN: " + e.getAddress().getHostAddress() + " --> " + e.getName());
     if (this.offlineMode.booleanValue()) {
     if (this.db.userBlacklist.containsKey(e.getName())) {
       debug("[A] userBlacklist: " + e.getName() + " --> " + this.db.userBlacklist.get(e.getName()));
       if (((Boolean)this.db.userBlacklist.get(e.getName())).booleanValue()) {
         e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, this.kickMsg);
           }
           return;
         }
       } else {
    	     if (this.db.userBlacklist.containsKey(e.getUniqueId().toString())) {
    	         debug("[A] userBlacklist: " + e.getName() + " --> " + this.db.userBlacklist.get(e.getUniqueId().toString()));
    	         if (((Boolean)this.db.userBlacklist.get(e.getUniqueId().toString())).booleanValue()) {
    	           e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, this.kickMsg);
         }
       return;
       }
     }
     if (this.db.ipBlacklist.containsKey(e.getAddress().getHostAddress())) {
       debug("[A] ipBlacklist: " + e.getAddress().getHostAddress() + " --> " + this.db.ipBlacklist.get(e.getAddress().getHostAddress()));
       if (((Boolean)this.db.ipBlacklist.get(e.getAddress().getHostAddress())).booleanValue()) {
         e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, this.kickMsg);
       }
       return;
     }
     
     if (isProxy(e.getAddress().getHostAddress()).booleanValue()) {
       this.db.setIP(e.getAddress().getHostAddress(), true);
       e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, this.kickMsg);
     }
     else {
       this.db.setIP(e.getAddress().getHostAddress(), false);
     }
   }
   
   public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
     if ((sender.hasPermission("ajb.reload") || sender.hasPermission("ajb.toggle") || sender.hasPermission("ajb.add")) && cmd.getName().equalsIgnoreCase("ajb") && (args.length == 0)) {
       sender.sendMessage("[AJB] §cValid commands: §rhelp, toggle, add, block, reload");
     }
     
     if ((cmd.getName().equalsIgnoreCase("ajb")) && (args.length == 1)) {
        	 switch (args[0]) {
             case "reload":
            	 if (sender.hasPermission("ajb.reload")) {
    	            	 this.reloadConfig();
						 try {
						 if(this.db.isConnected())
							this.db.closeConnection();
						 this.db.connect(getConfig().getString("AntiJoinBot.MySQL.Host"), getConfig().getInt("AntiJoinBot.MySQL.Port"), getConfig().getString("AntiJoinBot.MySQL.Database"), getConfig().getString("AntiJoinBot.MySQL.User"), getConfig().getString("AntiJoinBot.MySQL.Password"), getConfig().getBoolean("AntiJoinBot.MySQL.Offline"));
						 if (!this.db.userBlacklist.isEmpty())
						 	this.db.userBlacklist.clear();
						 if (!this.db.ipBlacklist.isEmpty())
						 	this.db.ipBlacklist.clear();
						 if (this.offlineMode.booleanValue()) {
							this.db.initDBOffline();
							this.db.loadDBOfflinetoRAM();
						 } else {
							this.db.initDB();
							this.db.loadDBtoRAM();
						 }
							sender.sendMessage("[AJB] §cConfig & database reloaded successfully");
						 } catch (Exception e) {
							sender.sendMessage("[AJB] §cConfig reloaded but failed to reload the DB Connection.");
							if (this.debug) {
								e.printStackTrace();
							}
						 }
            	 }
            	 break;
             case "toggle":
            	 if ((sender.hasPermission("ajb.toggle"))) {
            	       if (this.enabled) {
            	         this.enabled = false;
            	       } else {
            	         this.enabled = true;
            	       }
            	       sender.sendMessage("[AJB] §cPlugin Enabled: §r" + this.enabled);
            	     }
                 	 break;
             case "help":
            	 if (sender.hasPermission("ajb.reload") || sender.hasPermission("ajb.toggle") || sender.hasPermission("ajb.add")) {
            		 sender.sendMessage("[AJB] §cValid Commands:");
            		 sender.sendMessage("/ajb toggle - §cActivates/Deactivates the plugin");
            		 sender.sendMessage("/ajb reload - §cReloads configuration and DB connection");
            		 sender.sendMessage("/ajb add <player> - §cAdds the given player to the AJB whitelist");
            		 sender.sendMessage("/ajb block <player> - §cAdds the given player to the AJB blacklist");
            	 }
                 break;
             case "add":
            	 if (sender.hasPermission("ajb.reload") || sender.hasPermission("ajb.toggle") || sender.hasPermission("ajb.add")) {
            	 sender.sendMessage("[AJB] §cUsage: §r/ajb add <player>");
            	 }
                 break;
             case "block":
            	 if (sender.hasPermission("ajb.reload") || sender.hasPermission("ajb.toggle") || sender.hasPermission("ajb.add")) {
            	 sender.sendMessage("[AJB] §cUsage: §r/ajb block <player>");
            	 }
                 break;
             }
     }  
   
     if (sender.hasPermission("ajb.add")) {
       try {
         if ((cmd.getName().equalsIgnoreCase("ajb")) && (args.length == 2)) {
        	 if (!this.db.isConnected())
        		 this.db.connect(getConfig().getString("AntiJoinBot.MySQL.Host"), getConfig().getInt("AntiJoinBot.MySQL.Port"), getConfig().getString("AntiJoinBot.MySQL.Database"), getConfig().getString("AntiJoinBot.MySQL.User"), getConfig().getString("AntiJoinBot.MySQL.Password"), getConfig().getBoolean("AntiJoinBot.MySQL.Offline"));
           switch (args[0]) {
           case "add":
        	   if (this.offlineMode.booleanValue()) {
        		   this.db.setUserName(args[1], false);
                   sender.sendMessage("[AJB] §cPlayer added to whitelist: §r" + args[1]);
        	   } else {
        		   if (UUIDFetcher.getUUIDOf(args[1]) == null) {
        			   sender.sendMessage("[AJB] §cCannot get UUID of §r" + args[1] + "§c. Check the nickname!");
        		   } else {
        		   this.db.setUser(UUIDFetcher.getUUIDOf(args[1]), false);
                   sender.sendMessage("[AJB] §cPlayer added to whitelist: §r" + args[1]);
        		   }
        	   }
             break;
           case "block":
        	   if (this.offlineMode.booleanValue()) {
        		   this.db.setUserName(args[1], true);
                   sender.sendMessage("[AJB] §cPlayer added to blacklist: §r" + args[1]);
        	   } else {
        		   if (UUIDFetcher.getUUIDOf(args[1]) == null) {
        			   sender.sendMessage("[AJB] §cCannot get UUID of §r" + args[1] + "§c. Check the nickname!");
        		   } else {
        		   this.db.setUser(UUIDFetcher.getUUIDOf(args[1]), true);
                   sender.sendMessage("[AJB] §cPlayer added to blacklist: §r" + args[1]);
        		   }
        	   }
             break;
           }
         }
       } catch (Exception e) {
       	sender.sendMessage("[AJB] §cError adding player to the database");
    	   if (this.debug) {
		e.printStackTrace();
		}
       }
     }
     return false;
   }
   
   public void debug(String text) {
     if (this.debug) {
       System.out.println("[AJB]: [D] " + text);
     }
   }
   
   public void onDisable() {
	   try {
	   if (this.db.isConnected()) {
		   this.db.closeConnection();
		   }
	   } catch (SQLException e) {}
	   Bukkit.getPluginManager().disablePlugin(this.plugin);
   }
 }
