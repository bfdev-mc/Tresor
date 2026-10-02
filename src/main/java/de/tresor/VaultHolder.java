package de.tresor;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class VaultHolder implements InventoryHolder {
    final TresorVault vault;

    VaultHolder(TresorVault vault) {
        this.vault = vault;
    }

    @Override
    public Inventory getInventory() {
        return vault.inventory;
    }
}
