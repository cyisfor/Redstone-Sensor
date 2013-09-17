package com.ugleh.redstonesensor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
//import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;


// plugin configuration and info go here.

public class RedstoneSensorPlugin extends JavaPlugin {
    public static Integer defaultRange = null;
    public static Integer maxRange = null;
    public static Boolean onlyOwner = null;
    public static Boolean outdated = false;

    private String currentVersion = "2.1.0";

    private String readurl = "https://raw.github.com/Ugleh/RedstoneSensor/master/version.txt";
    public static Boolean updatechecker;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new RedstoneSensorListener(this), this);
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
        FileConfiguration config = getConfig();
        // Basic config settings below to make sure they have certian configs
        // and if they dont then add them.
        maxRange = config.getInt("Config.max-range");
        defaultRange = config.getInt("Config.default-range");
        onlyOwner = config.getBoolean("Config.owner-only-change-range");
        RedstoneSensor.text = config.getString("Config.proximity-sensor-name");
        RedstoneSensor.notText = config.getString("Config.not-proximity-sensor-name");
        RedstoneSensor.notifyText = config.getString("Config.proximity-range-notify-text");
        updatechecker = config.getBoolean("Config.update-checker");
        ArrayList<String> keys = new ArrayList<String>();
        keys.addAll(config.getConfigurationSection("Config").getKeys(false));
        if (!keys.contains("update-checker")) {
            updatechecker = true;
            config.set("Config.update-checker", true);
        }
        if (!keys.contains("max-range")) {
            maxRange = 10;
            config.set("Config.max-range", 10);
        }
        if (!keys.contains("owner-only-change-rank")) {
            onlyOwner = true;
            config.set("Config.owner-only-change-range", true);
        }
        if (!keys.contains("default-range")) {
            defaultRange = 3;
            config.set("Config.default-range", 3);
        }
        if (!keys.contains("proximity-sensor-name")) {
            RedstoneSensor.text = "Redstone Proximity Sensor";
            config.set("Config.proximity-sensor-name", "Redstone Proximity Sensor");
        }
        if (!keys.contains("not-proximity-sensor-name")) {
            RedstoneSensor.notText = "NOT Redstone Proximity Sensor";
            config.set("Config.not-proximity-sensor-name", "NOT Redstone Proximity Sensor");
        }
        if (!keys.contains("proximity-range-notify-text")) {
            RedstoneSensor.notifyText = "Proximity Range";
            config.set("Config.proximity-range-notify-text", "Proximity Range");
        }
        try {
            config.save(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // load previously saved torches
        for (String key : config.getConfigurationSection("Redstones").getKeys(false)) {
                String[] customrange = String.valueOf(config.getString("Redstones." + key + ".CustomRange")).split(";");
                Location l1 = null;
                Location l2 = null;
                int value;
                if (customrange.length != 2) {
                    value = config.getInt("Redstones." + key + ".Range");
                } else {
                    value = -999;
                    String[] coords = customrange[0].split("|");
                    // world, x, y, z
                    World world = getServer().getWorld(coords[0]);
                    l1 = new Location(world,Integer.valueOf(coords[1]),Integer.valueOf(coords[2]),Integer.valueOf(coords[3]));
                    l2 = new Location(world,Integer.valueOf(coords[4]),Integer.valueOf(coords[5]),Integer.valueOf(coords[6]));
                }
                String[] location =  String.valueOf(config.getString("Redstones." + key + ".Location")).split("|");
                World world = getServer().getWorld(location[0]);
                RedstoneSensor.torchList.add(new RedstoneSensor.Torch(
                            "NOT".equalsIgnoreCase(config.getString("Redstones." + key + ".Type")),
                            value,
                            config.getString("Redstones." + key + ".Owner"),
                            new Location(world,Integer.valueOf(location[1]),Integer.valueOf(location[2]),Integer.valueOf(location[3])),
                            l1,
                            l2));
        }

        // Crafting Recipe for the regular Proximity Sensor
        ItemStack rps = new ItemStack(Material.REDSTONE_TORCH_OFF, 1);
        ItemMeta rpsmeta = rps.getItemMeta();
        rpsmeta.setDisplayName(ChatColor.RED + RedstoneSensor.text);
        rps.setItemMeta(rpsmeta);
        ShapedRecipe rpsRecipe = new ShapedRecipe(rps);
        rpsRecipe.shape(" R ", " R ", " R ");
        rpsRecipe.setIngredient('R', Material.REDSTONE_TORCH_ON);
        this.getServer().addRecipe(rpsRecipe);

        // Crafting Recipe for the Inverted or NOT Proximity Sensor.
        ItemStack rps2 = new ItemStack(Material.REDSTONE_TORCH_OFF, 1);
        ItemMeta rpsmeta2 = rps2.getItemMeta();
        rpsmeta2.setDisplayName(ChatColor.RED + RedstoneSensor.notText);
        rps2.setItemMeta(rpsmeta2);
        ShapedRecipe rpsRecipe2 = new ShapedRecipe(rps2);
        rpsRecipe2.shape("   ", "RRR", "   ");
        rpsRecipe2.setIngredient('R', Material.REDSTONE_TORCH_ON);
        this.getServer().addRecipe(rpsRecipe2);

        // Crafting Recipe for the Custom Proximity Wand
        ItemStack wand = new ItemStack(Material.STICK, 1);
        ItemMeta wandmeta = wand.getItemMeta();
        wandmeta.setDisplayName(ChatColor.RED + "Redstone Proximity Wand");
        ShapedRecipe wandrecipe = new ShapedRecipe(wand);
        wandrecipe.shape("   ", " S ", " R ");
        wandrecipe.setIngredient('R', Material.REDSTONE_TORCH_ON);
        wandrecipe.setIngredient('S', Material.STICK);
        this.getServer().addRecipe(wandrecipe);

        if(updatechecker) {
            // if we didn't check updates, we don't know if it's outdated.
            // so, don't be annoying!
            outdated = true;
            startUpdateCheck();
            if (outdated) {
                Bukkit.broadcastMessage("[RPS] " + "Your version of Redstone Proximity Sensor is outdated");
            }
        } /* else {
            Bukkit.broadcastMessage("[RPS] Waaah you don't love me anymore");
        }*/
    }

    public void startUpdateCheck() {
        try {
            URL url = new URL(readurl);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = br.readLine()) != null) {
                if (str.startsWith(currentVersion)) {
                    outdated = false;
                }
            }
            br.close();
        } catch (IOException e) {
            Bukkit.broadcastMessage("The Update Checker URL is Invalid. Please let Ugleh know.");
        }
    }
}
