package com.skilora.service.usermanagement;

import javafx.scene.image.Image;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    public static Image get(String url) { return CACHE.get(url); }
    public static void put(String url, Image img) { if (url != null && img != null) CACHE.put(url, img); }
}
