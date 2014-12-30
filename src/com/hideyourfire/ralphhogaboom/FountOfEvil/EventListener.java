package com.hideyourfire.ralphhogaboom.FountOfEvil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import lib.PatPeter.SQLibrary.SQLite;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import com.hideyourfire.ralphhogaboom.FountOfEvil.Main;

public class EventListener implements Listener {
	
	Main thisInstance;
	public SQLite sqlite;

	public EventListener(Main instance) {
		  thisInstance = instance;
		}
	
	public void sqlConnection() {
		sqlite = new SQLite(Main.getPlugin().getLogger(), "FountOfEvil", Main.getPlugin().getDataFolder().getAbsolutePath(), "Founts");
		try {
		sqlite.open();
		    } catch (Exception e) {
				sqlite.close();
		    	// Main.getPlugin().getLogger().info(e.getMessage());
		    	Main.getPlugin().getLogger().info("Opening sqlite database failed. Does your host support sqlite?");
		    	Main.getPlugin().onDisable();
		    }
		}
	
	public static int randInt(int min, int max) {
		Random rand = new Random();
		int randomNum = rand.nextInt((max - min) + 1) + min;
		return randomNum;
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		Chunk chunk = event.getChunk();
		sqlConnection();
		String sqlQuery = "SELECT id, fountName, chunk, world, maxMobs, mob, locX, locY, locZ FROM founts WHERE world = '" + event.getWorld().getName() + "' AND chunk = 'x=" + chunk.getX() + "z=" + chunk.getZ() + "';'";
		try {
			ResultSet rs = sqlite.query(sqlQuery);
			while (rs.next()) {
				// Check how many entities are in that location
				Main.getPlugin().getLogger().info("Now running command: runMobSpawnCheck(" + rs.getInt("locX") + ", " + rs.getInt("locY") + ", " + rs.getInt("locZ") + ", " +  rs.getString("world") + ", " +  rs.getString("mob") + ", " +  rs.getInt("maxMobs"));
				Main.runMobSpawnCheck(rs.getInt("locX"), rs.getInt("locY"), rs.getInt("locZ"), rs.getString("world"), rs.getString("mob"), rs.getInt("maxMobs"));
			}
		} catch (SQLException e) {
			Main.getPlugin().getLogger().info("SQLite query failed: " + sqlQuery);
		}
	}
}
