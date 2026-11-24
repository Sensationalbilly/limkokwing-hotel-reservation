package com.hotel;

import com.hotel.ui.LoginFrame;
import com.hotel.ui.SplashWindow;
import com.hotel.ui.Theme;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        // All Swing work happens on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Using default look and feel: " + e.getMessage());
            }
            Theme.installDefaults();
            new SplashWindow().start(() -> new LoginFrame().setVisible(true));
        });
    }
}
