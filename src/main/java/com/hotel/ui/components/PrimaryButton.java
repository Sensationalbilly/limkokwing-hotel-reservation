package com.hotel.ui.components;

import com.hotel.ui.Theme;

import java.awt.Color;

/** Main action on a screen. */
public class PrimaryButton extends BaseButton {

    public PrimaryButton(String text) {
        super(text);
    }

    @Override
    protected Color getBaseColor() {
        return Theme.primary();
    }

    @Override
    protected Color getTextColor() {
        return Theme.onAccent();
    }
}
