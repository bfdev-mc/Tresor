package de.tresor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class TresorPlugin extends JavaPlugin implements Listener {
    /** Wie lange ein eingegebener Code gueltig bleibt (zum Oeffnen/Abbauen). */
    static final long UNLOCK_MILLIS = 2 * 60 * 1000L;

    private VaultManager manager;
    private NamespacedKey smallRecipe;
    private NamespacedKey largeRecipe;

    @Override
    public void onEnable() {
        manager = new VaultManager(this);
        manager.load();
        registerRecipes();

        getServer().getPluginManager().registerEvents(new TresorListener(this, manager), this);
        getServer().getPluginManager().registerEvents(this, this);

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
        e.getPlayer().discoverRecipes(java.util.List.of(smallRecipe, largeRecipe));
    }
}
