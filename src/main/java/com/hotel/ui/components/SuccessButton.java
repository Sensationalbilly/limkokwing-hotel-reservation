package com.hotel.ui.components;

import com.hotel.ui.Theme;

import java.awt.Color;

/** Confirms something positive, e.g. "Confirm booking". */
public class SuccessButton extends BaseButton {

    public SuccessButton(String text) {
        super(text);
    }

    @Override
    protected Color getBaseColor() {
        return Theme.success();
    }

    @Override
    protected Color getTextColor() {
        return Theme.onAccent();
    }
}
