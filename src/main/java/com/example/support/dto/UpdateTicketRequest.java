package com.example.support.dto;

import com.example.support.model.TicketPriority;

/**
 * Partial update of a ticket: every field is optional and a null value means
 * "leave unchanged". Status is changed through its own operation because it has
 * transition rules.
 */
public record UpdateTicketRequest(String customerName,
                                  String subject,
                                  String description,
                                  TicketPriority priority) {

    public boolean isEmpty() {
        return customerName == null && subject == null && description == null && priority == null;
    }
}
