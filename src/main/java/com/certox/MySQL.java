package com.certox;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.evilmidget38.UUIDFetcher;
 
 
 
 public class MySQL
 {
   private Connection con;
   
   public MySQL() {}
         
   HashMap<String, Boolean> ipBlacklist = new HashMap<String, Boolean>();
   HashMap<String, Boolean> userBlacklist = new HashMap<String, Boolean>();
   
   boolean lite = false;
   
   public void connect(String dbHost, int dbPort, String database, String dbUser, String dbPassword, boolean offline) throws ClassNotFoundException, SQLException {
     this.lite = offline;
     if (!offline) {
       Class.forName("com.mysql.jdbc.Driver");
       this.con = DriverManager.getConnection("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + database + "?" + "user=" + dbUser + "&" + "password=" + dbPassword);
       return;
     }
     Class.forName("org.sqlite.JDBC");
     this.con = DriverManager.getConnection("jdbc:sqlite:plugins/AntiJoinBot/offline_data.db");
   }
   
   public void initDB() throws SQLException {
     if (!isConnected())
    	 return;
     Statement stm = this.con.createStatement();
     stm.execute("CREATE TABLE IF NOT EXISTS `ajb_blacklist` (IP varchar(15),blocked varchar(5),PRIMARY KEY(IP))");
     stm.execute("CREATE TABLE IF NOT EXISTS `ajb_uuid` (UUID char(36) NOT NULL,blocked varchar(5),PRIMARY KEY(UUID))");
   }
   
   public void initDBOffline() throws SQLException {
	 if (!isConnected())
    	 return;
	 Statement stm = this.con.createStatement();
	 stm.execute("CREATE TABLE IF NOT EXISTS `ajb_blacklist` (IP varchar(15),blocked varchar(5),PRIMARY KEY(IP))");
	 stm.execute("CREATE TABLE IF NOT EXISTS `ajb_user` (name varchar(32),blocked varchar(32),PRIMARY KEY(`name`, `blocked`))");
   }
   
   public void alterUUID() throws SQLException {
	   if (!isConnected())
	    	 return;
	   Statement stm = this.con.createStatement();
	   stm.execute("CREATE TABLE IF NOT EXISTS `ajb_uuid` (UUID char(36) NOT NULL,blocked varchar(5) NOT NULL,PRIMARY KEY(UUID))");
   }
   
   public void migrateUUID() throws Exception, SQLException {
	   if (!isConnected())
	    	 return;
	   Statement stm = this.con.createStatement();
	   Statement rstm = this.con.createStatement();
	   ResultSet rs = stm.executeQuery("SELECT * from `ajb_user`");
	   while (rs.next()) {
		   UUID response = UUIDFetcher.getUUIDOf(rs.getString("name"));
		   if (response != null) {
			   if (this.lite) {
				   rstm.execute("INSERT OR REPLACE INTO `ajb_uuid` (UUID, blocked) VALUES ('" + response + "', '" + rs.getString("blocked") + "')");
			   } else {
				   rstm.execute("REPLACE `ajb_uuid` (UUID, blocked) VALUES ('" + response + "', '" + rs.getString("blocked") + "')");
				   }
		   } else {
			   System.out.println("[AJB] Couldn't find the UUID for " + rs.getString("name") + ". Skipping it");
		   }
	   }
	   stm.execute("DROP TABLE `ajb_user`");
	   rstm.close();
   }
   
   public boolean isConnected() throws SQLException {
     return (this.con != null) && (!this.con.isClosed());
   }
   
   public void closeConnection() throws SQLException {
	   if (!isConnected())
		     return;
	   try {
		   con.close();
	   } catch (SQLException e) {
		   e.printStackTrace();
	   }
   }
   
   public void loadDBtoRAM() throws SQLException {
     if (!isConnected())
    	 return;
     int ipBlacklistCount = 0;
     int userBlacklistCount = 0;
     Statement stm = this.con.createStatement();
     ResultSet rs = stm.executeQuery("SELECT * from `ajb_blacklist`");
     while (rs.next()) {
       this.ipBlacklist.put(rs.getString("IP"), Boolean.valueOf(Boolean.parseBoolean(rs.getString("blocked"))));
       ipBlacklistCount++;
     }
     stm = this.con.createStatement();
     rs = stm.executeQuery("SELECT * from `ajb_uuid`");
     while (rs.next()) {
       this.userBlacklist.put(rs.getString("uuid"), Boolean.valueOf(Boolean.parseBoolean(rs.getString("blocked"))));
       userBlacklistCount++;
     }
     
     System.out.println("[AJB] Loaded: " + ipBlacklistCount + " IP's");
     System.out.println("[AJB] Loaded: " + userBlacklistCount + " User's");
   }
   
   public void loadDBOfflinetoRAM() throws SQLException {
	     if (!isConnected())
	    	 return;
	     int ipBlacklistCount = 0;
	     int userBlacklistCount = 0;
	     Statement stm = this.con.createStatement();
	     ResultSet rs = stm.executeQuery("SELECT * from `ajb_blacklist`");
	     while (rs.next()) {
	       this.ipBlacklist.put(rs.getString("IP"), Boolean.valueOf(Boolean.parseBoolean(rs.getString("blocked"))));
	       ipBlacklistCount++;
	     }
	     stm = this.con.createStatement();
	     rs = stm.executeQuery("SELECT * from `ajb_user`");
	     while (rs.next()) {
	       this.userBlacklist.put(rs.getString("name"), Boolean.valueOf(Boolean.parseBoolean(rs.getString("blocked"))));
	       userBlacklistCount++;
	     }
	     
	     System.out.println("[AJB] Loaded: " + ipBlacklistCount + " IP's");
	     System.out.println("[AJB] Loaded: " + userBlacklistCount + " User's");
	   }
   
   public void startTransaction() throws SQLException {
	   if (!isConnected())
	    	 return;
	   Statement stm = this.con.createStatement();
	   if (this.lite) {
		   stm.execute("BEGIN TRANSACTION");
	   } else
		   stm.execute("START TRANSACTION");
   }
   
   public void setIP(String IP, boolean blocked) throws SQLException {
	 this.ipBlacklist.put(IP, Boolean.valueOf(blocked));
	 if (!isConnected())
    	 return;
     Statement stm = this.con.createStatement();
     if (this.lite) {
       stm.execute("INSERT OR REPLACE INTO `ajb_blacklist` (IP, blocked) VALUES ('" + IP + "', '" + blocked + "')");
     } else
       stm.execute("REPLACE `ajb_blacklist` (IP, blocked) VALUES ('" + IP + "', '" + blocked + "')");
   }
   
   public void setUserName(Player p, boolean blocked) throws SQLException {
	     this.userBlacklist.put(p.getName(), Boolean.valueOf(blocked));
	     if (!isConnected())
	    	 return;
	     Statement stm = this.con.createStatement();
	     if (this.lite) {
	       stm.execute("INSERT OR REPLACE INTO `ajb_user` (name, blocked) VALUES ('" + p.getName() + "', '" + blocked + "')");
	     } else
	       stm.execute("REPLACE `ajb_user` (name, blocked) VALUES ('" + p.getName() + "', '" + blocked + "')");
	   }
   
   public void setUser(Player p, boolean blocked) throws SQLException {
	 this.userBlacklist.put(p.getUniqueId().toString(), Boolean.valueOf(blocked));
	 if (!isConnected())
    	 return;
     Statement stm = this.con.createStatement();
     if (this.lite) {
       stm.execute("INSERT OR REPLACE INTO `ajb_uuid` (UUID, blocked) VALUES ('" + p.getUniqueId() + "', '" + blocked + "')");
     } else
       stm.execute("REPLACE `ajb_uuid` (UUID, blocked) VALUES ('" + p.getUniqueId() + "', '" + blocked + "')");
   }
   
   public void setUserName(String user, boolean blocked) throws SQLException {
	     this.userBlacklist.put(user, Boolean.valueOf(blocked));
	     if (!isConnected())
	    	 return;
	     Statement stm = this.con.createStatement();
	     if (this.lite) {
	       stm.execute("INSERT OR REPLACE INTO `ajb_user` (name, blocked) VALUES ('" + user + "', '" + blocked + "')");
	     } else
	       stm.execute("REPLACE `ajb_user` (name, blocked) VALUES ('" + user + "', '" + blocked + "')");
	   }
   
   public void setUser(UUID user, boolean blocked) throws SQLException {
	 this.userBlacklist.put(user.toString(), Boolean.valueOf(blocked));
	 if (!isConnected())
    	 return;
     Statement stm = this.con.createStatement();
     if (this.lite) {
       stm.execute("INSERT OR REPLACE INTO `ajb_uuid` (UUID, blocked) VALUES ('" + user + "', '" + blocked + "')");
     } else
       stm.execute("REPLACE `ajb_uuid` (UUID, blocked) VALUES ('" + user + "', '" + blocked + "')");
   }
   
   public void commit() throws SQLException {
	   if (!isConnected())
	    	 return;
	   Statement stm = this.con.createStatement();
	   stm.execute("COMMIT");
   }
 }