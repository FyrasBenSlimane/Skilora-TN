package com.skilora.framework.components;

import javafx.scene.layout.Region;

/**
 * TLSkeleton - shadcn/ui Skeleton (loading placeholder).
 * Region with .skeleton; optional .skeleton-circle for avatar placeholder.
 */
public class TLSkeleton extends Region {

    public enum Shape {
        RECTANGLE, CIRCLE
    }

    public TLSkeleton() {
        this(200, 20, Shape.RECTANGLE);
    }

    public TLSkeleton(double width, double height) {
        this(width, height, Shape.RECTANGLE);
    }

    public TLSkeleton(double width, double height, Shape shape) {
        getStyleClass().add("skeleton");
        if (shape == Shape.CIRCLE) getStyleClass().add("skeleton-circle");
        else getStyleClass().add("skeleton-rectangle");
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
    }
}
