package com.azthera.ecocore.data.model;
 
/**
 * Persistent state for a currently active (or most recent) market event,
 * so an in-progress Boom/Crash/Festival/etc survives a server restart
 * instead of resetting silently.
 */
public final class MarketEventRecord {
 
    private final long id;
    private final String eventType;
    private final long startedAtMillis;
    private long endsAtMillis;
    private double priceModifier;
    private double demandModifier;
    private boolean active;
 
    public MarketEventRecord(long id, String eventType, long startedAtMillis, long endsAtMillis,
                              double priceModifier, double demandModifier, boolean active) {
        this.id = id;
        this.eventType = eventType;
        this.startedAtMillis = startedAtMillis;
        this.endsAtMillis = endsAtMillis;
        this.priceModifier = priceModifier;
        this.demandModifier = demandModifier;
        this.active = active;
    }
 
    public long getId() {
        return id;
    }
 
    public String getEventType() {
        return eventType;
    }
 
    public long getStartedAtMillis() {
        return startedAtMillis;
    }
 
    public long getEndsAtMillis() {
        return endsAtMillis;
    }
 
    public void setEndsAtMillis(long endsAtMillis) {
        this.endsAtMillis = endsAtMillis;
    }
 
    public double getPriceModifier() {
        return priceModifier;
    }
 
    public void setPriceModifier(double priceModifier) {
        this.priceModifier = priceModifier;
    }
 
    public double getDemandModifier() {
        return demandModifier;
    }
 
    public void setDemandModifier(double demandModifier) {
        this.demandModifier = demandModifier;
    }
 
    public boolean isActive() {
        return active;
    }
 
    public void setActive(boolean active) {
        this.active = active;
    }
}
