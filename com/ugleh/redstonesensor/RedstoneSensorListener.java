package com.ugleh.redstonesensor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RedstoneSensorListener implements Listener {
	public Location nullLocation = new Location(Bukkit.getServer().getWorlds().get(0), 0, 0, 0);
	public HashMap<String, ArrayList<Location>> playerLocationStorage = new HashMap<String, ArrayList<Location>>();
	private RedstoneSensorPlugin plugin;

	public RedstoneSensorListener(RedstoneSensorPlugin p) {
		plugin = p;
	}

	@EventHandler
	public void BlockCreated(BlockPlaceEvent event) {
		if (event.getPlayer().hasPermission("redstonesensor.use")) {
			Block blk = event.getBlock();
            if(!(blk.getType()==Material.REDSTONE_TORCH_OFF ||
                    blk.getType()==Material.REDSTONE_TORCH_ON))
                return;

            RedstoneSensor.Type type = RedstoneSensor.check(event.getPlayer().getItemInHand());
            if(type == RedstoneSensor.Type.NADA)
                return;

			ArrayList<String> list = new ArrayList<String>();
			list.add(String.valueOf(RedstoneSensorPlugin.defaultRange));
			list.add(event.getPlayer().getName());
			list.add("null");

            RedstoneSensor.Torch torch = new RedstoneSensor.Torch(
                    type==RedstoneSensor.Type.NORMAL,
                    RedstoneSensorPlugin.defaultRange,
                    event.getPlayer().getName(),
                    blk.getLocation());

            RedstoneSensor.torchList.add(torch);
			String setname = "Redstones." + torch.getIdentifier();
            plugin.getConfig().set(setname, null);
            plugin.getConfig().set(setname + ".Range", 3);
            plugin.getConfig().set(setname + ".CustomRange", null);
            plugin.getConfig().set(setname + ".Owner", event.getPlayer().getName());
            plugin.getConfig().set(setname + ".X", blk.getLocation().getBlockX());
            plugin.getConfig().set(setname + ".Y", blk.getLocation().getBlockY());
            plugin.getConfig().set(setname + ".Z", blk.getLocation().getBlockZ());
            plugin.getConfig().set(setname + ".World", blk.getLocation().getWorld().getName());
            try {
                plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
		}
	}

	@EventHandler
	public void InventoryClick(InventoryClickEvent event) {
		if (!event.getWhoClicked().hasPermission("redstonesensor.create")) {
			ItemStack rps = new ItemStack(Material.REDSTONE_TORCH_OFF, 1);
			ItemMeta rpsmeta = rps.getItemMeta();
			rpsmeta.setDisplayName(ChatColor.RED + RedstoneSensor.text);
			rps.setItemMeta(rpsmeta);

			ItemStack rps2 = new ItemStack(Material.REDSTONE_TORCH_OFF, 1);
			ItemMeta rpsmeta2 = rps2.getItemMeta();
			rpsmeta2.setDisplayName(ChatColor.RED + RedstoneSensor.notText);
			rps2.setItemMeta(rpsmeta2);

			if ((event.getResult().equals(rps2) || (event.getResult().equals(rps)))) {
				event.setResult(Result.DENY);
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void ItemDrop(ItemSpawnEvent event) {
        RedstoneSensor.Torch torch = new RedstoneSensor.Torch(event.getLocation());

        boolean not = false;
		if (!RedstoneSensor.torchList.contains(torch)) {
            return;
        }

		RedstoneSensor.torchList.remove(torch);

        ItemStack rps = new ItemStack(Material.REDSTONE_TORCH_OFF, 1);
        ItemMeta rpsmeta = rps.getItemMeta();
        if(not)
            rpsmeta.setDisplayName(ChatColor.RED + RedstoneSensor.notText);
        else
            rpsmeta.setDisplayName(ChatColor.RED + RedstoneSensor.text);
        rps.setItemMeta(rpsmeta);
        event.getLocation().getBlock().getWorld().dropItem(event.getLocation(), rps);

        String setname = "Redstones." + torch.getIdentifier();
        plugin.getConfig().set(setname, null);
        try {
            plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        event.setCancelled(true);
	}

    String makeCoords(Location loc) {
        String theList = loc.getWorld().getName();
        theList += '|';
        theList += String.valueOf(loc.getBlockX());
        theList += '|';
        theList += String.valueOf(loc.getBlockY());
        theList += '|';
        theList += String.valueOf(loc.getBlockZ());
        return theList;
    }

    void setRange(Player player, Block blk) {
        if (!(blk.getType().equals(Material.REDSTONE_TORCH_OFF) || blk.getType().equals(Material.REDSTONE_TORCH_ON))) {
            if (blk.getRelative(BlockFace.UP).getType().equals(Material.REDSTONE_TORCH_OFF) || blk.getRelative(BlockFace.UP).getType().equals(Material.REDSTONE_TORCH_ON)) {
                blk = blk.getRelative(BlockFace.UP);
            } else {
                return;
            }
        }

        Location lok = blk.getLocation();
        RedstoneSensor.Torch torch = new RedstoneSensor.Torch(lok);

        if (!RedstoneSensor.torchList.contains(torch)) {
            player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "The block you're aiming at is not a RPS. Try aiming at the block under the RPS, or the RPS itself.");
            return;
        }
        // Add new range
        String setname = "Redstones." + torch.getIdentifier();
        ArrayList<Location> pair = playerLocationStorage.get(player.getName());
        Location l1 = pair.get(0);
        Location l2 = pair.get(1);
        if(l1.getWorld() != l2.getWorld()) {
            player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "N-dimensional regions are not supported yet.");
        }
        int x1 = Math.min(l1.getBlockX(), l2.getBlockX());
        int y1 = Math.min(l1.getBlockY(), l2.getBlockY());
        int z1 = Math.min(l1.getBlockZ(), l2.getBlockZ());
        int x2 = Math.max(l1.getBlockX(), l2.getBlockX());
        int y2 = Math.max(l1.getBlockY(), l2.getBlockY());
        int z2 = Math.max(l1.getBlockZ(), l2.getBlockZ());
        l1 = new Location(l1.getWorld(), x1, y1, z1);
        l2 = new Location(l2.getWorld(), x2, y2, z2);
        plugin.getConfig().set(setname + ".Range", -999);
        plugin.getConfig().set(setname + ".CustomRange", makeCoords(l1) + "|" + l2.getBlockX() + '|' + l2.getBlockY() + '|' + l2.getBlockZ());
        RedstoneSensor.torchList.remove(torch);
        RedstoneSensor.torchList.add(new RedstoneSensor.Torch(torch.isNot,-999,torch.owner,lok,l1,l2));
        player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "New custom range set.");

        try {
            plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	@EventHandler
	public boolean PlayerCommandPreprocess(PlayerCommandPreprocessEvent event) throws IOException {
		String cmd = event.getMessage();
		Player player = event.getPlayer();
		if (cmd.startsWith("/")) {
			cmd = cmd.substring(1);
		}
		List<String> arguments = new ArrayList<String>(Arrays.asList(cmd.split(" ")));
		if (arguments.size() > 0) {
			if ((arguments.get(0).matches("rps|redstonesensor|rsensor")) && (player.hasPermission("redstonesensor.commands"))) {
				if (arguments.size() == 1) {

					player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "/rps [maxrange/defaultrange/onlyowner/reload/wand/setrange]");
				}
				if (arguments.size() == 2) {
					if (("reload").equalsIgnoreCase(arguments.get(1))) {
						plugin.getServer().getPluginManager().disablePlugin(plugin);
						plugin.getServer().getPluginManager().enablePlugin(plugin);
						player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "Reloaded!");

					} else if (("defaultrange").equalsIgnoreCase(arguments.get(1))) {
						player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "/rps defaultrange <number>");
					} else if (("maxrange").equalsIgnoreCase(arguments.get(1))) {
						player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "/rps maxrange <number>");
					} else if (("onlyowner").equalsIgnoreCase(arguments.get(1))) {
						player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "/rps onlyowner <true/false>");
					} else if (("wand").equalsIgnoreCase(arguments.get(1))) {
						if (!player.hasPermission("redstonesensor.customrange")) {
							player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "You do not have permission to use that command");
						} else {
							player.getInventory().addItem(Wand.create());
							player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "RPS Wand Given. Left clicking a block with the RPS Wand marks that block as the first corner of the cuboid you wish to select. A right-click chooses the second corner.");
						}
					} else if (("setrange").equalsIgnoreCase(arguments.get(1))) {
						if (!player.hasPermission("redstonesensor.customrange")) {
							player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "You do not have permission to use that command");

						} else {
							if (playerLocationStorage.containsKey(event.getPlayer().getName())) {
								if (playerLocationStorage.get(event.getPlayer().getName()).get(0).equals(nullLocation) || playerLocationStorage.get(event.getPlayer().getName()).get(1).equals(nullLocation)) {
									player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "Your RPS Wand Selection is not complete. You are missing a corner.");

								} else {
                                    setRange(event.getPlayer(),event.getPlayer().getTargetBlock(null, 15));
								}
							} else {
								player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "You must first create a selection with the RPS Wand. Type '/rps wand' to get it");

							}
						}
					}
				} else if (arguments.size() == 3) {
					if (("defaultrange").equalsIgnoreCase(arguments.get(1))) {
						RedstoneSensorPlugin.defaultRange = Integer.valueOf(arguments.get(2));
						player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "Default range set to " + String.valueOf(Integer.valueOf(arguments.get(2))));
						plugin.getConfig().set("Config.default-range", RedstoneSensorPlugin.defaultRange);
						plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));
					} else if (("maxrange").equalsIgnoreCase(arguments.get(1))) {
						RedstoneSensorPlugin.maxRange = Integer.valueOf(arguments.get(2));
						player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "Max range set to " + String.valueOf(Integer.valueOf(arguments.get(2))));
						plugin.getConfig().set("Config.max-range", RedstoneSensorPlugin.maxRange);
						plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));
					} else if (("onlyowner").equalsIgnoreCase(arguments.get(1))) {
						RedstoneSensorPlugin.onlyOwner = Boolean.valueOf(arguments.get(2));
						player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "Owner Only Right Click set to " + String.valueOf(Boolean.valueOf(arguments.get(2))));
						plugin.getConfig().set("Config.owner-only-change-range", RedstoneSensorPlugin.onlyOwner);
						plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));

					}
				}
			}
		}
		return true;
	}

	@EventHandler
	public void PlayerJoin(PlayerJoinEvent event) {
        System.err.println("Playjoin");
		if(RedstoneSensorPlugin.updatechecker){
            if (event.getPlayer().isOp() && RedstoneSensorPlugin.outdated) {
                event.getPlayer().sendMessage(
                        ChatColor.DARK_PURPLE + "The version of " + ChatColor.DARK_RED + "Redstone Proximity Sensor" + ChatColor.DARK_PURPLE + " that this server is running is out of date. Please consider updating to the latest version at " + ChatColor.ITALIC + ChatColor.GREEN
                                + "http://dev.bukkit.org/server-mods/redstonesensor/");
            }
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerMove(PlayerMoveEvent event) {
        System.err.println("Playmove");
        Player player = event.getPlayer();
		Iterator<RedstoneSensor.Torch> it = RedstoneSensor.torchList.iterator();
        while (it.hasNext()) {
            RedstoneSensor.Torch torch = it.next();
            if(plugin.getServer().getWorld(torch.getWorld().getName())==null) {
                // whoops, was this world deleted?
                continue;
            }
            Block blk = torch.getBlock();
            if (blk.getType() == Material.REDSTONE_TORCH_ON || blk.getType() == Material.REDSTONE_TORCH_OFF) {
                boolean playerinside = (player.getWorld().getName() == torch.getWorld().getName()) &&
                         ((torch.value == -999) ? playerWithin(torch.l1, torch.l2, player.getLocation()) : torch.distance(player.getLocation()) <= torch.value);
                if (torch.isNot == false) {
                    if (playerinside == true) {
                        if (blk.getType() == Material.REDSTONE_TORCH_OFF){
                            BlockRedstoneEvent e = new BlockRedstoneEvent(blk, 15, 0);
                            Bukkit.getServer().getPluginManager().callEvent(e);
                            blk.setType(Material.REDSTONE_TORCH_ON);
                        }
                    } else {
                        if (blk.getType() == Material.REDSTONE_TORCH_ON){
                            blk.setType(Material.REDSTONE_TORCH_OFF);
                        }
                    }
                } else {
                    if (playerinside == true) {
                        if (blk.getType() == Material.REDSTONE_TORCH_ON)
                            blk.setType(Material.REDSTONE_TORCH_OFF);
                    } else {
                        if (blk.getType() == Material.REDSTONE_TORCH_OFF)
                            blk.setType(Material.REDSTONE_TORCH_ON);
                    }
                }
            } else {
                // whoops, there's no longer a torch at this location!
                it.remove();
                String setname = "Redstones." + torch.getIdentifier();
                plugin.getConfig().set(setname, null);
                try {
                    plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
	}

    @EventHandler(priority = EventPriority.MONITOR)
        public void PlayerRightClick(PlayerInteractEvent event) throws IOException {
            Player player = event.getPlayer();
            if (player.hasPermission("redstonesensor.use")) {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Block blk = event.getClickedBlock();
                    if ((blk.getType() == Material.REDSTONE_TORCH_ON) || (blk.getType() == Material.REDSTONE_TORCH_OFF)) {
                        // XXX: wow this is a hack!
                        RedstoneSensor.Torch torch = RedstoneSensor.torchList.ceiling(new RedstoneSensor.Torch(blk));
                        if ((torch != null) &&
                                ((RedstoneSensorPlugin.onlyOwner && torch.owner.equals(player.getName())) ||
                                 (!RedstoneSensorPlugin.onlyOwner || torch.owner == null)))
                        {
                            if (torch.value == -999) {
                                player.sendMessage(ChatColor.GOLD + "This RPS has a Custom Range and can not be changed via right clicking.");
                            } else {
                                Integer newvalue = RedstoneSensorPlugin.defaultRange;
                                if (torch.value == RedstoneSensorPlugin.maxRange) {
                                    newvalue = 1;
                                } else {
                                    newvalue = torch.value + 1;
                                }
                                RedstoneSensor.torchList.remove(torch);
                                RedstoneSensor.torchList.add(new RedstoneSensor.Torch(torch.isNot,newvalue,torch.owner,torch));

                                String setname = "Redstones." + torch.getIdentifier();
                                plugin.getConfig().set(setname + ".Range", newvalue);
                                plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));

                                player.sendMessage(ChatColor.GOLD + RedstoneSensor.notifyText + ": " + ChatColor.RED + newvalue.toString());
                            }
                        }
                    }
                }
            }

            // Now check for wand activity
            if (!(player.getItemInHand().getTypeId() == 0)) {
                if(Wand.check(player.getItemInHand())) {
                    if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                        Block clicked = event.getClickedBlock();

                        if (RedstoneSensor.torchList.contains(new RedstoneSensor.Torch(clicked))) {
                            // user has smacked a torch
                            setRange(player,clicked);
                            event.setCancelled(true);
                            return;
                        }

                        // XXX: It is now impossible to select a sensor torch as first corner of a range
                        // just break it and put another block there I guess.
                        // that way when you whack a torch you don't have to type /ts setrange to finish it!

                        ArrayList<Location> locations = new ArrayList<Location>();
                        locations.add(event.getClickedBlock().getLocation()); // Left click
                        if (playerLocationStorage.containsKey(player.getName()))
                            locations.add(playerLocationStorage.get(player.getName()).get(1)); // Right click
                        else
                            locations.add(nullLocation); // Right Click

                        player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "First corner set (" + clicked.getX() + "," + clicked.getY() + "," + clicked.getLocation().getBlockZ() + ")");
                        playerLocationStorage.put(player.getName(), locations);
                    } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                        ArrayList<Location> locations = new ArrayList<Location>();
                        if (playerLocationStorage.containsKey(player.getName()))
                            locations.add(playerLocationStorage.get(player.getName()).get(0)); // Left click
                        else
                            locations.add(nullLocation); // Right Click

                        locations.add(event.getClickedBlock().getLocation()); // Right click
                        player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.GREEN + "Second corner set (" + event.getClickedBlock().getX() + "," + event.getClickedBlock().getY() + "," + event.getClickedBlock().getLocation().getBlockZ() + ")");
                        playerLocationStorage.put(player.getName(), locations);

                    }
                    if (playerLocationStorage.containsKey(player.getName())) {
                        if ((!playerLocationStorage.get(player.getName()).get(0).equals(nullLocation)) &&
                                (!playerLocationStorage.get(player.getName()).get(1).equals(nullLocation))) {
                            player.sendMessage(ChatColor.RED + "[RPS] " + ChatColor.AQUA + "Both corners have been set. Hit an RPS to set its new range to the current cuboid area.");
                                }
                    }
                    event.setCancelled(true);
                }
            }
        }

	public boolean playerWithin(Location l1, Location l2, Location pLoc) {
		if (l1.equals(nullLocation) || l2.equals(nullLocation)) {
			return false;
		} else {
			return pLoc.getBlockX() >= l1.getBlockX() && pLoc.getBlockX() <= l2.getBlockX() && pLoc.getBlockY() >= l1.getBlockY() && pLoc.getBlockY() <= l2.getBlockY() && pLoc.getBlockZ() >= l1.getBlockZ() && pLoc.getBlockZ() <= l2.getBlockZ();
		}
	}

	@EventHandler
	public void RedEvent(BlockRedstoneEvent event) {
		Block blk = event.getBlock();
		for (RedstoneSensor.Torch torch : RedstoneSensor.torchList) {
			if(plugin.getServer().getWorld(torch.getWorld().getName()) == null)
				continue;
			if (blk.getLocation().equals(torch)) {
				event.setNewCurrent(event.getOldCurrent());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	// must be high priority
	public void RemoveRedstone(BlockBreakEvent event) throws IOException {
		Block blk = event.getBlock();
		if ((blk.getType() == Material.REDSTONE_TORCH_ON) || (blk.getType() == Material.REDSTONE_TORCH_OFF)) {
			Iterator<RedstoneSensor.Torch> it = RedstoneSensor.torchList.iterator();

			while (it.hasNext()) {
				RedstoneSensor.Torch torch = it.next();
				if(plugin.getServer().getWorld(torch.getWorld().getName()) == null) {
                    it.remove();
					continue;
                }
				if (blk.getLocation().equals(torch)) {
					it.remove();
					String setname = "Redstones." + torch.getIdentifier();
					plugin.getConfig().set(setname, null);
					plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));
					ItemStack rps = new ItemStack(Material.REDSTONE_TORCH_OFF, 1);
					ItemMeta rpsmeta = rps.getItemMeta();
					rpsmeta.setDisplayName(ChatColor.RED + RedstoneSensor.text);
					rps.setItemMeta(rpsmeta);
					blk.setType(Material.AIR);
					blk.getWorld().dropItemNaturally(blk.getLocation(), rps);
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public boolean ServerCommand(ServerCommandEvent event) throws IOException {
		String cmd = event.getCommand();
		if (cmd.startsWith("/")) {
			cmd = cmd.substring(1);
		}
		List<String> arguments = new ArrayList<String>(Arrays.asList(cmd.split(" ")));
		if (arguments.size() > 0) {
			if (arguments.get(0).matches("rps|redstonesensor|rsensor")) {
				if (arguments.size() == 1) {

					System.out.print("[RPS] " + "/rps [maxrange/defaultrange/onlyowner/reload]");
				}
				if (arguments.size() == 2) {
					if (("reload").equalsIgnoreCase(arguments.get(1))) {
						plugin.getServer().getPluginManager().disablePlugin(plugin);
						plugin.getServer().getPluginManager().enablePlugin(plugin);
						System.out.print("[RPS] " + "Reloaded!");

					} else if (("defaultrange").equalsIgnoreCase(arguments.get(1))) {
						System.out.print("[RPS] " + "/rps defaultrange <number>");
					} else if (("maxrange").equalsIgnoreCase(arguments.get(1))) {
						System.out.print("[RPS] " + "/rps maxrange <number>");
					} else if (("onlyowner").equalsIgnoreCase(arguments.get(1))) {
						System.out.print("[RPS] " + "/rps onlyowner <true/false>");
					}
				} else if (arguments.size() == 3) {
					if (("defaultrange").equalsIgnoreCase(arguments.get(1))) {
						RedstoneSensorPlugin.defaultRange = Integer.valueOf(arguments.get(2));
						System.out.print("[RPS] " + "Default range set to " + String.valueOf(Integer.valueOf(arguments.get(2))));
						plugin.getConfig().set("Config.default-range", RedstoneSensorPlugin.defaultRange);
						plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));
					} else if (("maxrange").equalsIgnoreCase(arguments.get(1))) {
						RedstoneSensorPlugin.maxRange = Integer.valueOf(arguments.get(2));
						System.out.print("[RPS] " + "Max range set to " + String.valueOf(Integer.valueOf(arguments.get(2))));
						plugin.getConfig().set("Config.max-range", RedstoneSensorPlugin.maxRange);
						plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));

					} else if (("onlyowner").equalsIgnoreCase(arguments.get(1))) {
						RedstoneSensorPlugin.onlyOwner = Boolean.valueOf(arguments.get(2));
						System.out.print("[RPS] " + "Only Owner Can Change Range set to " + String.valueOf(Boolean.valueOf(arguments.get(2))));
						plugin.getConfig().set("Config.owner-only-change-range", RedstoneSensorPlugin.onlyOwner);
						plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));

					}
				}
			}
		}
		return true;
	}
}
