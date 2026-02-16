package com.skilora.user.service;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MediaCache - Preloads video assets for instant playback.
 * Simplified prewarming with no nested threads or MAX_PRIORITY.
 */
public class MediaCache {

    private static volatile MediaCache instance;
    private Media heroMedia;
    private MediaPlayer cachedPlayer;
    private final AtomicBoolean isPrewarmed = new AtomicBoolean(false);
    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    private MediaCache() {
    }

    public static MediaCache getInstance() {
        if (instance == null) {
            synchronized (MediaCache.class) {
                if (instance == null) {
                    instance = new MediaCache();
                }
            }
        }
        return instance;
    }

    /**
     * Preloads the video file. Should be called early (e.g., in Main.init()).
     * Uses a daemon thread at normal priority to avoid starving other startup tasks.
     */
    public void prewarm() {
        if (isPrewarmed.get() || isLoading.get())
            return;
        isLoading.set(true);

        Thread preloader = new Thread(() -> {
            try {
                URL resource = getClass().getResource("/com/skilora/assets/videos/hero.mp4");
                if (resource == null) {
                    return;
                }

                heroMedia = new Media(resource.toExternalForm());

                CountDownLatch readyLatch = new CountDownLatch(1);

                Platform.runLater(() -> {
                    try {
                        cachedPlayer = new MediaPlayer(heroMedia);
                        cachedPlayer.setVolume(0);
                        cachedPlayer.setMute(true);
                        cachedPlayer.setCycleCount(1);

                        cachedPlayer.setOnReady(() -> {
                            // Seek to start to preload initial frames - no nested thread needed
                            cachedPlayer.seek(Duration.ZERO);
                            cachedPlayer.play();

                            // Use a JavaFX timeline to pause after a brief play
                            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                                    javafx.util.Duration.millis(100));
                            pause.setOnFinished(e -> {
                                cachedPlayer.pause();
                                cachedPlayer.seek(Duration.ZERO);
                                isPrewarmed.set(true);
                                readyLatch.countDown();
                            });
                            pause.play();
                        });

                        cachedPlayer.setOnError(() -> {
                            readyLatch.countDown();
                        });

                    } catch (Exception e) {
                        readyLatch.countDown();
                    }
                });

                readyLatch.await(5, TimeUnit.SECONDS);

            } catch (Exception e) {
                // Video prewarming is optional
            } finally {
                isLoading.set(false);
            }
        }, "MediaCache-Preloader");

        preloader.setDaemon(true);
        // Normal priority - don't starve other initialization
        preloader.setPriority(Thread.NORM_PRIORITY);
        preloader.start();
    }

    /**
     * Returns the preloaded Media object.
     */
    public Media getHeroMedia() {
        if (heroMedia == null) {
            try {
                URL resource = getClass().getResource("/com/skilora/assets/videos/hero.mp4");
                if (resource != null) {
                    heroMedia = new Media(resource.toExternalForm());
                }
            } catch (Exception e) {
                return null;
            }
        }
        return heroMedia;
    }

    /**
     * Returns a MediaPlayer ready for immediate playback.
     */
    public MediaPlayer createReadyPlayer() {
        Media media = getHeroMedia();
        if (media == null)
            return null;

        MediaPlayer player = new MediaPlayer(media);
        player.setVolume(0);
        player.setMute(true);
        player.setCycleCount(MediaPlayer.INDEFINITE);
        player.setAutoPlay(true);

        return player;
    }

    /**
     * Cleanup resources when no longer needed.
     */
    public void dispose() {
        if (cachedPlayer != null) {
            cachedPlayer.dispose();
            cachedPlayer = null;
        }
    }
}
