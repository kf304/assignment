package com.example.support.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A customer support ticket.
 *
 * <p>Identity and creation time are immutable; the remaining fields are mutable
 * because agents edit tickets over their lifetime.
 */
public class Ticket {

    private final String id;
    private final Instant createdAt;

    private String customerName;
    private String subject;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Instant updatedAt;

    public Ticket(String id,
                  String customerName,
                  String subject,
                  String description,
                  TicketStatus status,
                  TicketPriority priority,
                  Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerName = Objects.requireNonNull(customerName, "customerName");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.description = Objects.requireNonNull(description, "description");
        this.status = Objects.requireNonNull(status, "status");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = Objects.requireNonNull(customerName, "customerName");
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = Objects.requireNonNull(subject, "subject");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Objects.requireNonNull(description, "description");
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = Objects.requireNonNull(priority, "priority");
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** Returns an independent copy, so callers cannot mutate stored state by accident. */
    public Ticket copy() {
        Ticket copy = new Ticket(id, customerName, subject, description, status, priority, createdAt);
        copy.updatedAt = updatedAt;
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Ticket ticket)) {
            return false;
        }
        return id.equals(ticket.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Ticket{id='%s', customer='%s', subject='%s', status=%s, priority=%s, createdAt=%s}"
                .formatted(id, customerName, subject, status, priority, createdAt);
    }
}
