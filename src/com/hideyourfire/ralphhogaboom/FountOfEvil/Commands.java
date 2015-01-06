package com.hideyourfire.ralphhogaboom.FountOfEvil;

import java.sql.ResultSet;
import java.sql.SQLException;

import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.hideyourfire.ralphhogaboom.FountOfEvil.Main;

public class Commands implements CommandExecutor {

	Main thisInstance;
	public SQLite sqlite;
	
	public Commands(Main instance) {
		thisInstance = instance;
	}
	
	public static int parseWithDefault(String number, int defaultVal) {
		try {
			return Integer.parseInt(number);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("fount")) {
			// Check sender permissions
			if (sender instanceof Player) {
				Player p = (Player) sender;
				if (!(p.hasPermission("fount.admin"))) {
					sender.sendMessage("Error: you don't have permission to use that command. Please add 'fount.admin' to your permissions group.");
					return false;
				}
			}
			// K, split out the different commands.
			// Check args length
			if (args.length > 0) {
				if (args[0].equalsIgnoreCase("list")) {
					// list dreams; fetch SQL query and list all founts
					try {
						sqlConnection();
						String sqlQuery = "SELECT id, fountName, mob, world, maxMobs FROM founts ORDER BY id;";
						ResultSet rs = sqlite.query(sqlQuery);
						int i = 0;
						while (rs.next()) {
							++i;
							sender.sendMessage("" + ChatColor.DARK_GREEN + i + ": " + rs.getString("fountName") + " [" + rs.getInt("maxMobs") + " " + rs.getString("mob") + "s on " + rs.getString("world") + "]");
						}
						if (i < 1) {
							sender.sendMessage("There are no founts in the database. Type " + ChatColor.GOLD + "/fount add <mob> <amount> <description>" + ChatColor.WHITE + " to create a mob fount.");
						}
						rs.close();
					} catch (SQLException e) {
						sender.sendMessage(ChatColor.RED + "Can't list founts; an error occurred. Most likely, the database is locked momentarily.");
						if (thisInstance.doDebug()) {
							e.printStackTrace();
						}
					}
				}
				if (args[0].equalsIgnoreCase("delete")) {
					if (args.length == 2) {
						// Check if it's a number
						if (isNumeric(args[1].toString())) {
							String sqlQuery = "SELECT * FROM founts ORDER BY id;";
							ResultSet rs;
							try {
								sqlConnection();
								rs = sqlite.query(sqlQuery);
								int i = 0;
								while (rs.next()) {
									++i;
									if (i == Integer.parseInt(args[1].toString())) {
										// Delete it, yo
										if (thisInstance.doDebug()) {
											Main.getPlugin().getLogger().info("world: " + rs.getString("world") + ", int: " + rs.getInt("id"));
										}
 										sqlQuery = "DELETE FROM founts WHERE id = " + rs.getInt("id") + ";";
									}
								}
								rs.close();
								try {
									if (thisInstance.doDebug()) {
										Main.getPlugin().getLogger().info("DEBUG - sqlQuery: " + sqlQuery);
									}
									sqlite.query(sqlQuery);
									sender.sendMessage("Fount " + args[1].toString() + " has been deleted.");
								} catch (SQLException e) {
									e.printStackTrace();
									sender.sendMessage("An internal error occurred; check the console log for specific errors.");
								}
								rs.close();
							} catch (SQLException e1) {
								e1.printStackTrace();
							}
						}
					} else {
						sender.sendMessage(ChatColor.YELLOW + "Unrecognized command format.");
						sender.sendMessage("  Example: " + ChatColor.GOLD + "/fount delete 8");
						return false;
					}
				}
				if (args[0].equalsIgnoreCase("add")) {
					// Check sender is player
					if (sender instanceof Player) {
						Player player = (Player) sender;
												
						// CHECK FOR MAX FOUNTS
						if (thisInstance.doWarnOnMaxFounts()) {
							sender.sendMessage(ChatColor.RED + "Warning:" + ChatColor.GOLD + " max founts exceeded in the database. This fount will be added, but will not spawn mobs.");
							sender.sendMessage(ChatColor.GOLD + "Either reduce the number of founts in the database with /fount delete, or increase the maxFounts value in the config file.");
						}
						
						// Check mob argument
						if (args.length < 2) {
							sender.sendMessage("Error: missing elements in command.");
							sender.sendMessage("  Example: " + ChatColor.GOLD + "/fount add skeleton 1 skellyfount");
						}
						if (args[1].equalsIgnoreCase("pigzombie") || args[1].equalsIgnoreCase("skeleton") || args[1].equalsIgnoreCase("spider") || args[1].equalsIgnoreCase("ghast") || args[1].equalsIgnoreCase("zombie") || args[1].equalsIgnoreCase("cavespider") || args[1].equalsIgnoreCase("creeper") || args[1].equalsIgnoreCase("endermite") || args[1].equalsIgnoreCase("blaze") || args[1].equalsIgnoreCase("magmacube") || args[1].equalsIgnoreCase("silverfish") || args[1].equalsIgnoreCase("skeleton") || args[1].equalsIgnoreCase("slime") || args[1].equalsIgnoreCase("witch") || args[1].equalsIgnoreCase("wither") || args[1].equalsIgnoreCase("zombie") || args[1].equalsIgnoreCase("guardian") || args[1].equalsIgnoreCase("giant")) {
							// Check that argument is an integer between x and y.
							if (parseWithDefault(args[2], 0) != 0) {
								// Check that there's a spawn name.
								String fountName = "unnamedFount";
								if (args.length > 3) {
									fountName = args[3].toString();
								}
								// add spawn fount to the database.
								sqlConnection();
								String sqlInsert = "INSERT INTO founts (fountName, chunk, frequency, locX, locY, locZ, world, maxMobs, mob) VALUES ('" + fountName + "', 'x=" + player.getLocation().getChunk().getX() + "z=" + player.getLocation().getChunk().getZ() + "', '" + 1 + "', " + player.getLocation().getX() + ", " + player.getLocation().getY() + ", " + player.getLocation().getZ() + ", '" + player.getWorld().getName().toLowerCase() + "', " + args[2].replace("'", "`") + ", '" + args[1].toLowerCase().replace("'", "''") + "');";
										
						    	if (thisInstance.doDebug()) {
							    	thisInstance.getLogger().info("sqlInsert: " + sqlInsert);
						    	}
								try {
									sqlite.query(sqlInsert);
									sender.sendMessage("Fount created successfully.");
									return true;
								} catch (SQLException e) {
									sender.sendMessage("Error saving Fount - check the console log for errors, or file a bug at github.com/ralphhogaboom.");
									Main.getPlugin().getLogger().info("Error saving Fount: " + e.getMessage());
								}
							} else {
								sender.sendMessage("Error: Expected an amount; got '" + args[2] + "' instead.");
								return false;
							}
						} else {
							sender.sendMessage("Error: Expected a mob name (pigzombie, skeleton, ghast, or spider). Please check the command and try again.");
							return false;
						}
					return true;
					}
				}
				if (args[0].equalsIgnoreCase("mobs")) {
					sender.sendMessage("Mob options available: blaze, creeper, spider, cavespider, skeleton, wither, ghast, pigzombie, endermite, slime, silverfish, giant.");
					return true;
				}
				if (args[0].equalsIgnoreCase("tpto")) {
					// Validate name
					if (args.length > 1) {
						String sqlQuery = "SELECT locX, locY, locZ, world, fountName FROM founts WHERE fountName LIKE '%" + args[1].replace("'", "\'") + "%' LIMIT 1;";
				    	if (thisInstance.doDebug()) {
					    	thisInstance.getLogger().info("sqlQuery on tpto: " + sqlQuery);
				    	}
						sqlConnection();
						ResultSet rs;
						try {
							rs = sqlite.query(sqlQuery);
							while (rs.next()) {
								Player player = (Player) sender;
								World targetWorld = Bukkit.getWorld(rs.getString("world"));
								Location loc = new Location(targetWorld, rs.getInt("locX"), rs.getInt("locY"), rs.getInt("locZ"));
						    	if (thisInstance.doDebug()) {
							    	thisInstance.getLogger().info("Teleporting " + player.getName() + " to " + loc.toString());
						    	}
								player.teleport(loc);
								rs.close();
								return true;
							}
						} catch (SQLException e) {
							sender.sendMessage("An error occurred trying to locate the font Check the console log for details.");
							e.printStackTrace();
							Main.getPlugin().getLogger().info("Error teleporting to Fount. See stack trace above.");
							return true;
						} 
						sender.sendMessage("Location '" + args[1].toString() + "' not found. For a list of founds, type " + ChatColor.GOLD + "/founts list" + ChatColor.WHITE + ".");
					}
					return true;
				}
			} else {
				sender.sendMessage(ChatColor.YELLOW + "Unrecognized command format.");
				sender.sendMessage(ChatColor.YELLOW + "The command format is " + ChatColor.GOLD + "/fount (add|delete|list)" + ChatColor.YELLOW + ".");
				sender.sendMessage("Example: " + ChatColor.GOLD + "/fount list");
				sender.sendMessage("     or: " + ChatColor.GOLD + "/fount delete 14");
				sender.sendMessage("     or: " + ChatColor.GOLD + "/fount add zombie 6 zombie_fount_near_forest");
				return true;
			}
		}
		return false;
	}
			
			
			
			
			
//			if (args[0].equalsIgnoreCase("list")) {
//				try {
//					sqlConnection();
//					String sqlQuery = "SELECT fountName, world, locX, locY, locZ, mob FROM founts ORDER BY id;";
//					ResultSet rs = sqlite.query(sqlQuery);
//					int i = 0;
//					while (rs.next()) {
//						++i;
//						sender.sendMessage("" + ChatColor.DARK_GREEN + i + ": " + rs.getString("fountName") + " [" + rs.getString("mob") + "] in " + rs.getString("world"));
//					}
//					if (i < 1) {
//						sender.sendMessage("There are no founts yet. To make one, type " + ChatColor.GOLD + "/fount add (skeleton|creeper) <amount> <name>" + ChatColor.WHITE + ".");
//					}
//					return true;
//				} catch (SQLException e) {
//					sender.sendMessage("Error: Can't list founts - an error ocurred. Try again in 15 seconds.");
//					e.printStackTrace();
//					Main.getPlugin().getLogger().info("Either the database file is locked, or there's a bug in the plugin. Please report the error and your server log to the plugin page on Bukkit, or on github.com/ralphhogaboom.");
//				}
//				return true;
//			}
//			if (args[0].equalsIgnoreCase("add")) {
//				// Check sender is player
//				if (sender instanceof Player) {
//					Player player = (Player) sender;
//					// Check mob argument
//					if (args.length < 2) {
//						sender.sendMessage("Error: missing elements in command.");
//						sender.sendMessage("  Example: " + ChatColor.GOLD + "/fount add skeleton 1 skellyfount");
//					}
//					if (args[1].equalsIgnoreCase("pigzombie") || args[1].equalsIgnoreCase("skeleton") || args[1].equalsIgnoreCase("spider") || args[1].equalsIgnoreCase("ghast") || args[1].equalsIgnoreCase("zombie") || args[1].equalsIgnoreCase("cavespider") || args[1].equalsIgnoreCase("creeper") || args[1].equalsIgnoreCase("endermite") || args[1].equalsIgnoreCase("blaze") || args[1].equalsIgnoreCase("magmacube") || args[1].equalsIgnoreCase("silverfish") || args[1].equalsIgnoreCase("skeleton") || args[1].equalsIgnoreCase("slime") || args[1].equalsIgnoreCase("witch") || args[1].equalsIgnoreCase("wither") || args[1].equalsIgnoreCase("zombie") || args[1].equalsIgnoreCase("guardian") || args[1].equalsIgnoreCase("giant")) {
//						// Check that argument is an integer between x and y.
//						if (parseWithDefault(args[2], 0) != 0) {
//							// Check that there's a spawn name.
//							String fountName = "unnamedFount";
//							if (args[3].length() > 0) {
//								fountName = args[3].toString();
//							}
//							// add spawn fount to the database.
//							sqlConnection();
//							String sqlInsert = "INSERT INTO founts (fountName, chunk, frequency, locX, locY, locZ, world, maxMobs, mob) VALUES ('" + fountName + "', 'x=" + player.getLocation().getChunk().getX() + "z=" + player.getLocation().getChunk().getZ() + "', '" + 1 + "', " + player.getLocation().getX() + ", " + player.getLocation().getY() + ", " + player.getLocation().getZ() + ", '" + player.getWorld().getName().toLowerCase() + "', " + args[2] + ", '" + args[1].toLowerCase() + "');";
//					    	if (thisInstance.doDebug()) {
//						    	thisInstance.getLogger().info("sqlInsert: " + sqlInsert);
//					    	}
//							try {
//								sqlite.query(sqlInsert);
//								sender.sendMessage("Fount created successfully.");
//								return true;
//							} catch (SQLException e) {
//								sender.sendMessage("Error saving Fount - check the console log for errors, or file a bug at github.com/ralphhogaboom.");
//								Main.getPlugin().getLogger().info("Error saving Fount: " + e.getMessage());
//							}
//						} else {
//							sender.sendMessage("Error: Expected an amount; got '" + args[2] + "' instead.");
//							return false;
//						}
//					} else {
//						sender.sendMessage("Error: Expected a mob name (pigzombie, skeleton, ghast, or spider). Please check the command and try again.");
//						return false;
//					}
//				return true;
//				}
//			}
//			if (args[0].equalsIgnoreCase("delete")) {
//				if (args.length == 2) {
//					if (isNumeric(args[1].toString())) {
//						String sqlQuery = "SELECT id, fountName, world FROM founts ORDER BY id;";
//						ResultSet rs;
//						try {
//							sqlConnection();
//							rs = sqlite.query(sqlQuery);
//							int i = 0;
//							while (rs.next()) {
//								++i;
//								if (i == Integer.parseInt(args[1].toString())) {
//									// Delete it
//									sqlQuery = "DELETE FROM founts WHERE id = " + rs.getInt("id") + ";";
//								}
//							}
//							try {
//								sqlite.query(sqlQuery);
//								sender.sendMessage("Fount " + args[1].toString() + " has been deleted.");
//							} catch (SQLException e) {
//								e.printStackTrace();
//								sender.sendMessage("An internal error occurred; check the console log for the SQLibrary stack trace and submit a bug report.");
//							}
//						} catch (SQLException e) {
//							e.printStackTrace();
//							sender.sendMessage("An internal error occurred; check the console log for the SQLibrary stack trace and submit a bug report.");
//						}
//					} else {
//						sender.sendMessage("Error: incorrect command formatting.");
//						sender.sendMessage("  Example: " + ChatColor.GOLD + "/founts delete 12" + ChatColor.WHITE + " to delete the 12th listing in the database.");
//					}
//				} else {
//					sender.sendMessage("Error: incorrect command formatting.");
//					sender.sendMessage("  Example: " + ChatColor.GOLD + "/founts delete 12" + ChatColor.WHITE + " to delete the 12th listing in the database.");
//				}
//				return true;
//			}
//			// Now, assume it was something else so we better help them out.	
//			sender.sendMessage("Type " + ChatColor.GOLD + "/fount list" + ChatColor.WHITE + " to show all a list of all custom mob spawn areas.");
//			sender.sendMessage("Type " + ChatColor.GOLD + "/fount add <MOBNAME> <AMOUNT> description" + ChatColor.WHITE + " to add a new custom mob spawn area.");
//			sender.sendMessage("Type " + ChatColor.GOLD + "/fount delete <ID>" + ChatColor.WHITE + " to remove one.");
//			sender.sendMessage("Type " + ChatColor.GOLD + "/fount tpto <ID>" + ChatColor.WHITE + " to teleport to a specific fount.");
//			sender.sendMessage("Type " + ChatColor.GOLD + "/fount mobs" + ChatColor.WHITE + " to show all possible mob options when creating founts.");
//			// sender.sendMessage("Type " + ChatColor.GOLD + "/fount purge" + ChatColor.WHITE + " to immediately clear all founts.");
//			return true;
//		}
//		return true;
//	}
	
	public static boolean isNumeric(String str) {  
		try {  
			double d = Double.parseDouble(str);  
		} catch(NumberFormatException nfe) {  
			return false;  
		}  
		return true;  
	}
	
	public void sqlConnection() {
		sqlite = new SQLite(Main.getPlugin().getLogger(), "FountOfEvil", Main.getPlugin().getDataFolder().getAbsolutePath(), "Founts");
		try {
			sqlite.open();
		} catch (Exception e) {
			sqlite.close();
			Main.getPlugin().getLogger().info(e.getMessage());
		}
	}
}
