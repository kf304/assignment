package com.example.support.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TicketTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-01T09:00:00Z");
    private static final Instant MERGED_AT = Instant.parse("2026-09-08T14:12:00Z");

    @Test
    void freshTicketIsNotMerged() {
        Ticket ticket = ticket("TCK-1005");

        assertFalse(ticket.isMerged());
        assertNull(ticket.getMergedIntoId());
        assertNull(ticket.getMergedAt());
    }

    @Test
    void marksTicketAsMergedIntoAnotherTicket() {
        Ticket ticket = ticket("TCK-1005");

        ticket.markMergedInto("TCK-1002", MERGED_AT);

        assertTrue(ticket.isMerged());
        assertEquals("TCK-1002", ticket.getMergedIntoId());
        assertEquals(MERGED_AT, ticket.getMergedAt());
    }

    @Test
    void trimsTheMergeTargetId() {
        Ticket ticket = ticket("TCK-1005");

        ticket.markMergedInto("  TCK-1002  ", MERGED_AT);

        assertEquals("TCK-1002", ticket.getMergedIntoId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankMergeTargetId(String targetId) {
        Ticket ticket = ticket("TCK-1005");

        assertThrows(IllegalArgumentException.class, () -> ticket.markMergedInto(targetId, MERGED_AT));
    }

    @Test
    void rejectsNullMergeArguments() {
        Ticket ticket = ticket("TCK-1005");

        assertThrows(NullPointerException.class, () -> ticket.markMergedInto(null, MERGED_AT));
        assertThrows(NullPointerException.class, () -> ticket.markMergedInto("TCK-1002", null));
    }

    @Test
    void rejectsMergingATicketIntoItself() {
        Ticket ticket = ticket("TCK-1005");

        assertThrows(IllegalArgumentException.class, () -> ticket.markMergedInto("TCK-1005", MERGED_AT));
    }

    @Test
    void copyPreservesMergeState() {
        Ticket ticket = ticket("TCK-1005");
        ticket.setUpdatedAt(MERGED_AT);
        ticket.markMergedInto("TCK-1002", MERGED_AT);

        Ticket copy = ticket.copy();

        assertEquals("TCK-1002", copy.getMergedIntoId());
        assertEquals(MERGED_AT, copy.getMergedAt());
        assertEquals(MERGED_AT, copy.getUpdatedAt());
        assertTrue(copy.isMerged());
    }

    @Test
    void copyOfAnUnmergedTicketHasNoMergeState() {
        Ticket copy = ticket("TCK-1005").copy();

        assertFalse(copy.isMerged());
        assertNull(copy.getMergedIntoId());
        assertNull(copy.getMergedAt());
    }

    @Test
    void copyIsIndependentOfTheOriginalMergeState() {
        Ticket ticket = ticket("TCK-1005");
        Ticket copy = ticket.copy();

        ticket.markMergedInto("TCK-1002", MERGED_AT);

        assertFalse(copy.isMerged());
    }

    @Test
    void toStringMentionsTheMergeTargetOnlyWhenMerged() {
        Ticket ticket = ticket("TCK-1005");

        assertFalse(ticket.toString().contains("mergedInto"));

        ticket.markMergedInto("TCK-1002", MERGED_AT);

        assertTrue(ticket.toString().contains("mergedInto='TCK-1002'"));
    }

    private static Ticket ticket(String id) {
        return new Ticket(id,
                "Priya Nair",
                "Export to CSV still times out",
                "Retried this morning on the 80k-row report, same gateway timeout at ~60s.",
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                CREATED_AT);
    }
}
