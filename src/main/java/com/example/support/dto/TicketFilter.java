package com.example.support.dto;

import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;

/**
 * Optional criteria for listing tickets; a null field means "no restriction".
 * {@code excludeMerged} hides tickets that were closed as duplicates of another ticket;
 * it defaults to {@code false}, so merged tickets are listed unless they are opted out.
 */
public record TicketFilter(TicketStatus status,
                           TicketPriority priority,
                           String customerName,
                           boolean excludeMerged) {

    /** Criteria that keep merged tickets in the results. */
    public TicketFilter(TicketStatus status, TicketPriority priority, String customerName) {
        this(status, priority, customerName, false);
    }

    public static TicketFilter none() {
        return new TicketFilter(null, null, null);
    }

    public static TicketFilter byStatus(TicketStatus status) {
        return new TicketFilter(status, null, null);
    }

    public static TicketFilter byPriority(TicketPriority priority) {
        return new TicketFilter(null, priority, null);
    }

    public static TicketFilter byCustomer(String customerName) {
        return new TicketFilter(null, null, customerName);
    }

    /** Every ticket that has not been merged into another one. */
    public static TicketFilter excludingMerged() {
        return new TicketFilter(null, null, null, true);
    }
}
