package com.skilora.framework.utils;

public class PerformanceUtils {
    public static void runInBackground(Runnable task) {
        if (task == null) return;
        Thread t = new Thread(task, "Skilora-Bg");
        t.setDaemon(true);
        t.start();
    }
}
