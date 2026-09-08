package com.example.support.model;

import java.util.Objects;

/** Urgency of a ticket, ordered from least to most urgent. */
public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    /** Returns the more urgent of two priorities; equal values return that value. */
    public static TicketPriority highestOf(TicketPriority first, TicketPriority second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return first.compareTo(second) >= 0 ? first : second;
    }
}
