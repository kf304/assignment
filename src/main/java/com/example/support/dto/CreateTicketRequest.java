package com.example.support.dto;

import com.example.support.model.TicketPriority;

/**
 * Input for creating a ticket. {@code priority} may be null, in which case the
 * service applies its default.
 */
public record CreateTicketRequest(String customerName,
                                  String subject,
                                  String description,
                                  TicketPriority priority) {

    public CreateTicketRequest(String customerName, String subject, String description) {
        this(customerName, subject, description, null);
    }
}
