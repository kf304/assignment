package com.example.support.dto;

import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;

/** Optional criteria for listing tickets; a null field means "no restriction". */
public record TicketFilter(TicketStatus status, TicketPriority priority, String customerName) {

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
}
