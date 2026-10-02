package de.tresor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class TresorListener implements Listener {
    private static final int MAX_FAILURES = 5;
    private static final long LOCKOUT_MILLIS = 30_000L;

    private final TresorPlugin plugin;
    private final VaultManager manager;
    private final VaultDisplays displays;
    private final java.util.Set<UUID> packLoaded;

    TresorListener(TresorPlugin plugin, VaultManager manager, VaultDisplays displays, java.util.Set<UUID> packLoaded) {
        this.plugin = plugin;
        this.manager = manager;
        this.displays = displays;
        this.packLoaded = packLoaded;
    }

    private static void msg(Player p, String text, NamedTextColor color) {
        p.sendMessage(Component.text("[Tresor] ", NamedTextColor.DARK_GRAY).append(Component.text(text, color)));
    }

    // ------------------------------------------------------------ Platzieren

    private static BlockFace horizontalFacing(Player p) {
        float yaw = (p.getLocation().getYaw() % 360 + 360) % 360;
        if (yaw >= 315 || yaw < 45) return BlockFace.SOUTH;
        if (yaw < 135) return BlockFace.WEST;
        if (yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Boolean large = VaultItems.typeOf(plugin, e.getItemInHand());
        if (large == null) return;

        Player p = e.getPlayer();
        Block first = e.getBlockPlaced();
        BlockFace look = horizontalFacing(p);

        // Der Block selbst ist Eisen (Abbauzeit, Partikel, Sound); das Aussehen kommt vom ItemDisplay.
        first.setType(Material.IRON_BLOCK, false);

        List<String> keys = new ArrayList<>();
        keys.add(VaultManager.key(first));

        TresorVault vault = new TresorVault(UUID.randomUUID(), large, keys);
        vault.front = look.getOppositeFace();
        manager.register(vault);
        displays.ensure(vault);
        plugin.getServer().getScheduler().runTask(plugin, () -> showKeypad(p, new KeypadHolder(vault, KeypadHolder.Mode.SET, packLoaded.contains(p.getUniqueId()))));
    }

    /** Bricht das Platzieren ab, wenn kein Code gesetzt wurde. */
    private void cancelPending(TresorVault vault, Player p) {
        manager.unregister(vault);
        displays.remove(vault);
        removeBlocks(vault);
        if (p.getGameMode() != GameMode.CREATIVE) {
            giveBack(p, VaultItems.create(plugin, vault.large));
        }
        msg(p, "Platzieren abgebrochen - es wurde kein Code festgelegt.", NamedTextColor.RED);
    }

    private void giveBack(Player p, ItemStack item) {
        p.getInventory().addItem(item).values()
                .forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
    }

    private void removeBlocks(TresorVault vault) {
        for (String k : vault.blocks) {
            String[] s = k.split(";");
            org.bukkit.World w = plugin.getServer().getWorld(s[0]);
            if (w == null) continue;
            w.getBlockAt(Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3]))
                    .setType(Material.AIR, false);
        }
    }

    // ------------------------------------------------------------ Oeffnen

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Action action = e.getAction();
        if ((action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) || e.getClickedBlock() == null) return;
        TresorVault vault = manager.at(e.getClickedBlock());
        if (vault == null) return;

        Player p = e.getPlayer();
        boolean admin = p.hasPermission("tresor.admin");

        if (action == Action.LEFT_CLICK_BLOCK) {
            // Abbauen = Linksklick + richtiger Code (Mining selbst ist gesperrt, siehe onBreak)
            if (admin) return;
            e.setCancelled(true);
            if (vault.pending()) return;
            if (p.getOpenInventory().getType() == org.bukkit.event.inventory.InventoryType.CRAFTING) {
                openKeypad(p, vault, KeypadHolder.Mode.BREAK);
            }
            return;
        }

        // Sneaken mit Block in der Hand = normal platzieren, wie bei Kisten
        if (p.isSneaking() && !p.getInventory().getItemInMainHand().isEmpty()) return;

        e.setCancelled(true); // verhindert normale Block-Interaktion
        if (e.getHand() != EquipmentSlot.HAND || vault.pending()) return;

        if (admin) {
            openVault(p, vault);
            return;
        }
        openKeypad(p, vault, KeypadHolder.Mode.OPEN);
    }

    private void openKeypad(Player p, TresorVault vault, KeypadHolder.Mode mode) {
        Long lock = vault.lockedUntil.get(p.getUniqueId());
        if (lock != null && lock > System.currentTimeMillis()) {
            msg(p, "Zu viele Fehlversuche. Warte " + ((lock - System.currentTimeMillis()) / 1000 + 1) + " Sekunden.",
                    NamedTextColor.RED);
            return;
        }
        showKeypad(p, new KeypadHolder(vault, mode, packLoaded.contains(p.getUniqueId())));
    }

    private void showKeypad(Player p, KeypadHolder pad) {
        p.openInventory(pad.getInventory());
    }

    private void openVault(Player p, TresorVault vault) {
        p.openInventory(manager.inventory(vault));
        p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 0.8f, 1f);
    }

    // ------------------------------------------------------------ Zahlenfeld

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof KeypadHolder pad)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        int slot = e.getSlot();
        int digit = KeypadHolder.digitOf(slot);
        if (digit >= 0) {
            if (pad.entry.length() < KeypadHolder.MAX_LENGTH) pad.entry.append(digit);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            pad.render();
        } else if (slot == KeypadHolder.CLEAR) {
            pad.entry.setLength(0);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.8f);
            pad.render();
        } else if (slot == KeypadHolder.OK) {
            confirm(p, pad);
        }
    }

    private void confirm(Player p, KeypadHolder pad) {
        TresorVault vault = pad.vault;
        String code = pad.entry.toString();

        if (pad.setMode) {
            if (code.length() < KeypadHolder.MIN_LENGTH) {
                msg(p, "Der Code braucht mindestens " + KeypadHolder.MIN_LENGTH + " Ziffern.", NamedTextColor.RED);
                return;
            }
            manager.setCode(vault, code);
            pad.done = true;
            manager.save();
            p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
            msg(p, "Code gesetzt. Merke ihn dir - ohne Code kommt niemand mehr rein!", NamedTextColor.GREEN);
            plugin.getServer().getScheduler().runTask(plugin, () -> p.closeInventory());
            return;
        }

        UUID id = p.getUniqueId();
        if (manager.checkCode(vault, code)) {
            vault.failures.remove(id);
            pad.done = true;
            if (pad.mode == KeypadHolder.Mode.BREAK) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    p.closeInventory();
                    if (manager.at(blockOf(vault)) == vault) dismantle(vault, p);
                });
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> openVault(p, vault));
            }
        } else {
            int fails = vault.failures.merge(id, 1, Integer::sum);
            pad.entry.setLength(0);
            pad.render();
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            if (fails >= MAX_FAILURES) {
                vault.failures.remove(id);
                vault.lockedUntil.put(id, System.currentTimeMillis() + LOCKOUT_MILLIS);
                pad.done = true;
                msg(p, "Zu viele Fehlversuche. Gesperrt für 30 Sekunden.", NamedTextColor.RED);
                plugin.getServer().getScheduler().runTask(plugin, () -> p.closeInventory());
            } else {
                msg(p, "Falscher Code.", NamedTextColor.RED);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof KeypadHolder) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getHolder() instanceof KeypadHolder pad) {
            if (pad.setMode && !pad.done && e.getPlayer() instanceof Player p && pad.vault.pending()) {
                cancelPending(pad.vault, p);
            }
        } else if (inv.getHolder() instanceof VaultHolder holder) {
            if (e.getPlayer() instanceof Player p) {
                p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 0.8f, 1f);
            }
            manager.save();
        }
    }

    // ------------------------------------------------------------ Abbauen

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        TresorVault vault = manager.at(e.getBlock());
        if (vault == null) return;
        Player p = e.getPlayer();

        // Normales Abbauen gibt es nicht: nur per Linksklick + Code (oder Admin)
        e.setCancelled(true);
        if (p.hasPermission("tresor.admin") && !vault.pending()) {
            dismantle(vault, p);
            return;
        }
        if (!vault.pending()) {
            msg(p, "Zum Abbauen: Linksklick auf den Tresor und den Code eingeben.", NamedTextColor.RED);
        }
    }

    private Block blockOf(TresorVault vault) {
        String[] s = vault.blocks.get(0).split(";");
        return plugin.getServer().getWorld(s[0]).getBlockAt(Integer.parseInt(s[1]), Integer.parseInt(s[2]),
                Integer.parseInt(s[3]));
    }

    /** Baut den Tresor ab: Inhalt und Tresor-Item droppen wie bei einer Kiste. */
    private void dismantle(TresorVault vault, Player p) {
        if (vault.inventory != null) {
            for (org.bukkit.entity.HumanEntity viewer : new ArrayList<>(vault.inventory.getViewers())) {
                viewer.closeInventory();
            }
        }
        Location drop = blockOf(vault).getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack it : manager.contents(vault)) {
            if (it != null && !it.isEmpty()) drop.getWorld().dropItemNaturally(drop, it);
        }
        if (p.getGameMode() != GameMode.CREATIVE) {
            drop.getWorld().dropItemNaturally(drop, VaultItems.create(plugin, vault.large));
        }
        manager.unregister(vault);
        displays.remove(vault);
        removeBlocks(vault);
        manager.save();
        drop.getWorld().playSound(drop, Sound.BLOCK_IRON_DOOR_OPEN, 1f, 0.7f);
    }

    // ------------------------------------------------------------ Schutz

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(manager::isVault);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(manager::isVault);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(manager::isVault)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream().anyMatch(manager::isVault)) e.setCancelled(true);
    }
}
