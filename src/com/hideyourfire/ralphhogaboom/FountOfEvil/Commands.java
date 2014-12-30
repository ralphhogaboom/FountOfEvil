package com.hideyourfire.ralphhogaboom.FountOfEvil;

import java.sql.SQLException;

import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {

	Main thisInstance;
	public SQLite sqlite;
	
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
			if (args.length < 1) {
				Main.getPlugin().getLogger().info("No arguments detected.");
				sender.sendMessage("Type " + ChatColor.GOLD + "/fount list" + ChatColor.WHITE + " to show all a list of all custom mob spawn areas.");
				sender.sendMessage("Type " + ChatColor.GOLD + "/fount add (creeper|spider|cavespider|skeleton|wither|ghast|pigzombie|endermite|slime|silverfish|giant) amt name" + ChatColor.WHITE + " to add a new custom mob spawn area.");
				sender.sendMessage("Type " + ChatColor.GOLD + "/fount delete <ID>" + ChatColor.WHITE + " to remove one.");
				return true;
			}
			if (args.length > 1) {
				if (args[0].equalsIgnoreCase("list")) {
					sender.sendMessage("This command intentionally left blank.");
					return true;
				}
				if (args[0].equalsIgnoreCase("add")) {
					// Check sender is player
					if (sender instanceof Player) {
						// Check permissions
						Player player = (Player) sender;
						if (player.hasPermission("fount.admin")) {
							// Check mob argument
							if (args[1].equalsIgnoreCase("pigzombie") || args[1].equalsIgnoreCase("skeleton") || args[1].equalsIgnoreCase("spider") || args[1].equalsIgnoreCase("ghast") || args[1].equalsIgnoreCase("zombie") || args[1].equalsIgnoreCase("cavespider") || args[1].equalsIgnoreCase("creeper") || args[1].equalsIgnoreCase("endermite") || args[1].equalsIgnoreCase("blaze") || args[1].equalsIgnoreCase("magmacube") || args[1].equalsIgnoreCase("silverfish") || args[1].equalsIgnoreCase("skeleton") || args[1].equalsIgnoreCase("slime") || args[1].equalsIgnoreCase("witch") || args[1].equalsIgnoreCase("wither") || args[1].equalsIgnoreCase("zombie") || args[1].equalsIgnoreCase("guardian") || args[1].equalsIgnoreCase("giant")) {
								// Check that argument is an integer between x and y.
								if (parseWithDefault(args[2], 0) != 0) {
									// Check that there's a spawn name.
									String fountName = "unnamedFount";
									if (args[3].length() > 0) {
										fountName = args[3].toString();
									}
									// add spawn fount to the database.
									sqlConnection();
									String sqlInsert = "INSERT INTO founts (fountName, chunk, frequency, locX, locY, locZ, world, maxMobs, mob) VALUES ('" + fountName + "', 'x=" + player.getLocation().getChunk().getX() + "z=" + player.getLocation().getChunk().getZ() + "', '" + 1 + "', " + player.getLocation().getX() + ", " + player.getLocation().getY() + ", " + player.getLocation().getZ() + ", '" + player.getWorld().getName().toLowerCase() + "', " + args[2] + ", '" + args[1].toLowerCase() + "');";
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
						} else {
							sender.sendMessage("Error: you don't have permission to do that. Please add the fount.admin permissions node to your group or player.");
							return false;
						}
					} else {
						sender.sendMessage("This command can only be used by a player.");
						return false;
					}
					return true;
				}
				if (args[0].equalsIgnoreCase("delete")) {
					return true;
				}
				return true;
			}
			if (args.length == 2) {
				Main.getPlugin().getLogger().info("2 arguments detected.");
				return true;
			}
		}
		return false;
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
