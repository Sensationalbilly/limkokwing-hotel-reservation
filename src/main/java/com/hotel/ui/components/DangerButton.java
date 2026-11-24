package com.hotel.ui.components;

import com.hotel.ui.Theme;

import java.awt.Color;

/** Destructive action such as cancel or delete. */
public class DangerButton extends BaseButton {

    public DangerButton(String text) {
        super(text);
    }

    @Override
    protected Color getBaseColor() {
        return Theme.danger();
    }

    @Override
    protected Color getTextColor() {
        return Theme.onAccent();
    }
}
