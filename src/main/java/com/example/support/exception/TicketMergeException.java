package com.example.support.exception;

/**
 * Thrown when two tickets cannot be merged: merging a ticket with itself, a ticket
 * that has already been merged, a closed target, or tickets of different customers.
 */
public class TicketMergeException extends RuntimeException {

    public TicketMergeException(String message) {
        super(message);
    }
}
