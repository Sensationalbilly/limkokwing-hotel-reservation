package com.hotel.controller;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Small publish/subscribe hub (observer pattern). Controllers publish a topic after they
 * change data and every screen showing that data refreshes itself, so screens never
 * need references to each other. Listeners always run on the Swing event thread.
 */
public final class EventBus {

    public enum Topic { ROOMS_CHANGED, CUSTOMERS_CHANGED, BOOKINGS_CHANGED, PASSWORD_REQUESTS_CHANGED }

    private static final Map<Topic, List<Runnable>> SUBSCRIBERS = new EnumMap<>(Topic.class);

    private EventBus() { }

    public static synchronized void subscribe(Topic topic, Runnable listener) {
        SUBSCRIBERS.computeIfAbsent(topic, t -> new ArrayList<>()).add(listener);
    }

    public static void publish(Topic topic) {
        List<Runnable> listeners;
        synchronized (EventBus.class) {
            listeners = new ArrayList<>(SUBSCRIBERS.getOrDefault(topic, List.of()));
        }
        Runnable notifyAll = () -> listeners.forEach(Runnable::run);
        if (SwingUtilities.isEventDispatchThread()) {
            notifyAll.run();
        } else {
            SwingUtilities.invokeLater(notifyAll);
        }
    }

    /** Drops every subscriber, e.g. when the dashboard window closes. */
    public static synchronized void clear() {
        SUBSCRIBERS.clear();
    }
}
