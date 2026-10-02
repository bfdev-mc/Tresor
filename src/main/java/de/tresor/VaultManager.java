package de.tresor;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

final class VaultManager {
    private final TresorPlugin plugin;
    private final File file;
    private final Map<UUID, TresorVault> vaults = new HashMap<>();
    private final Map<String, TresorVault> byBlock = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    VaultManager(TresorPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tresore.yml");
    }

    static String key(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    TresorVault at(Block b) {
        return byBlock.get(key(b));
    }

    boolean isVault(Block b) {
        return byBlock.containsKey(key(b));
    }

    void register(TresorVault v) {
        vaults.put(v.id, v);
        for (String k : v.blocks) byBlock.put(k, v);
    }

    void unregister(TresorVault v) {
        vaults.remove(v.id);
        for (String k : v.blocks) byBlock.remove(k);
    }

    // ---- Code ----

    void setCode(TresorVault v, String code) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        v.salt = Base64.getEncoder().encodeToString(salt);
        v.hash = hash(v.salt, code);
    }

    boolean checkCode(TresorVault v, String code) {
        return MessageDigest.isEqual(hash(v.salt, code).getBytes(), v.hash.getBytes());
    }

    private static String hash(String salt, String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            return Base64.getEncoder().encodeToString(md.digest(code.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- Inventar ----

    Inventory inventory(TresorVault v) {
        if (v.inventory == null) {
            v.inventory = Bukkit.createInventory(new VaultHolder(v), v.size(),
                    Component.text(v.large ? "Großer Tresor" : "Tresor"));
            if (v.stored != null) {
                v.inventory.setContents(Arrays.copyOf(v.stored, v.size()));
            }
        }
        return v.inventory;
    }

    ItemStack[] contents(TresorVault v) {
        if (v.inventory != null) return v.inventory.getContents();
        return v.stored == null ? new ItemStack[0] : v.stored;
    }

    // ---- Speichern / Laden ----

    void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (TresorVault v : vaults.values()) {
            if (v.pending()) continue;
            String p = "vaults." + v.id;
            yml.set(p + ".large", v.large);
            yml.set(p + ".blocks", v.blocks);
            yml.set(p + ".salt", v.salt);
            yml.set(p + ".hash", v.hash);
            List<String> items = new ArrayList<>();
            for (ItemStack it : contents(v)) {
                items.add(it == null || it.isEmpty() ? ""
                        : Base64.getEncoder().encodeToString(it.serializeAsBytes()));
            }
            yml.set(p + ".items", items);
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Tresore konnten nicht gespeichert werden", e);
        }
    }

    void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yml.getConfigurationSection("vaults");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try {
                TresorVault v = new TresorVault(UUID.fromString(id), s.getBoolean("large"),
                        new ArrayList<>(s.getStringList("blocks")));
                v.salt = s.getString("salt");
                v.hash = s.getString("hash");
                List<String> items = s.getStringList("items");
                ItemStack[] arr = new ItemStack[v.size()];
                for (int i = 0; i < items.size() && i < arr.length; i++) {
                    String b64 = items.get(i);
                    if (!b64.isEmpty()) arr[i] = ItemStack.deserializeBytes(Base64.getDecoder().decode(b64));
                }
                v.stored = arr;
                register(v);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Tresor " + id + " konnte nicht geladen werden", e);
            }
        }
        plugin.getLogger().info(vaults.size() + " Tresore geladen.");
    }
}
