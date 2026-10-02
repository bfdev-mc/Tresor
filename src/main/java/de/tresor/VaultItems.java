package de.tresor;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

final class VaultItems {
    private VaultItems() {}

    static NamespacedKey typeKey(TresorPlugin plugin) {
        return new NamespacedKey(plugin, "type");
    }

    static ItemStack create(TresorPlugin plugin, boolean large) {
        ItemStack item = new ItemStack(Material.VAULT);
        ItemMeta meta = item.getItemMeta();
        meta.customName(Component.text(large ? "Großer Tresor" : "Tresor", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(large ? "Platz wie eine Doppelkiste" : "Platz wie eine Kiste",
                        NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Beim Platzieren wird ein Code festgelegt.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        meta.setItemModel(new NamespacedKey("tresor", large ? "safe_large" : "safe_small"));
        meta.getPersistentDataContainer().set(typeKey(plugin), PersistentDataType.STRING, large ? "large" : "small");
        item.setItemMeta(meta);
        return item;
    }

    /** @return null wenn kein Tresor-Item, sonst true = gross. */
    static Boolean typeOf(TresorPlugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.VAULT || !item.hasItemMeta()) return null;
        String type = item.getItemMeta().getPersistentDataContainer().get(typeKey(plugin), PersistentDataType.STRING);
        if (type == null) return null;
        return type.equals("large");
    }

    /** Button im letzten Slot des geoeffneten Tresors. */
    static ItemStack lockButton() {
        ItemStack item = new ItemStack(Material.IRON_DOOR);
        ItemMeta meta = item.getItemMeta();
        meta.customName(Component.text("Abschließen", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Sperrt den Tresor wieder zu.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.setItemModel(new NamespacedKey("tresor", "lock"));
        item.setItemMeta(meta);
        return item;
    }
}
