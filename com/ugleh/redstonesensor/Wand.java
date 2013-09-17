package com.ugleh.redstonesensor;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.Material;

class Wand {
    static ItemStack create() {
        ItemStack rps2 = new ItemStack(Material.STICK, 1);
        ItemMeta rpsmeta2 = rps2.getItemMeta();
        rpsmeta2.setDisplayName(ChatColor.RED + "RPS Wand");
        rps2.setItemMeta(rpsmeta2);
        return rps2;
    }
    static boolean check(ItemStack stack) {
	    return (ChatColor.RED + "RPS Wand").equals(stack.getItemMeta().getDisplayName());
    }

}
