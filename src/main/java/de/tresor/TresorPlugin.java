package de.tresor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;

public final class TresorPlugin extends JavaPlugin implements Listener {
    /** Wie lange nach korrekter Code-Eingabe abgebaut werden darf. */
    static final long UNLOCK_MILLIS = 15 * 1000L;
    /** Feste Pack-ID, damit ein neues Pack das alte ersetzt. */
    private static final UUID PACK_ID = UUID.fromString("7a1f0c2e-5b3d-4e6a-9c88-0d7e2f4b1a11");

    private VaultManager manager;
    private VaultDisplays displays;
    private final Set<UUID> packLoaded = new HashSet<>();
    private NamespacedKey smallRecipe;
    private NamespacedKey largeRecipe;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new VaultManager(this);
        manager.load();
        displays = new VaultDisplays(this, manager);
        registerRecipes();

        getServer().getPluginManager().registerEvents(new TresorListener(this, manager, displays, packLoaded), this);
        getServer().getPluginManager().registerEvents(this, this);

        // Modelle fuer bereits geladene Chunks nachziehen
        getServer().getWorlds().forEach(w -> {
            for (Chunk c : w.getLoadedChunks()) displays.refreshChunk(c);
        });

        // Autosave alle 5 Minuten
        getServer().getScheduler().runTaskTimer(this, manager::save, 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.save();
    }

    private void registerRecipes() {
        smallRecipe = new NamespacedKey(this, "tresor_klein");
        largeRecipe = new NamespacedKey(this, "tresor_gross");

        // Kleiner Tresor: Kiste + Eisenblock
        ShapelessRecipe small = new ShapelessRecipe(smallRecipe, VaultItems.create(this, false));
        small.addIngredient(Material.CHEST);
        small.addIngredient(Material.IRON_BLOCK);
        getServer().addRecipe(small);

        // Grosser Tresor: wie eine Kiste, aber mit Eisenbloecken statt Holz
        ShapedRecipe large = new ShapedRecipe(largeRecipe, VaultItems.create(this, true));
        large.shape("III", "I I", "III");
        large.setIngredient('I', Material.IRON_BLOCK);
        getServer().addRecipe(large);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.getPlayer().discoverRecipes(List.of(smallRecipe, largeRecipe));

        String url = getConfig().getString("resource-pack.url", "");
        String sha1 = getConfig().getString("resource-pack.sha1", "");
        if (getConfig().getBoolean("resource-pack.send", true) && !url.isBlank() && !sha1.isBlank()) {
            e.getPlayer().setResourcePack(PACK_ID, url, sha1, Component.text("Tresor-Design (Tresor-Block und Zahlenfeld)"),
                    getConfig().getBoolean("resource-pack.required", false));
        }
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent e) {
        if (!PACK_ID.equals(e.getID())) return;
        if (e.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            packLoaded.add(e.getPlayer().getUniqueId());
        } else if (e.getStatus() != PlayerResourcePackStatusEvent.Status.ACCEPTED
                && e.getStatus() != PlayerResourcePackStatusEvent.Status.DOWNLOADED) {
            packLoaded.remove(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        packLoaded.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        Chunk chunk = e.getChunk();
        getServer().getScheduler().runTask(this, () -> {
            if (chunk.isLoaded()) displays.refreshChunk(chunk);
        });
    }
}
