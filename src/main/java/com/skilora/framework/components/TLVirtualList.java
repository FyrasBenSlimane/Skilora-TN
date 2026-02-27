package com.skilora.framework.components;

import javafx.scene.control.ListView;

public class TLVirtualList<T> extends ListView<T> {
    public TLVirtualList() { super(); getStyleClass().add("tl-virtual-list"); }
}
