package de.tresor;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Das sichtbare Tresor-Modell: ein ItemDisplay mit custom Item-Model ueber einem Eisenblock.
 * Ohne Resource Pack sieht man nur den Eisenblock.
 */
final class VaultDisplays {
    private final TresorPlugin plugin;
    private final VaultManager manager;
    private final NamespacedKey marker;

    VaultDisplays(TresorPlugin plugin, VaultManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.marker = new NamespacedKey(plugin, "display");
    }

    boolean isOurs(Entity e) {
        return e instanceof ItemDisplay && e.getPersistentDataContainer().has(marker, PersistentDataType.BYTE);
    }

    private static float yawOf(BlockFace f) {
        return switch (f) {
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
    }

    private static String modelFor(TresorVault v, int index) {
        return v.large ? "safe_large" : "safe_small";
    }

    private static Block blockOf(String key, org.bukkit.Server server) {
        String[] s = key.split(";");
        World w = server.getWorld(s[0]);
        if (w == null) return null;
        return w.getBlockAt(Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3]));
    }

    private ItemDisplay find(Block b) {
        Location c = b.getLocation().add(0.5, 0.5, 0.5);
        for (Entity e : b.getWorld().getNearbyEntities(c, 0.3, 0.3, 0.3)) {
            if (isOurs(e)) return (ItemDisplay) e;
        }
        return null;
    }

    /** Stellt sicher, dass jeder Block des Tresors sein Modell hat (idempotent). */
    void ensure(TresorVault v) {
        for (int i = 0; i < v.blocks.size(); i++) {
            Block b = blockOf(v.blocks.get(i), plugin.getServer());
            if (b == null || !b.getWorld().isChunkLoaded(b.getX() >> 4, b.getZ() >> 4)) continue;
            if (find(b) != null) continue;

            Location loc = b.getLocation().add(0.5, 0.5, 0.5);
            loc.setYaw(yawOf(v.front));
            String model = modelFor(v, i);
            b.getWorld().spawn(loc, ItemDisplay.class, d -> {
                ItemStack stack = new ItemStack(Material.PAPER);
                ItemMeta meta = stack.getItemMeta();
                meta.setItemModel(new NamespacedKey("tresor", model));
                stack.setItemMeta(meta);
                d.setItemStack(stack);
                // Leicht groesser als der Block darunter, damit nichts flackert
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                        new Vector3f(1.002f), new AxisAngle4f()));
                // Im Eisenblock waere es sonst stockdunkel
                d.setBrightness(new Display.Brightness(15, 15));
                d.setPersistent(true);
                d.setInvulnerable(true);
                d.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
            });
        }
    }

    void remove(TresorVault v) {
        for (String key : v.blocks) {
            Block b = blockOf(key, plugin.getServer());
            if (b == null) continue;
            ItemDisplay d;
            while ((d = find(b)) != null) d.remove();
        }
    }

    /** Beim Laden eines Chunks: fehlende Modelle erzeugen, verwaiste entfernen. */
    void refreshChunk(Chunk chunk) {
        for (Entity e : chunk.getEntities()) {
            if (!isOurs(e)) continue;
            if (manager.at(e.getLocation().getBlock()) == null) e.remove();
        }
        manager.inChunk(chunk).forEach(this::ensure);
    }
}
