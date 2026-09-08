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

    /** Set only when this ticket has been merged into another one; null otherwise. */
    private String mergedIntoId;
    private Instant mergedAt;

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

    /** Id of the ticket this one was merged into, or null when it has not been merged. */
    public String getMergedIntoId() {
        return mergedIntoId;
    }

    /** When this ticket was merged into another one, or null when it has not been merged. */
    public Instant getMergedAt() {
        return mergedAt;
    }

    public boolean isMerged() {
        return mergedIntoId != null;
    }

    /** Records that this ticket is a duplicate that was merged into {@code targetId}. */
    public void markMergedInto(String targetId, Instant at) {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(at, "mergedAt");
        String trimmed = targetId.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("targetId must not be blank");
        }
        if (trimmed.equals(id)) {
            throw new IllegalArgumentException("Ticket " + id + " cannot be merged into itself");
        }
        this.mergedIntoId = trimmed;
        this.mergedAt = at;
    }

    /** Returns an independent copy, so callers cannot mutate stored state by accident. */
    public Ticket copy() {
        Ticket copy = new Ticket(id, customerName, subject, description, status, priority, createdAt);
        copy.updatedAt = updatedAt;
        copy.mergedIntoId = mergedIntoId;
        copy.mergedAt = mergedAt;
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
        String merged = isMerged() ? ", mergedInto='%s'".formatted(mergedIntoId) : "";
        return "Ticket{id='%s', customer='%s', subject='%s', status=%s, priority=%s, createdAt=%s%s}"
                .formatted(id, customerName, subject, status, priority, createdAt, merged);
    }
}
