package com.skilora.model.service;

import javafx.scene.image.Image;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ImageCache - Thread-safe singleton with LRU eviction.
 * Uses a LinkedHashMap with access-order for true LRU behavior
 * and a ReadWriteLock for concurrent read access.
 */
public class ImageCache {

    private static volatile ImageCache instance;

    private static final int MAX_CACHE_SIZE = 120;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // LinkedHashMap with accessOrder=true gives LRU eviction for free
    private final LinkedHashMap<String, Image> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private ImageCache() {
    }

    public static ImageCache getInstance() {
        if (instance == null) {
            synchronized (ImageCache.class) {
                if (instance == null) {
                    instance = new ImageCache();
                }
            }
        }
        return instance;
    }

    /**
     * Get an image from cache or load it if not cached.
     * Uses read lock for cache hits (concurrent), write lock only for misses.
     */
    public Image getImage(String url, double width, double height) {
        if (url == null || url.isEmpty())
            return null;

        String cacheKey = url + "_" + (int) width + "x" + (int) height;

        // Fast path: read lock for cache hit
        lock.readLock().lock();
        try {
            Image cached = cache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } finally {
            lock.readLock().unlock();
        }

        // Slow path: write lock for cache miss
        lock.writeLock().lock();
        try {
            // Double-check after acquiring write lock
            Image cached = cache.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            // backgroundLoading=true, smooth=false for fast initial render
            Image img = new Image(url, width, height, true, false, true);
            cache.put(cacheKey, img);
            return img;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get an image with default sizing.
     */
    public Image getImage(String url) {
        if (url == null || url.isEmpty())
            return null;

        lock.readLock().lock();
        try {
            Image cached = cache.get(url);
            if (cached != null) {
                return cached;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            Image cached = cache.get(url);
            if (cached != null) {
                return cached;
            }
            Image img = new Image(url, true);
            cache.put(url, img);
            return img;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Preload an image into cache (call from background thread).
     */
    public void preload(String url, double width, double height) {
        getImage(url, width, height);
    }

    /**
     * Clear all cached images.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get current cache size.
     */
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
