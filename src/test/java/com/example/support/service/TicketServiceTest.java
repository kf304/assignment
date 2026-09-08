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
