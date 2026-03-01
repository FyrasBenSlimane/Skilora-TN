package com.skilora.framework.components;

import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

/**
 * TLInputOTP - shadcn/ui Input OTP (row of single-char inputs).
 * HBox of small TextFields; optional length (default 6).
 */
public class TLInputOTP extends HBox {

    private final List<TextField> slots = new ArrayList<>();
    private int length = 6;

    public TLInputOTP() {
        this(6);
    }

    public TLInputOTP(int slotCount) {
        getStyleClass().add("input-otp");
        setSpacing(8);
        setAlignment(javafx.geometry.Pos.CENTER);
        length = Math.max(1, Math.min(slotCount, 12));
        buildSlots();
    }

    private void buildSlots() {
        getChildren().clear();
        slots.clear();
        for (int i = 0; i < length; i++) {
            TextField tf = new TextField();
            tf.getStyleClass().add("input-otp-slot");
            tf.setMaxWidth(40);
            tf.setMinWidth(40);
            tf.setPrefColumnCount(1);
            final int idx = i;
            tf.textProperty().addListener((o, oldVal, newVal) -> {
                if (newVal != null && newVal.length() > 1) tf.setText(newVal.substring(0, 1));
                if (newVal != null && newVal.length() == 1 && idx < slots.size() - 1)
                    slots.get(idx + 1).requestFocus();
            });
            slots.add(tf);
            getChildren().add(tf);
        }
    }

    /** Get OTP string from all slots. */
    public String getValue() {
        StringBuilder sb = new StringBuilder();
        for (TextField t : slots) sb.append(t.getText() != null ? t.getText() : "");
        return sb.toString();
    }

    public void setValue(String value) {
        if (value == null) value = "";
        for (int i = 0; i < slots.size() && i < value.length(); i++)
            slots.get(i).setText(value.substring(i, i + 1));
        for (int i = value.length(); i < slots.size(); i++)
            slots.get(i).setText("");
    }

    public void setLength(int slotCount) {
        length = Math.max(1, Math.min(slotCount, 12));
        buildSlots();
    }
}
