package com.example.support.exception;

import com.example.support.model.TicketStatus;

/** Thrown when a requested status change is not allowed by the ticket lifecycle. */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to) {
        super("Cannot change ticket status from " + from + " to " + to);
    }
}
