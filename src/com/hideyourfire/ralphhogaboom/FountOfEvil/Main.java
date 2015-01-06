package com.hideyourfire.ralphhogaboom.FountOfEvil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Zombie;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.hideyourfire.ralphhogaboom.FountOfEvil.Commands;

public class Main extends JavaPlugin {
	private static Plugin plugin;
	public SQLite sqlite;
	private boolean debug = false;
	private int maxSpawnMobs = 0;
	private int maxFounts = 0;
	
	public void onEnable() {	
		plugin = this;
		this.saveDefaultConfig(); // For first run, save default config file.
		this.getConfig();
		debug = this.getConfig().getBoolean("debug");
		Main.getPlugin().getLogger().info("Show debug output: " + doDebug());
		maxSpawnMobs = this.getConfig().getInt("maxSpawnMobs");
		maxFounts = this.getConfig().getInt("maxFounts");
		
		
		sqlConnection();
		sqlTableCheck();
		getCommand("fount").setExecutor(new Commands(this));
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				doCheckAllFounts();
			}
		}, 0L, 999L);

		// CHECK FOR MAX FOUNTS
		if (doWarnOnMaxFounts()) {
			Main.getPlugin().getLogger().info("Warning: max founts exceeded in the database. Only the first " + maxFounts + " founts will be loaded.");
			Main.getPlugin().getLogger().info("Either reduce the number of founts in the database with /fount delete, or increase the maxFounts value in the config file.");
		}
	}
	
	private void doCheckAllFounts() {
		sqlConnection();
		String sqlQuery = "SELECT id, fountName, chunk, world, maxMobs, mob, locX, locY, locZ FROM founts LIMIT " + maxFounts + ";";
		ResultSet rs;
		try {
			rs = sqlite.query(sqlQuery);
			try {
				while (rs.next()) {
					if (doDebug()) {
						Main.getPlugin().getLogger().info("World: " + rs.getString("world"));
						Main.getPlugin().getLogger().info("Chunk: " + rs.getString("chunk"));
					}
					World world = Bukkit.getWorld(rs.getString("world"));
					String strX = rs.getString("chunk");
					String strY = strX.substring(strX.indexOf("z"), strX.length());
					strX = strX.replace(strY, "");
					strX = strX.substring(2);
					strY = strY.substring(2);
					if (doDebug()) {
						Main.getPlugin().getLogger().info("strX: " + strX + "; strY = " + strY);
					}
					int intX = Integer.parseInt(strX);
					int intY = Integer.parseInt(strY);
					int intMaxSpawnMobs = 0;
					if (rs.getInt("maxMobs") <= maxSpawnMobs) {
						intMaxSpawnMobs = rs.getInt("maxMobs");
					} else {
						intMaxSpawnMobs = maxSpawnMobs;
					}
					Chunk chunk = world.getChunkAt(intX, intY);
					// Check if chunk is loaded
					if (chunk.isLoaded()) {
						// Do runMobSpawnCheck
						Main.runMobSpawnCheck(rs.getInt("locX"), rs.getInt("locY"), rs.getInt("locZ"), rs.getString("world"), rs.getString("mob"), intMaxSpawnMobs);
					}
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
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
	public void sqlTableCheck() {
	    if(sqlite.checkTable("founts")){
	    	if (doDebug()) {
		    	this.getLogger().info("Database founts opened; table 'founts' found.");
	    	}
	    	return;
	    } else {
	    	try {
				sqlite.query("CREATE TABLE founts (id INTEGER PRIMARY KEY AUTOINCREMENT, fountName VARCHAR(50), chunk VARCHAR(50), frequency INT(3), locX INT(50), locY INT(50), locZ INT(50), maxMobs INT(50), world VARCHAR(50), mob VARCHAR(50));");
			} catch (SQLException e) {
				e.printStackTrace();
			}
	    	if (doDebug()) {
	    		plugin.getLogger().info("founts has been created");
	    	}
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
		int i = 0;
		// Is a player near enough to this chunk?
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			playerX = player.getLocation().getBlockX();
			playerZ = player.getLocation().getBlockZ();
			if (playerX > (X - 100) && playerX < (X + 100)) {
				if (playerZ > (Z - 100) && playerZ < (Z + 100)) {
					// Spawn dummy entity, so I can get a count of everything around it.
					Entity entity = loc.getWorld().spawnEntity(loc, EntityType.SNOWBALL);

					if (mob.equalsIgnoreCase("blaze")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(64, 64, 64);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Blaze) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.BLAZE);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					
					if (mob.equalsIgnoreCase("cavespider")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(64, 64, 64);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof CaveSpider) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.CAVE_SPIDER);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					
					if (mob.equalsIgnoreCase("creeper")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(32, 32, 32);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Creeper) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.CREEPER);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					if (mob.equalsIgnoreCase("endermite")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(32, 32, 32);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Endermite) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.ENDERMITE);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					if (mob.equalsIgnoreCase("ghast")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(256, 256, 256);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Ghast) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.GHAST);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					
					if (mob.equalsIgnoreCase("giant")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(256, 256, 256);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Giant) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.GIANT);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					
					if (mob.equalsIgnoreCase("guardian")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(256, 256, 256);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Guardian) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.GUARDIAN);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					if (mob.equalsIgnoreCase("magmacube")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(32, 32, 32);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof MagmaCube) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.MAGMA_CUBE);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					if (mob.equalsIgnoreCase("pigzombie")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(32, 32, 32);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof PigZombie) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.PIG_ZOMBIE);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					if (mob.equalsIgnoreCase("silverfish")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(32, 32, 32);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Silverfish) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.SILVERFISH);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					if (mob.equalsIgnoreCase("skeleton")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(32, 32, 32);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Skeleton) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					if (mob.equalsIgnoreCase("slime")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(32, 32, 32);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Slime) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.SLIME);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}

					if (mob.equalsIgnoreCase("spider")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(64, 64, 64);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Spider) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.SPIDER);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					
					if (mob.equalsIgnoreCase("witch")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(64, 64, 64);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Witch) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.WITCH);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
					if (mob.equalsIgnoreCase("wither")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(256, 256, 256);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Wither) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.WITHER);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
						entity.remove();
					}
					if (mob.equalsIgnoreCase("zombie")) {
						// Count all mobs in chunk, if they match this mob then add 'em up
						List<Entity> nearbyEntities = entity.getNearbyEntities(32, 32, 32);
						for (Entity ent : nearbyEntities) {
							if (ent instanceof LivingEntity) {
								LivingEntity thisEntity = (LivingEntity) ent;
								if (thisEntity instanceof Zombie) {
									++i;
								}
							}
						}
						if (i < maxMobs) {
							loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
							runMobSpawnCheck(X, Y, Z, world, mob, maxMobs);
						}
					}
				}
			}
		}
	}

	public boolean doDebug() {
		return debug;
	}


	public boolean doWarnOnMaxFounts() {
		// Check to warn if fount numbers exceed maxFounts in config.
		boolean isMax = false;
		try {
			String sqlQuery = "SELECT Count(id) AS fountCount FROM founts;";
			ResultSet rs = sqlite.query(sqlQuery);
			while (rs.next()) {
				if (rs.getInt("fountCount") > maxFounts) {
					isMax = true;
				}
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return isMax;
	}
	
}
