package com.skilora.service.usermanagement;

import javafx.scene.image.Image;

public class FastCameraManager {
    private static volatile FastCameraManager instance;

    public static synchronized FastCameraManager getInstance() {
        if (instance == null) instance = new FastCameraManager();
        return instance;
    }

    public Image capture() { return null; }
}
