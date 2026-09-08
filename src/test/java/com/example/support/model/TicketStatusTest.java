package com.example.support.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketStatusTest {

    @ParameterizedTest
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "OPEN, RESOLVED",
            "OPEN, CLOSED",
            "IN_PROGRESS, RESOLVED",
            "IN_PROGRESS, OPEN",
            "RESOLVED, CLOSED",
            "RESOLVED, IN_PROGRESS",
            "CLOSED, OPEN"
    })
    @DisplayName("allows the transitions of the ticket lifecycle")
    void allowsValidTransitions(TicketStatus from, TicketStatus to) {
        assertTrue(from.canTransitionTo(to));
    }

    @ParameterizedTest
    @CsvSource({
            "CLOSED, IN_PROGRESS",
            "CLOSED, RESOLVED",
            "RESOLVED, OPEN"
    })
    @DisplayName("rejects transitions outside the lifecycle")
    void rejectsInvalidTransitions(TicketStatus from, TicketStatus to) {
        assertFalse(from.canTransitionTo(to));
    }

    @Test
    void rejectsTransitionToSameStatusAndNull() {
        assertFalse(TicketStatus.OPEN.canTransitionTo(TicketStatus.OPEN));
        assertFalse(TicketStatus.OPEN.canTransitionTo(null));
    }
}
