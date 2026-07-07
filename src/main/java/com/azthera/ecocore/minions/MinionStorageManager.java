package com.azthera.ecocore.minions;
 
import com.azthera.ecocore.config.MinionsConfig;
import com.azthera.ecocore.data.model.MinionData;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
 
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
 
/**
 * Handles serialization of a minion's storage {@link Inventory} contents
 * to/from the byte array persisted in {@link MinionData#getSerializedStorage()},
 * using Bukkit's own object stream format so any ItemStack (including
 * enchanted/NBT-bearing items) round-trips correctly.
 */
public final class MinionStorageManager {
 
    private final Logger logger;
    private MinionsConfig minionsConfig;
 
    public MinionStorageManager(MinionsConfig minionsConfig, Logger logger) {
        this.minionsConfig = minionsConfig;
        this.logger = logger;
    }
 
    public void reload(MinionsConfig minionsConfig) {
        this.minionsConfig = minionsConfig;
    }
 
    public Inventory createEmptyStorage(MinionType type) {
        int slots = resolveSlotCount();
        return Bukkit.createInventory(null, roundUpToMultipleOfNine(slots), "Minion Storage");
    }
 
    public Inventory deserializeStorage(byte[] serialized) {
        int slots = resolveSlotCount();
        Inventory inventory = Bukkit.createInventory(null, roundUpToMultipleOfNine(slots), "Minion Storage");
 
        if (serialized == null || serialized.length == 0) {
            return inventory;
        }
 
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(serialized);
             BukkitObjectInputStream objectStream = new BukkitObjectInputStream(byteStream)) {
            int storedSize = objectStream.readInt();
            for (int i = 0; i < storedSize && i < inventory.getSize(); i++) {
                ItemStack item = (ItemStack) objectStream.readObject();
                inventory.setItem(i, item);
            }
        } catch (IOException | ClassNotFoundException exception) {
            logger.warning("Failed to deserialize minion storage: " + exception.getMessage());
        }
 
        return inventory;
    }
 
    public byte[] serializeStorage(Inventory inventory) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream objectStream = new BukkitObjectOutputStream(byteStream)) {
            objectStream.writeInt(inventory.getSize());
            for (ItemStack item : inventory.getContents()) {
                objectStream.writeObject(item);
            }
            objectStream.flush();
            return byteStream.toByteArray();
        } catch (IOException exception) {
            logger.warning("Failed to serialize minion storage: " + exception.getMessage());
            return new byte[0];
        }
    }
 
    private int resolveSlotCount() {
        return minionsConfig.getBaseStorageSlots();
    }
 
    private int roundUpToMultipleOfNine(int value) {
        int remainder = value % 9;
        return remainder == 0 ? value : value + (9 - remainder);
    }
}
