package com.azthera.ecocore.data.cache;
 
import java.util.Collection;
import java.util.Optional;
 
/**
 * Simple key-value cache abstraction sitting in front of repositories, so
 * hot reads (balance checks, shop price lookups) do not hit the database on
 * every call. Kept as an interface so it can be swapped for a more advanced
 * implementation (e.g. Caffeine-backed) later without touching repositories.
 */
public interface Cache<K, V> {
 
    Optional<V> get(K key);
 
    void put(K key, V value);
 
    void invalidate(K key);
 
    void invalidateAll();
 
    Collection<V> values();
 
    boolean contains(K key);
}
