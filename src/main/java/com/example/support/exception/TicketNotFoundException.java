package com.example.support.exception;

/** Thrown when a ticket id does not exist. */
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(String ticketId) {
        super("Ticket not found: " + ticketId);
    }
}
