package de.tresor;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/** Zahlenfeld-GUI zum Setzen bzw. Eingeben des Codes. */
final class KeypadHolder implements InventoryHolder {
    static final int MIN_LENGTH = 4;
    static final int MAX_LENGTH = 8;
    static final int DISPLAY = 4, CLEAR = 39, ZERO = 40, OK = 41;
    static final int[] DIGIT_SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32};

    final TresorVault vault;
    final boolean setMode;
    final StringBuilder entry = new StringBuilder();
    boolean done;
    private final Inventory inventory;

    KeypadHolder(TresorVault vault, boolean setMode) {
        this.vault = vault;
        this.setMode = setMode;
        this.inventory = Bukkit.createInventory(this, 45,
                Component.text(setMode ? "Code festlegen" : "Code eingeben"));
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", 1);
        for (int i = 0; i < 45; i++) inventory.setItem(i, filler);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(DIGIT_SLOTS[i], item(Material.LIGHT_GRAY_CONCRETE, String.valueOf(i + 1), i + 1));
        }
        inventory.setItem(ZERO, item(Material.LIGHT_GRAY_CONCRETE, "0", 1));
        inventory.setItem(CLEAR, item(Material.RED_CONCRETE, "Löschen", 1));
        inventory.setItem(OK, item(Material.LIME_CONCRETE, "Bestätigen", 1));
        render();
    }

    /** @return die Ziffer des Slots oder -1. */
    static int digitOf(int slot) {
        if (slot == ZERO) return 0;
        for (int i = 0; i < 9; i++) if (DIGIT_SLOTS[i] == slot) return i + 1;
        return -1;
    }

    void render() {
        String shown = entry.length() == 0 ? "-" : "*".repeat(entry.length());
        ItemStack display = item(Material.OAK_SIGN, "Code: " + shown, 1);
        ItemMeta meta = display.getItemMeta();
        meta.lore(List.of(
                lore(MIN_LENGTH + "-" + MAX_LENGTH + " Ziffern"),
                lore(setMode ? "Merke dir den Code gut!" : "Gib den Code des Tresors ein")));
        display.setItemMeta(meta);
        inventory.setItem(DISPLAY, display);
    }

    private static Component lore(String s) {
        return Component.text(s, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack item(Material m, String name, int amount) {
        ItemStack it = new ItemStack(m, amount);
        ItemMeta meta = it.getItemMeta();
        meta.customName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        it.setItemMeta(meta);
        return it;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
