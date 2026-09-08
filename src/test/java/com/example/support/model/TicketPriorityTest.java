package com.example.support.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class TicketPriorityTest {

    @ParameterizedTest
    @CsvSource({
            "LOW, LOW, LOW",
            "LOW, MEDIUM, MEDIUM",
            "LOW, HIGH, HIGH",
            "LOW, URGENT, URGENT",
            "MEDIUM, MEDIUM, MEDIUM",
            "MEDIUM, HIGH, HIGH",
            "MEDIUM, URGENT, URGENT",
            "HIGH, HIGH, HIGH",
            "HIGH, URGENT, URGENT",
            "URGENT, URGENT, URGENT"
    })
    void returnsTheMoreUrgentOfTwoPriorities(TicketPriority a, TicketPriority b, TicketPriority expected) {
        assertEquals(expected, TicketPriority.highestOf(a, b));
        assertEquals(expected, TicketPriority.highestOf(b, a), "highestOf must not depend on argument order");
    }

    @ParameterizedTest
    @EnumSource(TicketPriority.class)
    void returnsTheSameValueWhenBothAreEqual(TicketPriority priority) {
        assertEquals(priority, TicketPriority.highestOf(priority, priority));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> TicketPriority.highestOf(null, TicketPriority.LOW));
        assertThrows(NullPointerException.class, () -> TicketPriority.highestOf(TicketPriority.LOW, null));
        assertThrows(NullPointerException.class, () -> TicketPriority.highestOf(null, null));
    }
}
