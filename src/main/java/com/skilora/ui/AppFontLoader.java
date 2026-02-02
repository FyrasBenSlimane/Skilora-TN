package com.skilora.ui;

import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;

public class AppFontLoader {

    private static final Logger logger = LoggerFactory.getLogger(AppFontLoader.class);
    /**
     * Loads Inter fonts from resources if available.
     * Use /com/skilora/ui/fonts/Inter-*.otf
     */
    public static void load() {
        // Names must match file names in resources
        // We attempt to load them. If not found, it's fine (fallback to system).
        loadFont("Inter-Regular.otf", 12);
        loadFont("Inter-Medium.otf", 12);
        loadFont("Inter-SemiBold.otf", 12);
        loadFont("Inter-Bold.otf", 12);
    }

    private static void loadFont(String name, double size) {
        String path = "/com/skilora/ui/fonts/" + name;
        try (InputStream is = AppFontLoader.class.getResourceAsStream(path)) {
            if (is != null) {
                Font f = Font.loadFont(is, size);
                if (f == null) {
                    logger.error("Failed to load font (format error?): " + path);
                }
            } else {
                logger.debug("Font resource not found: " + path);
            }
        } catch (Exception e) {
            logger.error("Error loading font: " + path, e);
        }
    }
}
