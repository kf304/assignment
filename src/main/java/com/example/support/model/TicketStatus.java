package com.example.support.model;

import java.util.Set;

/**
 * Lifecycle state of a ticket. Each status declares which statuses it may move to,
 * so the service layer can reject nonsensical transitions.
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public boolean canTransitionTo(TicketStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return allowedTargets().contains(target);
    }

    private Set<TicketStatus> allowedTargets() {
        return switch (this) {
            case OPEN -> Set.of(IN_PROGRESS, RESOLVED, CLOSED);
            case IN_PROGRESS -> Set.of(OPEN, RESOLVED, CLOSED);
            case RESOLVED -> Set.of(IN_PROGRESS, CLOSED);
            case CLOSED -> Set.of(OPEN);
        };
    }
}
