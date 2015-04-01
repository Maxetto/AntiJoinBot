package com.certox;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import org.bukkit.entity.Player;



public class MySQL
{
  private Connection con;
  private static final String DB_PATH = "offline_data.db";
  
  public MySQL() {}
  
  HashMap<String, Boolean> ipBlacklist = new HashMap();
  HashMap<String, Boolean> userBlacklist = new HashMap();
  
  boolean lite = false;
  
  public void connect(String dbHost, int dbPort, String database, String dbUser, String dbPassword, boolean offline) throws ClassNotFoundException, SQLException {
    this.lite = offline;
    if (!offline) {
      Class.forName("com.mysql.jdbc.Driver");
      this.con = DriverManager.getConnection("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + database + "?" + "user=" + dbUser + "&" + "password=" + dbPassword);
      

      return;
    }
    Class.forName("org.sqlite.JDBC");
    this.con = DriverManager.getConnection("jdbc:sqlite:offline_data.db");
  }
  
  public void initDB() throws SQLException {
    if (!isConnected())
      return;
    Statement stm = this.con.createStatement();
    stm.execute("CREATE TABLE IF NOT EXISTS `ajb_blacklist` (IP varchar(32),blocked varchar(32),PRIMARY KEY(`IP`, `blocked`))");
    stm.execute("CREATE TABLE IF NOT EXISTS `ajb_user` (name varchar(32),blocked varchar(32),PRIMARY KEY(`name`, `blocked`))");
  }
  
  public boolean isConnected() throws SQLException {
    return (this.con != null) && (!this.con.isClosed());
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
    rs = stm.executeQuery("SELECT * from `ajb_user`");
    while (rs.next()) {
      this.userBlacklist.put(rs.getString("name"), Boolean.valueOf(Boolean.parseBoolean(rs.getString("blocked"))));
      userBlacklistCount++;
    }
    
    System.out.println("AJB: Loaded: " + ipBlacklistCount + " IP's");
    System.out.println("AJB: Loaded: " + userBlacklistCount + " User's");
  }
  
  public void setIP(String IP, boolean blocked) throws SQLException {
    this.ipBlacklist.put(IP, Boolean.valueOf(blocked));
    Statement stm = this.con.createStatement();
    if (this.lite) {
      stm.execute("INSERT OR REPLACE INTO `ajb_blacklist` (IP, blocked) VALUES ('" + IP + "', '" + blocked + "')");
    } else
      stm.execute("REPLACE `ajb_blacklist` (IP, blocked) VALUES ('" + IP + "', '" + blocked + "')");
  }
  
  public void setUser(Player p, boolean blocked) throws SQLException {
    this.userBlacklist.put(p.getName(), Boolean.valueOf(blocked));
    Statement stm = this.con.createStatement();
    if (this.lite) {
      stm.execute("INSERT OR REPLACE INTO `ajb_user` (name, blocked) VALUES ('" + p.getName() + "', '" + blocked + "')");
    } else
      stm.execute("REPLACE `ajb_user` (name, blocked) VALUES ('" + p.getName() + "', '" + blocked + "')");
  }
  
  public void setUser(String user, boolean blocked) throws SQLException {
    this.userBlacklist.put(user, Boolean.valueOf(blocked));
    Statement stm = this.con.createStatement();
    if (this.lite) {
      stm.execute("INSERT OR REPLACE INTO `ajb_user` (name, blocked) VALUES ('" + user + "', '" + blocked + "')");
    } else {
      stm.execute("REPLACE `ajb_user` (name, blocked) VALUES ('" + user + "', '" + blocked + "')");
    }
  }
}