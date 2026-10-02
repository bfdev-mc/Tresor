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
    static final int CLEAR = 39, OK = 41, DISPLAY_START = 0;
    /** Klassisches Ziffernfeld: 1-2-3 / 4-5-6 / 7-8-9 / C-0-OK. */
    static final int[] DIGIT_SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32, 40};
    static final int[] DIGITS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};

    final TresorVault vault;
    enum Mode { SET, OPEN, BREAK }

    final Mode mode;
    final boolean setMode;
    final boolean custom;
    final StringBuilder entry = new StringBuilder();
    boolean done;
    private final Inventory inventory;

    KeypadHolder(TresorVault vault, Mode mode, boolean custom) {
        this.vault = vault;
        this.mode = mode;
        this.setMode = mode == Mode.SET;
        this.custom = custom;

        Component title;
        if (custom) {
            title = Component.text("", NamedTextColor.WHITE)
                    .font(Key.key("tresor", "gui"))
                    .shadowColor(ShadowColor.none());
        } else {
            title = Component.text(setMode ? "Code festlegen" : mode == Mode.BREAK ? "Code zum Abbauen" : "Code eingeben");
        }
        this.inventory = Bukkit.createInventory(this, 45, title);

        if (!custom) {
            ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", null, 1);
            for (int i = 0; i < 45; i++) inventory.setItem(i, filler);
        }
        for (int i = 0; i < DIGIT_SLOTS.length; i++) {
            int d = DIGITS[i];
            inventory.setItem(DIGIT_SLOTS[i], item(Material.LIGHT_GRAY_CONCRETE, String.valueOf(d), "key_" + d, 1));
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
            if (!on) {
                inventory.setItem(DISPLAY_START + i, custom ? null : item(Material.BLACK_STAINED_GLASS_PANE, " ", null, 1));
                continue;
            }
            ItemStack lcd = item(Material.LIME_STAINED_GLASS_PANE, "*", "lcd_on", 1);
            ItemMeta meta = lcd.getItemMeta();
            meta.setHideTooltip(true);
            lcd.setItemMeta(meta);
            inventory.setItem(DISPLAY_START + i, lcd);
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
