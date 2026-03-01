package com.skilora.framework.components;

import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * TLAspectRatio - shadcn/ui Aspect Ratio.
 * Wraps content in a box that maintains width/height ratio (e.g. 16/9).
 */
public class TLAspectRatio extends StackPane {

    private double ratio = 16.0 / 9.0; // width / height

    public TLAspectRatio() {
        getStyleClass().addAll("aspect-ratio", "aspect-ratio-box");
    }

    public TLAspectRatio(Node content) {
        this();
        setContent(content);
    }

    public TLAspectRatio(Node content, double widthOverHeight) {
        this(content);
        setRatio(widthOverHeight);
    }

    public void setContent(Node content) {
        getChildren().setAll(content);
        applyRatio();
    }

    /** Set ratio = width / height (e.g. 16/9 = 1.78). */
    public void setRatio(double widthOverHeight) {
        this.ratio = widthOverHeight > 0 ? widthOverHeight : 16.0 / 9.0;
        applyRatio();
    }

    public double getRatio() {
        return ratio;
    }

    private void applyRatio() {
        prefHeightProperty().unbind();
        prefHeightProperty().bind(Bindings.createDoubleBinding(
            () -> getWidth() / ratio,
            widthProperty()
        ));
    }
}
