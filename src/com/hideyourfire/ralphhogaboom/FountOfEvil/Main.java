package com.hideyourfire.ralphhogaboom.FountOfEvil;

import java.sql.ResultSet;
import java.sql.SQLException;

import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Wither;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.hideyourfire.ralphhogaboom.FountOfEvil.Commands;

public class Main extends JavaPlugin {
	private static Plugin plugin;
	public SQLite sqlite;
	
	public void onEnable() {	
		plugin = this;
		registerEvents(this, new EventListener(this));
		this.saveDefaultConfig(); // For first run, save default config file.
		this.getConfig();
		sqlConnection();
		try {
			sqlTableCheck();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		getCommand("fount").setExecutor(new Commands());
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				doCheckAllFounts();
			}
		}, 0L, 999L);
	}
	
	private void doCheckAllFounts() {
		sqlConnection();
		String sqlQuery = "SELECT id, fountName, chunk, world, maxMobs, mob, locX, locY, locZ FROM founts;";
		ResultSet rs;
		try {
			rs = sqlite.query(sqlQuery);
			try {
				while (rs.next()) {
					World world = Bukkit.getWorld(rs.getString("world"));
					String strX = rs.getString("chunk");
					String strY = strX.substring(strX.indexOf("z"), strX.length());
					strX = strX.substring(0, (strX.indexOf("z") - 1));
					strX = strX.substring(2);
					strY = strY.substring(2);
					int intX = Integer.parseInt(strX);
					int intY = Integer.parseInt(strY);
					Chunk chunk = world.getChunkAt(intX, intY);
					// Check if chunk is loaded
					if (chunk.isLoaded()) {
						// Do runMobSpawnCheck
						Main.runMobSpawnCheck(rs.getInt("locX"), rs.getInt("locY"), rs.getInt("locZ"), rs.getString("world"), rs.getString("mob"), rs.getInt("maxMobs"));
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void registerEvents(org.bukkit.plugin.Plugin plugin, Listener...listeners) {
		for (Listener listener : listeners) {
			Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
		}
	}

	public void onDisable() {
		sqlite.close();
		plugin = null; // Stops memory leaks.
	}
	
	public static Plugin getPlugin() {
		return plugin;
	}
	
	public int getKey(String key) {
		return 0;
	}
	
	public void setKey(String key, int value) {
	}
		
	@SuppressWarnings("deprecation")
	public void sqlTableCheck() throws SQLException {
	    if(sqlite.checkTable("founts")){
	    	this.getLogger().info("Database founts opened; table 'founts' found.");
	    	return;
	    } else {
	    	sqlite.query("CREATE TABLE founts (id INT PRIMARY KEY, fountName VARCHAR(50), chunk VARCHAR(50), frequency INT(3), locX INT(50), locY INT(50), locZ INT(50), world VARCHAR(50), mob VARCHAR(50));");
	        sqlite.query("INSERT INTO founts(fountName, chunk, frequency, locX, locY, locZ, world, mob) VALUES('GhastPlaza', 'x=29z=-25', 10, 200, 84, 199, 'survival', 'ghast');"); //This is optional. You can do this later if you want.
	        plugin.getLogger().info("founts has been created");
	    }
	}

	public void sqlConnection() {
		sqlite = new SQLite(Main.getPlugin().getLogger(), "FountOfEvil", Main.getPlugin().getDataFolder().getAbsolutePath(), "Founts");
		try {
			sqlite.open();
		} catch (Exception e) {
			Main.getPlugin().getLogger().info(e.getMessage());
		}
	}

	@SuppressWarnings("deprecation")
	public static void runMobSpawnCheck(int X, int Y, int Z, String world, String mob, int maxMobs) {
		World targetWorld = Bukkit.getWorld(world);
		Location loc = new Location(targetWorld, X, Y, Z);
		int playerX = 0;
		int playerZ = 0;
		// Is a player in this chunk?
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			playerX = player.getLocation().getBlockX();
			playerZ = player.getLocation().getBlockZ();
			if (playerX > (X - 100) && playerX < (X + 100)) {
				Main.getPlugin().getLogger().info(playerX + " > " + (X - 100) + " && " + playerX + " < " + (X + 100));
				if (playerZ > (Z - 100) && playerZ < (Z + 100)) {
					Main.getPlugin().getLogger().info(playerZ + " > " + (Z - 100) + " && " + playerZ + " < " + (Z + 100));
					Main.getPlugin().getLogger().info("Player " + player.getName() + " is near " + X + ", " + Z + ".");
					if (mob.equalsIgnoreCase("spider")) {
						loc.getWorld().spawnEntity(loc, EntityType.SPIDER);
						// Count all mobs in chunk, if they match this mob then add 'em up
						int i = 0;
						for (Entity ents : targetWorld.getChunkAt(loc).getEntities()) {
							if (ents instanceof LivingEntity) {
								LivingEntity lents = (LivingEntity) ents;
								if (lents instanceof Spider) {
									++i;
								}
							}
						}
						if (i <= maxMobs) {
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					if (mob.equalsIgnoreCase("ghast")) {
						loc.getWorld().spawnEntity(loc, EntityType.GHAST);
						// Count all mobs in chunk, if they match this mob then add 'em up
						int i = 0;
						for (Entity ents : targetWorld.getChunkAt(loc).getEntities()) {
							if (ents instanceof LivingEntity) {
								LivingEntity lents = (LivingEntity) ents;
								if (lents instanceof Ghast) {
									++i;
								}
							}
						}
						if (i <= maxMobs) {
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					if (mob.equalsIgnoreCase("pigzombie")) {
						loc.getWorld().spawnEntity(loc, EntityType.PIG_ZOMBIE);
						// Count all mobs in chunk, if they match this mob then add 'em up
						int i = 0;
						for (Entity ents : targetWorld.getChunkAt(loc).getEntities()) {
							if (ents instanceof LivingEntity) {
								LivingEntity lents = (LivingEntity) ents;
								if (lents instanceof PigZombie) {
									++i;
								}
							}
						}
						if (i <= maxMobs) {
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					if (mob.equalsIgnoreCase("wither")) {
						loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
						// Count all mobs in chunk, if they match this mob then add 'em up
						int i = 0;
						for (Entity ents : targetWorld.getChunkAt(loc).getEntities()) {
							if (ents instanceof LivingEntity) {
								LivingEntity lents = (LivingEntity) ents;
								if (lents instanceof Skeleton) {
									++i;
								}
							}
						}
						if (i <= maxMobs) {
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
				}
			}
		}
	}
	
	

}
