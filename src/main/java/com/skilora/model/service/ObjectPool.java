package com.skilora.model.service;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * ObjectPool - Generic object pool for reusing expensive-to-create objects.
 * 
 * PERFORMANCE BENEFITS:
 * - Reduces garbage collection pressure
 * - Avoids repeated object allocation overhead
 * - Particularly useful for UI components that are frequently created/destroyed
 * 
 * Usage:
 * ObjectPool<JobCard> cardPool = new ObjectPool<>(
 * () -> new JobCard(), // Creator
 * card -> card.reset() // Resetter
 * );
 * 
 * JobCard card = cardPool.acquire();
 * // use card...
 * cardPool.release(card);
 */
public class ObjectPool<T> {

    private final ConcurrentLinkedQueue<T> pool;
    private final Supplier<T> creator;
    private final Consumer<T> resetter;
    private final int maxPoolSize;

    private static final int DEFAULT_MAX_POOL_SIZE = 50;

    /**
     * Create an object pool with default max size.
     * 
     * @param creator  Function to create new objects
     * @param resetter Function to reset objects before reuse (can be null)
     */
    public ObjectPool(Supplier<T> creator, Consumer<T> resetter) {
        this(creator, resetter, DEFAULT_MAX_POOL_SIZE);
    }

    /**
     * Create an object pool with custom max size.
     * 
     * @param creator     Function to create new objects
     * @param resetter    Function to reset objects before reuse (can be null)
     * @param maxPoolSize Maximum objects to keep in pool
     */
    public ObjectPool(Supplier<T> creator, Consumer<T> resetter, int maxPoolSize) {
        this.pool = new ConcurrentLinkedQueue<>();
        this.creator = creator;
        this.resetter = resetter;
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * Acquire an object from the pool, or create a new one if pool is empty.
     */
    public T acquire() {
        T obj = pool.poll();
        if (obj == null) {
            obj = creator.get();
        }
        return obj;
    }

    /**
     * Release an object back to the pool for reuse.
     * The object will be reset using the resetter function.
     */
    public void release(T obj) {
        if (obj == null)
            return;

        // Don't exceed max pool size
        if (pool.size() >= maxPoolSize) {
            return; // Let GC collect it
        }

        // Reset the object before returning to pool
        if (resetter != null) {
            resetter.accept(obj);
        }

        pool.offer(obj);
    }

    /**
     * Pre-warm the pool by creating objects ahead of time.
     */
    public void prewarm(int count) {
        for (int i = 0; i < Math.min(count, maxPoolSize); i++) {
            pool.offer(creator.get());
        }
    }

    /**
     * Clear all pooled objects.
     */
    public void clear() {
        pool.clear();
    }

    /**
     * Get current pool size.
     */
    public int size() {
        return pool.size();
    }
}
