package de.tresor;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Zahlenfeld-GUI zum Setzen bzw. Eingeben des Codes.
 * Mit geladenem Resource Pack wird der Hintergrund per Titel-Glyph gezeichnet (custom = true).
 */
final class KeypadHolder implements InventoryHolder {
    static final int MIN_LENGTH = 4;
    static final int MAX_LENGTH = 9;
    static final int CLEAR = 30, OK = 32;
    /** Ziffern 1-9, dann 0 - in zwei Reihen zu je fuenf Tasten. */
    static final int[] DIGIT_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
    static final int[] DIGITS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};

    final TresorVault vault;
    final boolean setMode;
    final boolean custom;
    final StringBuilder entry = new StringBuilder();
    boolean done;
    private final Inventory inventory;

    KeypadHolder(TresorVault vault, boolean setMode, boolean custom) {
        this.vault = vault;
        this.setMode = setMode;
        this.custom = custom;

        Component title;
        if (custom) {
            title = Component.text("", NamedTextColor.WHITE)
                    .font(Key.key("tresor", "gui"))
                    .shadowColor(ShadowColor.none());
        } else {
            title = Component.text(setMode ? "Code festlegen" : "Code eingeben");
        }
        this.inventory = Bukkit.createInventory(this, 36, title);

        if (!custom) {
            ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", null, 1);
            for (int i = 0; i < 36; i++) inventory.setItem(i, filler);
        }
        for (int i = 0; i < DIGIT_SLOTS.length; i++) {
            int d = DIGITS[i];
            inventory.setItem(DIGIT_SLOTS[i], item(Material.LIGHT_GRAY_CONCRETE, String.valueOf(d), "key_" + d, Math.max(d, 1)));
        }
        inventory.setItem(CLEAR, item(Material.RED_CONCRETE, "Löschen", "key_clear", 1));
        inventory.setItem(OK, item(Material.LIME_CONCRETE, "Bestätigen", "key_ok", 1));
        render();
    }

    /** @return die Ziffer des Slots oder -1. */
    static int digitOf(int slot) {
        for (int i = 0; i < DIGIT_SLOTS.length; i++) if (DIGIT_SLOTS[i] == slot) return DIGITS[i];
        return -1;
    }

    /** Zeichnet die obere Anzeige-Reihe: ein leuchtender Punkt pro eingegebener Ziffer. */
    void render() {
        for (int i = 0; i < 9; i++) {
            boolean on = i < entry.length();
            ItemStack lcd = item(on ? Material.LIME_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE,
                    on ? "*" : " ", on ? "lcd_on" : "lcd_off", 1);
            ItemMeta meta = lcd.getItemMeta();
            meta.setHideTooltip(true);
            lcd.setItemMeta(meta);
            inventory.setItem(i, lcd);
        }
    }

    private static ItemStack item(Material m, String name, String model, int amount) {
        ItemStack it = new ItemStack(m, amount);
        ItemMeta meta = it.getItemMeta();
        meta.customName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        if (model != null) meta.setItemModel(new NamespacedKey("tresor", model));
        it.setItemMeta(meta);
        return it;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
