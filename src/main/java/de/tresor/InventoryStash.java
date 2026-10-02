package de.tresor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Blendet waehrend des Zahlenfelds das Spielerinventar aus, indem es kurz weggelegt wird.
 * Vor dem Leeren wird alles auf die Platte geschrieben, damit nach einem Absturz nichts verloren geht
 * (beim naechsten Beitritt wird wiederhergestellt).
 */
final class InventoryStash {
    private final TresorPlugin plugin;
    private final File dir;
    private final Map<UUID, ItemStack[]> hidden = new HashMap<>();

    InventoryStash(TresorPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "stash");
    }

    private File file(UUID id) {
        return new File(dir, id + ".yml");
    }

    boolean isHidden(UUID id) {
        return hidden.containsKey(id);
    }

    void hide(Player p) {
        UUID id = p.getUniqueId();
        if (hidden.containsKey(id)) return;
        PlayerInventory inv = p.getInventory();
        ItemStack[] items = inv.getStorageContents().clone();

        YamlConfiguration yml = new YamlConfiguration();
        List<String> encoded = new ArrayList<>();
        for (ItemStack it : items) {
            encoded.add(it == null || it.isEmpty() ? "" : Base64.getEncoder().encodeToString(it.serializeAsBytes()));
        }
        yml.set("items", encoded);
        try {
            dir.mkdirs();
            yml.save(file(id));
        } catch (IOException e) {
            // Ohne sichere Kopie wird nichts geleert
            plugin.getLogger().log(Level.WARNING, "Inventar-Sicherung fehlgeschlagen, Inventar bleibt sichtbar", e);
            return;
        }
        hidden.put(id, items);
        inv.setStorageContents(new ItemStack[items.length]);
        p.updateInventory();
    }

    /** Entfernt die gesicherten Items (aus Speicher oder Datei) und gibt sie zurueck, oder null. */
    ItemStack[] take(UUID id) {
        ItemStack[] items = hidden.remove(id);
        File f = file(id);
        if (items == null && f.exists()) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
            List<String> encoded = yml.getStringList("items");
            items = new ItemStack[encoded.size()];
            for (int i = 0; i < items.length; i++) {
                String b64 = encoded.get(i);
                if (!b64.isEmpty()) items[i] = ItemStack.deserializeBytes(Base64.getDecoder().decode(b64));
            }
        }
        if (f.exists() && !f.delete()) f.deleteOnExit();
        return items;
    }

    void restore(Player p) {
        ItemStack[] saved = take(p.getUniqueId());
        if (saved == null) return;
        PlayerInventory inv = p.getInventory();
        ItemStack[] current = inv.getStorageContents(); // z.B. waehrenddessen aufgesammelt
        inv.setStorageContents(java.util.Arrays.copyOf(saved, inv.getStorageContents().length));
        for (ItemStack it : current) {
            if (it == null || it.isEmpty()) continue;
            inv.addItem(it).values().forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
        }
        p.updateInventory();
    }

    void restoreAll() {
        for (UUID id : new ArrayList<>(hidden.keySet())) {
            Player p = plugin.getServer().getPlayer(id);
            if (p != null) restore(p);
        }
    }
}
