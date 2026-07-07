package com.azthera.ecocore.data.model;
 
import java.util.UUID;
 
/**
 * Persistent state for a single placed minion instance: owner, type,
 * location, upgrade levels, and remaining fuel. Inventory contents for the
 * minion's storage are persisted separately via a serialized item-stack blob
 * to keep this record lightweight.
 */
public final class MinionData {
 
    private final UUID minionId;
    private final UUID ownerId;
    private final String minionType;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private int speedLevel;
    private int capacityLevel;
    private int fortuneLevel;
    private int fuelRemaining;
    private byte[] serializedStorage;
 
    public MinionData(UUID minionId, UUID ownerId, String minionType, String worldName, double x, double y, double z,
                       int speedLevel, int capacityLevel, int fortuneLevel, int fuelRemaining,
                       byte[] serializedStorage) {
        this.minionId = minionId;
        this.ownerId = ownerId;
        this.minionType = minionType;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.speedLevel = speedLevel;
        this.capacityLevel = capacityLevel;
        this.fortuneLevel = fortuneLevel;
        this.fuelRemaining = fuelRemaining;
        this.serializedStorage = serializedStorage;
    }
 
    public UUID getMinionId() {
        return minionId;
    }
 
    public UUID getOwnerId() {
        return ownerId;
    }
 
    public String getMinionType() {
        return minionType;
    }
 
    public String getWorldName() {
        return worldName;
    }
 
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
 
    public double getX() {
        return x;
    }
 
    public void setX(double x) {
        this.x = x;
    }
 
    public double getY() {
        return y;
    }
 
    public void setY(double y) {
        this.y = y;
    }
 
    public double getZ() {
        return z;
    }
 
    public void setZ(double z) {
        this.z = z;
    }
 
    public int getSpeedLevel() {
        return speedLevel;
    }
 
    public void setSpeedLevel(int speedLevel) {
        this.speedLevel = speedLevel;
    }
 
    public int getCapacityLevel() {
        return capacityLevel;
    }
 
    public void setCapacityLevel(int capacityLevel) {
        this.capacityLevel = capacityLevel;
    }
 
    public int getFortuneLevel() {
        return fortuneLevel;
    }
 
    public void setFortuneLevel(int fortuneLevel) {
        this.fortuneLevel = fortuneLevel;
    }
 
    public int getFuelRemaining() {
        return fuelRemaining;
    }
 
    public void setFuelRemaining(int fuelRemaining) {
        this.fuelRemaining = fuelRemaining;
    }
 
    public byte[] getSerializedStorage() {
        return serializedStorage;
    }
 
    public void setSerializedStorage(byte[] serializedStorage) {
        this.serializedStorage = serializedStorage;
    }
}
