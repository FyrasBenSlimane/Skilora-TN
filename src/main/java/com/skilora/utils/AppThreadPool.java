package com.skilora.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Shared daemon thread pool for background tasks across all modules.
 * Replaces ad-hoc {@code new Thread(task).start()} patterns with a bounded,
 * reusable pool that is shut down cleanly on application exit.
 *
 * <p>Usage:
 * <pre>{@code
 *   AppThreadPool.execute(myTask);       // fire-and-forget
 *   AppThreadPool.submit(myCallable);    // if you need a Future
 * }</pre>
 */
public final class AppThreadPool {

    private static final int POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());

    private static final ExecutorService POOL = Executors.newFixedThreadPool(POOL_SIZE, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("skilora-pool-" + t.getId());
        return t;
    });

    private AppThreadPool() {}

    /** Execute a {@link Runnable} on the shared pool. */
    public static void execute(Runnable task) {
        POOL.execute(task);
    }

    /** Submit a {@link java.util.concurrent.Callable} and return a {@link java.util.concurrent.Future}. */
    public static <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) {
        return POOL.submit(task);
    }

    /** Orderly shutdown — call once from {@code Application.stop()} or a shutdown hook. */
    public static void shutdown() {
        POOL.shutdown();
        try {
            if (!POOL.awaitTermination(3, TimeUnit.SECONDS)) {
                POOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
