package com.skilora.framework.components;

import javafx.scene.control.Button;

public class TLCommand extends Button {
    public TLCommand() { super(); getStyleClass().add("tl-command"); }
    public TLCommand(String text) { super(text); getStyleClass().add("tl-command"); }
}
