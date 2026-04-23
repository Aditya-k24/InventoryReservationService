package com.example.inventoryreservation.exception;

public class InvalidReservationStateException extends RuntimeException {
    public InvalidReservationStateException(String message) {
        super(message);
    }
}
