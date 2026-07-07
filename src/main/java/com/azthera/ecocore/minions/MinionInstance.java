package com.azthera.ecocore.minions;
 
import com.azthera.ecocore.data.model.MinionData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
 
/**
 * Runtime wrapper around a placed minion's persistent {@link MinionData},
 * pairing it with a live, in-memory {@link Inventory} for its storage (since
 * inventories cannot be persisted directly — {@code MinionStorageManager}
 * handles (de)serialization to/from {@code MinionData#getSerializedStorage}).
 */
public final class MinionInstance {
 
    private final MinionData data;
    private final Inventory storageInventory;
 
    public MinionInstance(MinionData data, Inventory storageInventory) {
        this.data = data;
        this.storageInventory = storageInventory;
    }
 
    public MinionData getData() {
        return data;
    }
 
    public Inventory getStorageInventory() {
        return storageInventory;
    }
 
    public MinionType getType() {
        return MinionType.valueOf(data.getMinionType());
    }
 
    public Location resolveLocation(World world) {
        return new Location(world, data.getX(), data.getY(), data.getZ());
    }
 
    public boolean hasFreeStorageSlot() {
        for (ItemStack item : storageInventory.getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                return true;
            }
            if (item.getAmount() < item.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }
 
    public boolean tryAddItem(ItemStack item) {
        var leftover = storageInventory.addItem(item);
        return leftover.isEmpty();
    }
 
    public boolean hasFuel() {
        return data.getFuelRemaining() > 0;
    }
 
    public void consumeFuel(int amount) {
        data.setFuelRemaining(Math.max(0, data.getFuelRemaining() - amount));
    }
 
    public void addFuel(int amount) {
        data.setFuelRemaining(data.getFuelRemaining() + amount);
    }
}
