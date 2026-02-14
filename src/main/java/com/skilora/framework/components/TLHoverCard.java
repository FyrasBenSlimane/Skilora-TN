package com.skilora.framework.components;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * TLHoverCard - shadcn/ui Hover Card (popover on hover delay).
 * Wraps a trigger node; shows popover after delay on mouse enter, hides on mouse exit.
 */
public class TLHoverCard extends VBox {

    private final TLPopover popover;
    private final PauseTransition showDelay;
    private static final int DELAY_MS = 400;

    public TLHoverCard() {
        getStyleClass().add("hover-card-wrapper");
        popover = new TLPopover();
        popover.getContentBox().getStyleClass().add("hover-card");
        showDelay = new PauseTransition(Duration.millis(DELAY_MS));
        showDelay.setOnFinished(e -> {
            if (getScene() != null && getScene().getWindow() != null)
                popover.show(TLHoverCard.this);
        });
    }

    public void setTrigger(Node trigger) {
        getChildren().setAll(trigger);
        trigger.setOnMouseEntered(e -> showDelay.playFromStart());
        trigger.setOnMouseExited(e -> {
            showDelay.stop();
            popover.hide();
        });
    }

    public void setCardContent(Node... nodes) {
        popover.setContent(nodes);
    }

    public TLPopover getPopover() {
        return popover;
    }

    public void setOpenDelayMs(int ms) {
        showDelay.setDuration(Duration.millis(ms));
    }
}
