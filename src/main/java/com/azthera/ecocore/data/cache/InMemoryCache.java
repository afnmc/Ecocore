package com.azthera.ecocore.data.cache;
 
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
 
/**
 * Thread-safe in-memory cache implementation backed by {@link ConcurrentHashMap}.
 * Safe to read/write from both the main thread and async database callback threads,
 * which is required since repositories are accessed from job/quest/shop async workflows.
 */
public final class InMemoryCache<K, V> implements Cache<K, V> {
 
    private final ConcurrentHashMap<K, V> store = new ConcurrentHashMap<>();
 
    @Override
    public Optional<V> get(K key) {
        return Optional.ofNullable(store.get(key));
    }
 
    @Override
    public void put(K key, V value) {
        store.put(key, value);
    }
 
    @Override
    public void invalidate(K key) {
        store.remove(key);
    }
 
    @Override
    public void invalidateAll() {
        store.clear();
    }
 
    @Override
    public Collection<V> values() {
        return List.copyOf(store.values());
    }
 
    @Override
    public boolean contains(K key) {
        return store.containsKey(key);
    }
}
