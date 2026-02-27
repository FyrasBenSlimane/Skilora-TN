package com.skilora.framework.utils;

import com.skilora.framework.layouts.TLAppLayout;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WindowConfig {

    public static void configureStage(Stage stage) {
        // Use DECORATED so the window is always visible (TRANSPARENT can hide the window on some systems)
        stage.initStyle(StageStyle.DECORATED);
    }

    public static void configureScene(Scene scene) {
        scene.setFill(javafx.scene.paint.Color.web("#09090b"));
        TLAppLayout.applyStylesheets(scene);
    }
}
