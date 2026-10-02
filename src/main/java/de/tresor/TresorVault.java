package de.tresor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Ein Tresor (1 Block = klein, 2 Bloecke = gross). */
final class TresorVault {
    final UUID id;
    final boolean large;
    /** Bloecke im Format "welt;x;y;z". */
    final List<String> blocks;
    /** Seite, auf der die Tuer sitzt. */
    org.bukkit.block.BlockFace front = org.bukkit.block.BlockFace.SOUTH;
    String salt;
    String hash;
    ItemStack[] stored;
    Inventory inventory;

    final Map<UUID, Long> unlocked = new HashMap<>();
    final Map<UUID, Integer> failures = new HashMap<>();
    final Map<UUID, Long> lockedUntil = new HashMap<>();

    TresorVault(UUID id, boolean large, List<String> blocks) {
        this.id = id;
        this.large = large;
        this.blocks = blocks;
    }

    int size() {
        return large ? 54 : 27;
    }

    boolean pending() {
        return hash == null;
    }

    boolean isUnlocked(UUID player) {
        Long until = unlocked.get(player);
        if (until == null) return false;
        if (until < System.currentTimeMillis()) {
            unlocked.remove(player);
            return false;
        }
        return true;
    }

    void unlock(UUID player) {
        unlocked.put(player, System.currentTimeMillis() + TresorPlugin.UNLOCK_MILLIS);
        failures.remove(player);
    }
}
