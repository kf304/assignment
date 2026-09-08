package com.example.support.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.support.SampleData;
import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.TicketFilter;
import com.example.support.dto.UpdateTicketRequest;
import com.example.support.exception.InvalidStatusTransitionException;
import com.example.support.exception.TicketMergeException;
import com.example.support.exception.TicketNotFoundException;
import com.example.support.exception.ValidationException;
import com.example.support.model.Ticket;
import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;
import com.example.support.repository.InMemoryTicketRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.ValueSource;

class TicketServiceTest {

    private static final Instant START_TIME = Instant.parse("2026-09-01T09:00:00Z");

    private MutableClock clock;
    private TicketService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(START_TIME);
        service = new TicketService(new InMemoryTicketRepository(),
                new SequentialTicketIdGenerator(1000L),
                clock);
    }

    @Nested
    class Creation {

        @Test
        void createsTicketWithGeneratedIdAndDefaults() {
            Ticket ticket = service.createTicket(new CreateTicketRequest(
                    "Aisha Rahman",
                    "Cannot log in after password reset",
                    "The new password is rejected on the web login."));

            assertEquals("TCK-1000", ticket.getId());
            assertEquals(TicketStatus.OPEN, ticket.getStatus());
            assertEquals(TicketPriority.MEDIUM, ticket.getPriority());
            assertEquals(START_TIME, ticket.getCreatedAt());
            assertEquals(START_TIME, ticket.getUpdatedAt());
        }

        @Test
        void keepsExplicitPriorityAndTrimsText() {
            Ticket ticket = service.createTicket(new CreateTicketRequest(
                    "  Priya Nair  ",
                    "  Export to CSV times out  ",
                    "  The monthly usage export fails after 60 seconds.  ",
                    TicketPriority.URGENT));

            assertEquals("Priya Nair", ticket.getCustomerName());
            assertEquals("Export to CSV times out", ticket.getSubject());
            assertEquals("The monthly usage export fails after 60 seconds.", ticket.getDescription());
            assertEquals(TicketPriority.URGENT, ticket.getPriority());
        }

        @Test
        void generatesAscendingIds() {
            String first = service.createTicket(validRequest()).getId();
            String second = service.createTicket(validRequest()).getId();

            assertNotEquals(first, second);
            assertEquals("TCK-1001", second);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejectsBlankCustomerName(String name) {
            CreateTicketRequest request = new CreateTicketRequest(name, "Subject", "Description");

            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.createTicket(request));
            assertTrue(error.getMessage().contains("customerName"));
        }

        @Test
        void rejectsNullAndBlankFields() {
            assertThrows(ValidationException.class, () -> service.createTicket(null));
            assertThrows(ValidationException.class, () -> service.createTicket(
                    new CreateTicketRequest("Tomas Berg", null, "Description")));
            assertThrows(ValidationException.class, () -> service.createTicket(
                    new CreateTicketRequest("Tomas Berg", "Subject", "  ")));
        }

        @Test
        void rejectsOverlongSubject() {
            String subject = "x".repeat(TicketValidator.MAX_SUBJECT_LENGTH + 1);

            assertThrows(ValidationException.class, () -> service.createTicket(
                    new CreateTicketRequest("Tomas Berg", subject, "Description")));
        }
    }

    @Nested
    class Retrieval {

        @Test
        void retrievesTicketById() {
            String id = service.createTicket(validRequest()).getId();

            assertEquals(id, service.getTicket(id).getId());
        }

        @Test
        void throwsForUnknownId() {
            assertThrows(TicketNotFoundException.class, () -> service.getTicket("TCK-9999"));
        }

        @Test
        void throwsForBlankId() {
            assertThrows(ValidationException.class, () -> service.getTicket("  "));
            assertThrows(ValidationException.class, () -> service.getTicket(null));
        }

        @Test
        void returnedTicketIsDetachedFromTheStore() {
            String id = service.createTicket(validRequest()).getId();

            service.getTicket(id).setSubject("Changed outside the service");

            assertEquals("Cannot log in after password reset", service.getTicket(id).getSubject());
        }
    }

    @Nested
    class Updating {

        @Test
        void updatesOnlyProvidedFields() {
            String id = service.createTicket(validRequest()).getId();
            clock.advance(Duration.ofMinutes(5));

            Ticket updated = service.updateTicket(id,
                    new UpdateTicketRequest(null, "Login still fails after reset", null, TicketPriority.HIGH));

            assertEquals("Login still fails after reset", updated.getSubject());
            assertEquals(TicketPriority.HIGH, updated.getPriority());
            assertEquals("Aisha Rahman", updated.getCustomerName());
            assertEquals(START_TIME.plus(Duration.ofMinutes(5)), updated.getUpdatedAt());
            assertEquals(START_TIME, updated.getCreatedAt());
        }

        @Test
        void rejectsEmptyUpdate() {
            String id = service.createTicket(validRequest()).getId();

            assertThrows(ValidationException.class, () -> service.updateTicket(id,
                    new UpdateTicketRequest(null, null, null, null)));
        }

        @Test
        void rejectsBlankValueInUpdate() {
            String id = service.createTicket(validRequest()).getId();

            assertThrows(ValidationException.class, () -> service.updateTicket(id,
                    new UpdateTicketRequest(null, "   ", null, null)));
        }

        @Test
        void rejectsUpdateOfClosedTicket() {
            String id = service.createTicket(validRequest()).getId();
            service.changeStatus(id, TicketStatus.CLOSED);

            assertThrows(ValidationException.class, () -> service.updateTicket(id,
                    new UpdateTicketRequest(null, "Reopened subject", null, null)));
        }

        @Test
        void throwsForUnknownTicket() {
            assertThrows(TicketNotFoundException.class, () -> service.updateTicket("TCK-9999",
                    new UpdateTicketRequest(null, "Subject", null, null)));
        }

        @Test
        void changesPriority() {
            String id = service.createTicket(validRequest()).getId();

            assertEquals(TicketPriority.URGENT, service.changePriority(id, TicketPriority.URGENT).getPriority());
            assertThrows(ValidationException.class, () -> service.changePriority(id, null));
        }
    }

    @Nested
    class StatusChanges {

        @Test
        @DisplayName("moves a ticket through its lifecycle and stamps updatedAt")
        void changesStatus() {
            String id = service.createTicket(validRequest()).getId();
            clock.advance(Duration.ofMinutes(10));

            Ticket inProgress = service.changeStatus(id, TicketStatus.IN_PROGRESS);
            assertEquals(TicketStatus.IN_PROGRESS, inProgress.getStatus());
            assertEquals(START_TIME.plus(Duration.ofMinutes(10)), inProgress.getUpdatedAt());

            assertEquals(TicketStatus.RESOLVED, service.changeStatus(id, TicketStatus.RESOLVED).getStatus());
            assertEquals(TicketStatus.CLOSED, service.changeStatus(id, TicketStatus.CLOSED).getStatus());
        }

        @Test
        void rejectsInvalidTransition() {
            String id = service.createTicket(validRequest()).getId();
            service.changeStatus(id, TicketStatus.CLOSED);

            assertThrows(InvalidStatusTransitionException.class,
                    () -> service.changeStatus(id, TicketStatus.RESOLVED));
        }

        @Test
        void rejectsTransitionToSameStatus() {
            String id = service.createTicket(validRequest()).getId();

            assertThrows(InvalidStatusTransitionException.class,
                    () -> service.changeStatus(id, TicketStatus.OPEN));
        }

        @Test
        void rejectsNullStatusAndUnknownTicket() {
            String id = service.createTicket(validRequest()).getId();

            assertThrows(ValidationException.class, () -> service.changeStatus(id, null));
            assertThrows(TicketNotFoundException.class,
                    () -> service.changeStatus("TCK-9999", TicketStatus.CLOSED));
        }
    }

    @Nested
    class Listing {

        @BeforeEach
        void seed() {
            SampleData.seed(service);
        }

        @Test
        void listsAllTicketsNewestFirst() {
            List<Ticket> tickets = service.listTickets();

            assertEquals(SampleData.tickets().size(), tickets.size());
            for (int i = 1; i < tickets.size(); i++) {
                assertFalse(tickets.get(i - 1).getCreatedAt().isBefore(tickets.get(i).getCreatedAt()));
            }
        }

        @Test
        void filtersByStatus() {
            List<Ticket> inProgress = service.listTickets(TicketFilter.byStatus(TicketStatus.IN_PROGRESS));

            assertEquals(2, inProgress.size());
            assertTrue(inProgress.stream().allMatch(t -> t.getStatus() == TicketStatus.IN_PROGRESS));
        }

        @Test
        void filtersByPriority() {
            List<Ticket> urgent = service.listTickets(TicketFilter.byPriority(TicketPriority.URGENT));

            assertEquals(1, urgent.size());
            assertEquals("Export to CSV times out for large reports", urgent.get(0).getSubject());
        }

        @Test
        void filtersByCustomerNameIgnoringCase() {
            List<Ticket> byCustomer = service.listTickets(TicketFilter.byCustomer("aisha rahman"));

            assertEquals(2, byCustomer.size());
        }

        @Test
        void combinesCriteria() {
            List<Ticket> combined = service.listTickets(
                    new TicketFilter(TicketStatus.OPEN, TicketPriority.LOW, "Marco Silva"));

            assertEquals(1, combined.size());
            assertEquals("Request: add dark mode to the mobile app", combined.get(0).getSubject());
        }

        @Test
        void nullFilterReturnsEverything() {
            assertEquals(service.listTickets().size(), service.listTickets(null).size());
            assertEquals(service.listTickets().size(), service.listTickets(TicketFilter.none()).size());
        }

        @Test
        void includesMergedTicketsByDefault() {
            mergeTwoOfTheSeededTickets();

            assertEquals(SampleData.tickets().size(), service.listTickets().size());
            assertEquals(SampleData.tickets().size(), service.listTickets(TicketFilter.none()).size());
            assertEquals(SampleData.tickets().size(), service.listTickets(null).size());
            assertEquals(SampleData.tickets().size(), service.countTickets());
        }

        @Test
        void excludeMergedHidesTombstones() {
            String sourceId = mergeTwoOfTheSeededTickets();

            List<Ticket> tickets = service.listTickets(TicketFilter.excludingMerged());

            assertEquals(SampleData.tickets().size() - 1, tickets.size());
            assertTrue(tickets.stream().noneMatch(Ticket::isMerged));
            assertTrue(tickets.stream().noneMatch(t -> t.getId().equals(sourceId)));
        }

        @Test
        void excludeMergedCombinesWithTheOtherCriteria() {
            mergeTwoOfTheSeededTickets();

            List<Ticket> withMerged = service.listTickets(
                    new TicketFilter(null, null, "Aisha Rahman", false));
            List<Ticket> withoutMerged = service.listTickets(
                    new TicketFilter(null, null, "Aisha Rahman", true));

            assertEquals(2, withMerged.size());
            assertEquals(1, withoutMerged.size());
            assertFalse(withoutMerged.get(0).isMerged());
        }

        /** Merges Aisha Rahman's two seeded tickets and returns the id of the closed duplicate. */
        private String mergeTwoOfTheSeededTickets() {
            List<Ticket> aishas = service.listTickets(TicketFilter.byCustomer("Aisha Rahman"));
            String sourceId = aishas.get(0).getId();
            String targetId = aishas.get(1).getId();
            service.mergeTickets(sourceId, targetId);
            return sourceId;
        }
    }

    @Nested
    class Merging {

        @Test
        @DisplayName("merges a duplicate into the surviving ticket and returns the target")
        void mergesDuplicateIntoTargetAndReturnsTarget() {
            String targetId = createTarget();
            service.changeStatus(targetId, TicketStatus.IN_PROGRESS);
            String sourceId = createDuplicate();
            clock.advance(Duration.ofHours(1));

            Ticket merged = service.mergeTickets(sourceId, targetId);

            assertEquals(targetId, merged.getId());
            assertEquals("Priya Nair", merged.getCustomerName());
            assertEquals("Export to CSV times out for large reports", merged.getSubject());
            assertEquals(TicketStatus.IN_PROGRESS, merged.getStatus());
            assertEquals(TicketPriority.URGENT, merged.getPriority());
            assertEquals(START_TIME, merged.getCreatedAt());
            assertEquals(START_TIME.plus(Duration.ofHours(1)), merged.getUpdatedAt());
            assertFalse(merged.isMerged());
        }

        @Test
        void closesSourceAndRecordsMergeTarget() {
            String targetId = createTarget();
            String sourceId = createDuplicate();
            clock.advance(Duration.ofHours(1));

            service.mergeTickets(sourceId, targetId);

            Ticket source = service.getTicket(sourceId);
            assertEquals(TicketStatus.CLOSED, source.getStatus());
            assertEquals(targetId, source.getMergedIntoId());
            assertEquals(START_TIME.plus(Duration.ofHours(1)), source.getMergedAt());
            assertEquals(START_TIME.plus(Duration.ofHours(1)), source.getUpdatedAt());
            assertEquals(TicketPriority.HIGH, source.getPriority());
        }

        @Test
        void sourceRemainsRetrievableAndNothingIsDeleted() {
            String targetId = createTarget();
            String sourceId = createDuplicate();

            service.mergeTickets(sourceId, targetId);

            assertEquals(sourceId, service.getTicket(sourceId).getId());
            assertEquals(2, service.countTickets());
        }

        @ParameterizedTest
        @CsvSource({
                "LOW, LOW, LOW", "LOW, MEDIUM, MEDIUM", "LOW, HIGH, HIGH", "LOW, URGENT, URGENT",
                "MEDIUM, LOW, MEDIUM", "MEDIUM, MEDIUM, MEDIUM", "MEDIUM, HIGH, HIGH", "MEDIUM, URGENT, URGENT",
                "HIGH, LOW, HIGH", "HIGH, MEDIUM, HIGH", "HIGH, HIGH, HIGH", "HIGH, URGENT, URGENT",
                "URGENT, LOW, URGENT", "URGENT, MEDIUM, URGENT", "URGENT, HIGH, URGENT", "URGENT, URGENT, URGENT"
        })
        void raisesTargetPriorityToTheHigherOfTheTwoAndNeverLowersIt(TicketPriority targetPriority,
                                                                     TicketPriority sourcePriority,
                                                                     TicketPriority expected) {
            String targetId = createTarget(targetPriority);
            String sourceId = createDuplicate(sourcePriority);

            assertEquals(expected, service.mergeTickets(sourceId, targetId).getPriority());
        }

        @Test
        void appendsSourceSubjectAndDescriptionToTarget() {
            String targetId = createTarget();
            String sourceId = createDuplicate();
            String original = service.getTicket(targetId).getDescription();
            clock.advance(Duration.ofHours(1));

            String description = service.mergeTickets(sourceId, targetId).getDescription();

            assertTrue(description.startsWith(original), description);
            assertTrue(description.contains("--- Merged from " + sourceId
                    + " (Priya Nair, 2026-09-01T10:00:00Z) ---"), description);
            assertTrue(description.contains("Export to CSV still times out"), description);
            assertTrue(description.contains("Retried this morning on the 80k-row report"), description);
        }

        @Test
        void annotatesTheSourceWithItsMergeTarget() {
            String targetId = createTarget();
            String sourceId = createDuplicate();

            service.mergeTickets(sourceId, targetId);

            assertTrue(service.getTicket(sourceId).getDescription()
                    .contains("--- Merged into " + targetId + " on 2026-09-01T09:00:00Z ---"));
        }

        @EnumSource(value = TicketStatus.class, names = "CLOSED", mode = Mode.EXCLUDE)
        @ParameterizedTest
        void leavesTargetStatusCustomerAndSubjectUnchanged(TicketStatus targetStatus) {
            String targetId = createTarget();
            if (targetStatus != TicketStatus.OPEN) {
                service.changeStatus(targetId, targetStatus);
            }
            String sourceId = createDuplicate();

            Ticket merged = service.mergeTickets(sourceId, targetId);

            assertEquals(targetStatus, merged.getStatus());
            assertEquals("Priya Nair", merged.getCustomerName());
            assertEquals("Export to CSV times out for large reports", merged.getSubject());
        }

        @EnumSource(value = TicketStatus.class, names = "CLOSED", mode = Mode.EXCLUDE)
        @ParameterizedTest
        void mergesSourceInEveryNonClosedStatus(TicketStatus sourceStatus) {
            String targetId = createTarget();
            String sourceId = createDuplicate();
            if (sourceStatus != TicketStatus.OPEN) {
                service.changeStatus(sourceId, sourceStatus);
            }

            service.mergeTickets(sourceId, targetId);

            assertEquals(TicketStatus.CLOSED, service.getTicket(sourceId).getStatus());
        }

        @Test
        @DisplayName("merges an already closed source without an illegal transition")
        void mergesAlreadyClosedSourceWithoutStatusTransitionError() {
            String targetId = createTarget();
            String sourceId = createDuplicate();
            service.changeStatus(sourceId, TicketStatus.CLOSED);

            Ticket merged = service.mergeTickets(sourceId, targetId);

            assertEquals(targetId, merged.getId());
            assertEquals(TicketStatus.CLOSED, service.getTicket(sourceId).getStatus());
            assertEquals(targetId, service.getTicket(sourceId).getMergedIntoId());
        }

        @Test
        void rejectsSelfMerge() {
            String targetId = createTarget();

            TicketMergeException error = assertThrows(TicketMergeException.class,
                    () -> service.mergeTickets(targetId, targetId));
            assertTrue(error.getMessage().contains(targetId), error.getMessage());
            assertTrue(error.getMessage().contains("itself"), error.getMessage());
        }

        @Test
        @DisplayName("reports a self-merge before checking that the ticket exists")
        void rejectsSelfMergeOfUnknownIdBeforeLookup() {
            assertThrows(TicketMergeException.class, () -> service.mergeTickets("TCK-9999", "TCK-9999"));
        }

        @Test
        void rejectsSelfMergeAfterTrimmingIds() {
            String targetId = createTarget();

            assertThrows(TicketMergeException.class,
                    () -> service.mergeTickets("  " + targetId + "  ", targetId));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejectsBlankIds(String blank) {
            String targetId = createTarget();

            assertThrows(ValidationException.class, () -> service.mergeTickets(blank, targetId));
            assertThrows(ValidationException.class, () -> service.mergeTickets(targetId, blank));
        }

        @Test
        void rejectsNullIds() {
            String targetId = createTarget();

            assertThrows(ValidationException.class, () -> service.mergeTickets(null, targetId));
            assertThrows(ValidationException.class, () -> service.mergeTickets(targetId, null));
        }

        @Test
        void throwsNotFoundForUnknownSourceOrTarget() {
            String targetId = createTarget();
            String sourceId = createDuplicate();

            assertThrows(TicketNotFoundException.class, () -> service.mergeTickets("TCK-9999", targetId));
            assertThrows(TicketNotFoundException.class, () -> service.mergeTickets(sourceId, "TCK-9999"));
        }

        @Test
        void reportsTheSourceWhenBothIdsAreUnknown() {
            TicketNotFoundException error = assertThrows(TicketNotFoundException.class,
                    () -> service.mergeTickets("TCK-8888", "TCK-9999"));

            assertTrue(error.getMessage().contains("TCK-8888"), error.getMessage());
        }

        @Test
        void rejectsMergingAnAlreadyMergedSource() {
            String targetId = createTarget();
            String sourceId = createDuplicate();
            service.mergeTickets(sourceId, targetId);
            String otherTargetId = createTarget();

            TicketMergeException error = assertThrows(TicketMergeException.class,
                    () -> service.mergeTickets(sourceId, otherTargetId));
            assertTrue(error.getMessage().contains(sourceId), error.getMessage());
            assertTrue(error.getMessage().contains(targetId), error.getMessage());
        }

        @Test
        void rejectsMergingIntoAnAlreadyMergedTarget() {
            String survivorId = createTarget();
            String middleId = createDuplicate();
            service.mergeTickets(middleId, survivorId);
            String newDuplicateId = createDuplicate();

            TicketMergeException error = assertThrows(TicketMergeException.class,
                    () -> service.mergeTickets(newDuplicateId, middleId));
            assertTrue(error.getMessage().contains(middleId), error.getMessage());
            assertTrue(error.getMessage().contains(survivorId), error.getMessage());
        }

        @Test
        void rejectsMergingIntoAClosedTarget() {
            String targetId = createTarget();
            service.changeStatus(targetId, TicketStatus.CLOSED);
            String sourceId = createDuplicate();

            TicketMergeException error = assertThrows(TicketMergeException.class,
                    () -> service.mergeTickets(sourceId, targetId));
            assertTrue(error.getMessage().contains(targetId), error.getMessage());
        }

        @Test
        void rejectsMergingTicketsOfDifferentCustomers() {
            String targetId = createTarget();
            String sourceId = service.createTicket(new CreateTicketRequest(
                    "Marco Silva",
                    "Export to CSV still times out",
                    "Our export of the monthly report also times out.",
                    TicketPriority.HIGH)).getId();

            TicketMergeException error = assertThrows(TicketMergeException.class,
                    () -> service.mergeTickets(sourceId, targetId));
            assertTrue(error.getMessage().contains("Marco Silva"), error.getMessage());
            assertTrue(error.getMessage().contains("Priya Nair"), error.getMessage());
        }

        @Test
        void truncatesTheAppendedBlockWhenTheCombinedDescriptionExceedsTheLimit() {
            String longDescription = "x".repeat(4900);
            String targetId = service.createTicket(new CreateTicketRequest(
                    "Priya Nair", "Export to CSV times out for large reports",
                    longDescription, TicketPriority.URGENT)).getId();
            String sourceId = service.createTicket(new CreateTicketRequest(
                    "Priya Nair", "Export to CSV still times out",
                    "y".repeat(400), TicketPriority.HIGH)).getId();

            String description = service.mergeTickets(sourceId, targetId).getDescription();

            assertEquals(TicketValidator.MAX_DESCRIPTION_LENGTH, description.length());
            assertTrue(description.startsWith(longDescription));
            assertTrue(description.endsWith("… [truncated]"));
        }

        @Test
        void leavesBothTicketsUntouchedWhenValidationFails() {
            String targetId = createTarget();
            service.changeStatus(targetId, TicketStatus.CLOSED);
            String sourceId = createDuplicate();
            Ticket targetBefore = service.getTicket(targetId);
            Ticket sourceBefore = service.getTicket(sourceId);

            assertThrows(TicketMergeException.class, () -> service.mergeTickets(sourceId, targetId));

            assertTicketUnchanged(targetBefore, service.getTicket(targetId));
            assertTicketUnchanged(sourceBefore, service.getTicket(sourceId));
        }

        @Test
        void leavesTheSourceUntouchedWhenTheTargetDoesNotExist() {
            String sourceId = createDuplicate();
            Ticket before = service.getTicket(sourceId);

            assertThrows(TicketNotFoundException.class, () -> service.mergeTickets(sourceId, "TCK-9999"));

            assertTicketUnchanged(before, service.getTicket(sourceId));
        }

        @Test
        void mergeIsNotCommutative() {
            String lowId = service.createTicket(new CreateTicketRequest(
                    "Marco Silva", "Add dark mode", "Please add a dark theme.", TicketPriority.LOW)).getId();
            String mediumId = service.createTicket(new CreateTicketRequest(
                    "Marco Silva", "Dark mode request", "The dashboard is hard to read at night.",
                    TicketPriority.MEDIUM)).getId();

            Ticket merged = service.mergeTickets(lowId, mediumId);

            assertEquals(mediumId, merged.getId());
            assertEquals(TicketPriority.MEDIUM, merged.getPriority());
            assertTrue(service.getTicket(lowId).isMerged());
            assertFalse(service.getTicket(mediumId).isMerged());
        }

        private String createTarget() {
            return createTarget(TicketPriority.URGENT);
        }

        private String createTarget(TicketPriority priority) {
            return service.createTicket(new CreateTicketRequest(
                    "Priya Nair",
                    "Export to CSV times out for large reports",
                    "Exporting the monthly usage report (about 80k rows) fails after roughly 60 seconds.",
                    priority)).getId();
        }

        private String createDuplicate() {
            return createDuplicate(TicketPriority.HIGH);
        }

        private String createDuplicate(TicketPriority priority) {
            return service.createTicket(new CreateTicketRequest(
                    "Priya Nair",
                    "Export to CSV still times out",
                    "Retried this morning on the 80k-row report, same gateway timeout at ~60s.",
                    priority)).getId();
        }

        private void assertTicketUnchanged(Ticket before, Ticket after) {
            assertEquals(before.getStatus(), after.getStatus());
            assertEquals(before.getPriority(), after.getPriority());
            assertEquals(before.getDescription(), after.getDescription());
            assertEquals(before.getSubject(), after.getSubject());
            assertEquals(before.getCustomerName(), after.getCustomerName());
            assertEquals(before.getUpdatedAt(), after.getUpdatedAt());
            assertEquals(before.getMergedIntoId(), after.getMergedIntoId());
            assertEquals(before.getMergedAt(), after.getMergedAt());
        }
    }

    @Test
    void deletesTicket() {
        String id = service.createTicket(validRequest()).getId();

        assertTrue(service.deleteTicket(id));
        assertFalse(service.deleteTicket(id));
        assertEquals(0, service.countTickets());
        assertThrows(ValidationException.class, () -> service.deleteTicket(" "));
    }

    private static CreateTicketRequest validRequest() {
        return new CreateTicketRequest(
                "Aisha Rahman",
                "Cannot log in after password reset",
                "The new password is rejected on the web login.");
    }

    /** Clock whose instant is advanced by the test, so timestamps are deterministic. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
