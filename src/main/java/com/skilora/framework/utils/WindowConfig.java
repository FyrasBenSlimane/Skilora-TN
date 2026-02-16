package com.skilora.framework.utils;

import com.skilora.framework.layouts.TLAppLayout;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WindowConfig {

    public static void configureStage(Stage stage) {
        // Set transparent style for custom chrome
        stage.initStyle(StageStyle.TRANSPARENT);
    }

    public static void configureScene(Scene scene) {
        scene.setFill(Color.TRANSPARENT);
        TLAppLayout.applyStylesheets(scene);
    }
}
