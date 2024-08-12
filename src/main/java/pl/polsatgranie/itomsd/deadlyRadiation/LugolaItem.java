package pl.polsatgranie.itomsd.deadlyRadiation;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class LugolaItem {

    public static ItemStack createLugola() {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta2 = item.getItemMeta();
        meta2.setDisplayName(DeadlyRadiation.lugola_name);
        meta2.setLore(DeadlyRadiation.lugola_lore);
        item.setItemMeta(meta2);
        return item;
    }
}
