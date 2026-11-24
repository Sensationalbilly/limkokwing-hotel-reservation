package com.hotel.controller;

/** A business rule was broken; the message is safe to show to the user as-is. */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }
}
