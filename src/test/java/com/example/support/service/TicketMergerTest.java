package com.example.support.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.support.exception.TicketMergeException;
import com.example.support.model.Ticket;
import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class TicketMergerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-01T09:00:00Z");
    private static final Instant NOW = Instant.parse("2026-09-08T14:12:00Z");

    // --- requireMergeable -------------------------------------------------

    @Test
    void acceptsAMergeablefPair() {
        TicketMerger.requireMergeable(source(), target());
    }

    @EnumSource(value = TicketStatus.class, names = "CLOSED", mode = Mode.EXCLUDE)
    @ParameterizedTest
    void acceptsEveryNonClosedTargetStatus(TicketStatus status) {
        Ticket target = target();
        target.setStatus(status);

        TicketMerger.requireMergeable(source(), target);
    }

    @EnumSource(TicketStatus.class)
    @ParameterizedTest
    void acceptsASourceInAnyStatusIncludingClosed(TicketStatus status) {
        Ticket source = source();
        source.setStatus(status);

        TicketMerger.requireMergeable(source, target());
    }

    @Test
    void rejectsAnAlreadyMergedSourceAndNamesItsSurvivingTicket() {
        Ticket source = source();
        source.markMergedInto("TCK-1002", NOW);

        TicketMergeException error = assertThrows(TicketMergeException.class,
                () -> TicketMerger.requireMergeable(source, target()));
        assertTrue(error.getMessage().contains("TCK-1005"), error.getMessage());
        assertTrue(error.getMessage().contains("TCK-1002"), error.getMessage());
    }

    @Test
    void rejectsAnAlreadyMergedTargetAndNamesItsSurvivingTicket() {
        Ticket target = target();
        target.markMergedInto("TCK-1007", NOW);

        TicketMergeException error = assertThrows(TicketMergeException.class,
                () -> TicketMerger.requireMergeable(source(), target));
        assertTrue(error.getMessage().contains("TCK-1002"), error.getMessage());
        assertTrue(error.getMessage().contains("TCK-1007"), error.getMessage());
    }

    @Test
    void rejectsAClosedTarget() {
        Ticket target = target();
        target.setStatus(TicketStatus.CLOSED);

        TicketMergeException error = assertThrows(TicketMergeException.class,
                () -> TicketMerger.requireMergeable(source(), target));
        assertTrue(error.getMessage().contains("TCK-1002"), error.getMessage());
    }

    @Test
    void rejectsTicketsOfDifferentCustomers() {
        Ticket source = source();
        source.setCustomerName("Marco Silva");

        TicketMergeException error = assertThrows(TicketMergeException.class,
                () -> TicketMerger.requireMergeable(source, target()));
        assertTrue(error.getMessage().contains("Marco Silva"), error.getMessage());
        assertTrue(error.getMessage().contains("Priya Nair"), error.getMessage());
    }

    @Test
    void comparesCustomerNamesIgnoringCaseAndSurroundingSpace() {
        Ticket source = source();
        source.setCustomerName("  priya NAIR  ");

        TicketMerger.requireMergeable(source, target());
    }

    @Test
    void checksTheSourceBeforeTheTarget() {
        Ticket source = source();
        source.markMergedInto("TCK-1009", NOW);
        Ticket target = target();
        target.setStatus(TicketStatus.CLOSED);

        TicketMergeException error = assertThrows(TicketMergeException.class,
                () -> TicketMerger.requireMergeable(source, target));
        assertTrue(error.getMessage().contains("already been merged"), error.getMessage());
    }

    // --- mergeInto (target) -----------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "URGENT, HIGH, URGENT",
            "MEDIUM, HIGH, HIGH",
            "LOW, LOW, LOW",
            "MEDIUM, LOW, MEDIUM",
            "LOW, URGENT, URGENT"
    })
    void raisesTargetPriorityToTheHigherOfTheTwo(TicketPriority targetPriority,
                                                 TicketPriority sourcePriority,
                                                 TicketPriority expected) {
        Ticket target = target();
        target.setPriority(targetPriority);
        Ticket source = source();
        source.setPriority(sourcePriority);

        assertEquals(expected, TicketMerger.mergeInto(target, source, NOW).getPriority());
    }

    @Test
    void keepsTargetIdentityStatusCustomerSubjectAndCreatedAt() {
        Ticket target = target();
        target.setStatus(TicketStatus.IN_PROGRESS);

        Ticket merged = TicketMerger.mergeInto(target, source(), NOW);

        assertEquals("TCK-1002", merged.getId());
        assertEquals(TicketStatus.IN_PROGRESS, merged.getStatus());
        assertEquals("Priya Nair", merged.getCustomerName());
        assertEquals("Export to CSV times out for large reports", merged.getSubject());
        assertEquals(CREATED_AT, merged.getCreatedAt());
        assertEquals(NOW, merged.getUpdatedAt());
    }

    @Test
    void leavesTheTargetUnmerged() {
        Ticket merged = TicketMerger.mergeInto(target(), source(), NOW);

        assertFalse(merged.isMerged());
        assertNull(merged.getMergedIntoId());
        assertNull(merged.getMergedAt());
    }

    @Test
    void appendsSourceIdCustomerSubjectAndDescriptionAfterTheOriginalText() {
        Ticket target = target();
        String original = target.getDescription();

        String description = TicketMerger.mergeInto(target, source(), NOW).getDescription();

        assertTrue(description.startsWith(original), description);
        assertTrue(description.contains("--- Merged from TCK-1005 (Priya Nair, 2026-09-08T14:12:00Z) ---"),
                description);
        assertTrue(description.contains("Export to CSV still times out"), description);
        assertTrue(description.contains("Retried this morning on the 80k-row report"), description);
    }

    @Test
    void doesNotModifyTheSourceWhenMergingIntoTheTarget() {
        Ticket source = source();

        TicketMerger.mergeInto(target(), source, NOW);

        assertEquals("Retried this morning on the 80k-row report, same gateway timeout at ~60s.",
                source.getDescription());
        assertEquals(TicketStatus.OPEN, source.getStatus());
        assertEquals(CREATED_AT, source.getUpdatedAt());
    }

    // --- asTombstone (source) ---------------------------------------------

    @Test
    void closesTheSourceAndRecordsWhereItWent() {
        Ticket tombstone = TicketMerger.asTombstone(source(), "TCK-1002", NOW);

        assertEquals(TicketStatus.CLOSED, tombstone.getStatus());
        assertEquals("TCK-1002", tombstone.getMergedIntoId());
        assertEquals(NOW, tombstone.getMergedAt());
        assertEquals(NOW, tombstone.getUpdatedAt());
    }

    @EnumSource(TicketStatus.class)
    @ParameterizedTest
    void closesTheSourceFromAnyStatusIncludingClosed(TicketStatus status) {
        Ticket source = source();
        source.setStatus(status);

        assertEquals(TicketStatus.CLOSED, TicketMerger.asTombstone(source, "TCK-1002", NOW).getStatus());
    }

    @Test
    void keepsTheSourcePriorityCustomerSubjectAndCreatedAt() {
        Ticket tombstone = TicketMerger.asTombstone(source(), "TCK-1002", NOW);

        assertEquals(TicketPriority.HIGH, tombstone.getPriority());
        assertEquals("Priya Nair", tombstone.getCustomerName());
        assertEquals("Export to CSV still times out", tombstone.getSubject());
        assertEquals(CREATED_AT, tombstone.getCreatedAt());
    }

    @Test
    void annotatesTheSourceDescriptionWithItsMergeTarget() {
        Ticket source = source();
        String original = source.getDescription();

        String description = TicketMerger.asTombstone(source, "TCK-1002", NOW).getDescription();

        assertTrue(description.startsWith(original), description);
        assertTrue(description.contains("--- Merged into TCK-1002 on 2026-09-08T14:12:00Z ---"), description);
    }

    // --- description length (D5) -------------------------------------------

    @Test
    void truncatesOnlyTheAppendedBlockWhenTheCombinedDescriptionIsTooLong() {
        String original = "x".repeat(4900);
        Ticket target = target();
        target.setDescription(original);
        Ticket source = source();
        source.setDescription("y".repeat(400));

        String description = TicketMerger.mergeInto(target, source, NOW).getDescription();

        assertEquals(TicketValidator.MAX_DESCRIPTION_LENGTH, description.length());
        assertTrue(description.startsWith(original), "the target's own description must stay intact");
        assertTrue(description.endsWith("… [truncated]"), description.substring(description.length() - 40));
    }

    @Test
    void leavesTheDescriptionUnchangedWhenThereIsNoRoomForTheBlock() {
        String original = "x".repeat(TicketValidator.MAX_DESCRIPTION_LENGTH);
        Ticket target = target();
        target.setDescription(original);

        String description = TicketMerger.mergeInto(target, source(), NOW).getDescription();

        assertEquals(original, description);
        assertEquals(TicketValidator.MAX_DESCRIPTION_LENGTH, description.length());
    }

    @Test
    void doesNotTruncateWhenTheCombinedDescriptionFits() {
        String description = TicketMerger.mergeInto(target(), source(), NOW).getDescription();

        assertTrue(description.length() < TicketValidator.MAX_DESCRIPTION_LENGTH);
        assertFalse(description.endsWith("… [truncated]"));
    }

    @Test
    void truncatesTheTombstoneAnnotationTheSameWay() {
        Ticket source = source();
        source.setDescription("z".repeat(TicketValidator.MAX_DESCRIPTION_LENGTH - 10));

        String description = TicketMerger.asTombstone(source, "TCK-1002", NOW).getDescription();

        assertTrue(description.length() <= TicketValidator.MAX_DESCRIPTION_LENGTH,
                "length was " + description.length());
    }

    private static Ticket target() {
        return new Ticket("TCK-1002",
                "Priya Nair",
                "Export to CSV times out for large reports",
                "Exporting the monthly usage report (about 80k rows) fails after roughly 60 seconds.",
                TicketStatus.IN_PROGRESS,
                TicketPriority.URGENT,
                CREATED_AT);
    }

    private static Ticket source() {
        return new Ticket("TCK-1005",
                "Priya Nair",
                "Export to CSV still times out",
                "Retried this morning on the 80k-row report, same gateway timeout at ~60s.",
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                CREATED_AT);
    }
}
