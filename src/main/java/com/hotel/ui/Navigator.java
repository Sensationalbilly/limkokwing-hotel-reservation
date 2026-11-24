package com.hotel.ui;

import com.hotel.model.Room;

/** Lets a screen ask the main window to switch pages without knowing about the window. */
public interface Navigator {

    void showPage(String pageId);

    /** Opens the booking form with this room pre-selected. */
    void bookRoom(Room room);
}
