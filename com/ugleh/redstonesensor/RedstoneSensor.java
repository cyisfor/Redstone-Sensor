package com.ugleh.redstonesensor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.Block;

public class RedstoneSensor {
    static enum Type {
        NADA,
        NORMAL,
        NOT
    };

    public static String notifyText = null;
    public static String notText = null;
    public static String text = null;


    static Type check(ItemStack item) {
        String name = item.getItemMeta().getDisplayName();
        if ((ChatColor.RED + text).equals(name))
            return Type.NORMAL;
        if ((ChatColor.RED + notText).equals(name))
            return Type.NOT;
        return Type.NADA;
    }

    public static class Torch extends Location, implements Comparable {
        public final int value;
        public final String owner;
        public final Location l1;
        public final Location l2;
        public final boolean isNot;

        public Torch(boolean isNot, int value, String owner, Location location, Location l1, Location l2) {
            super(location.getWorld(),location.getX(),location.getY(),location.getZ());
            this.isNot = isNot;
            this.value = value;
            this.owner = owner;
            this.l1 = l1;
            this.l2 = l2;
        }
        public Torch(boolean isNot, int value, String owner, Location location) {
            this(isNot,value,owner,location,null,null);
        }

        // this is still a hackish way to do things...
        public Torch(Location loc) {
            this(false,-1,null,loc);
        }
        public Torch(Block blk) {
            this(false,-1,null,blk.getLocation());
        }

        public int hashCode() {
            return getBlockX() * 1000000 + getBlockY() * 1000 + getBlockZ();
        }
        public boolean equals(Object other) {
            if(other instanceof Torch) {
                Torch loc = (Torch) other;
                return loc.getBlockX() == getBlockX() &&
                    loc.getBlockY() == getBlockY() &&
                    loc.getBlockZ() == getBlockZ() &&
                    loc.getWorld().getName() == getWorld().getName();
            }

            return false;
        }
        public int compareTo(Object derp) {
            if(derp instanceof Torch) {
                Torch other = (Torch) derp;
                int ret = other.getWorld().getName().compareTo(other.getWorld().getName());
                if(ret == 0) {
                    ret = other.getBlockX() - getBlockX()+
                        other.getBlockY() - getBlockY()+
                        other.getBlockZ() - getBlockZ();
                }
                return ret;
            }
            return 1;
        }

        String identifier;
        public String getIdentifier() {
            if(identifier == null) {
                identifier = getWorld().getName()+'|'+getBlockX()+'|'+getBlockY()+'|'+getBlockZ();
            }
            return identifier;
        }
    }

    // XXX: MUST be a treeset for TreeSet.ceiling(...)
    public static TreeSet<Torch> torchList = new TreeSet<Torch>();

    public static boolean playerWithin(Location l1, Location l2, Location pLoc) {
        int x1 = Math.min(l1.getBlockX(), l2.getBlockX());
        int y1 = Math.min(l1.getBlockY(), l2.getBlockY());
        int z1 = Math.min(l1.getBlockZ(), l2.getBlockZ());
        int x2 = Math.max(l1.getBlockX(), l2.getBlockX());
        int y2 = Math.max(l1.getBlockY(), l2.getBlockY());
        int z2 = Math.max(l1.getBlockZ(), l2.getBlockZ());
        l1 = new Location(l1.getWorld(), x1, y1, z1);
        l2 = new Location(l2.getWorld(), x2, y2, z2);

        return pLoc.getBlockX() >= l1.getBlockX() && pLoc.getBlockX() <= l2.getBlockX() && pLoc.getBlockY() >= l1.getBlockY() && pLoc.getBlockY() <= l2.getBlockY() && pLoc.getBlockZ() >= l1.getBlockZ() && pLoc.getBlockZ() <= l2.getBlockZ();
    }

}
