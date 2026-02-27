package com.skilora.framework.components;

import javafx.scene.control.TableView;

public class TLTable<T> extends TableView<T> {
    public TLTable() { super(); getStyleClass().add("tl-table"); }
}
