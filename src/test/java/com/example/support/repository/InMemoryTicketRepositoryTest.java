package com.example.support.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.support.model.Ticket;
import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryTicketRepositoryTest {

    private static final Instant BASE_TIME = Instant.parse("2026-09-01T09:00:00Z");

    private InMemoryTicketRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTicketRepository();
    }

    @Test
    void savesAndFindsTicketById() {
        repository.save(ticket("TCK-1000", BASE_TIME));

        Optional<Ticket> found = repository.findById("TCK-1000");

        assertTrue(found.isPresent());
        assertEquals("TCK-1000", found.get().getId());
        assertEquals(1, repository.count());
    }

    @Test
    void returnsEmptyForUnknownOrNullId() {
        assertTrue(repository.findById("TCK-4242").isEmpty());
        assertTrue(repository.findById(null).isEmpty());
    }

    @Test
    void listsTicketsNewestFirst() {
        repository.save(ticket("TCK-1000", BASE_TIME));
        repository.save(ticket("TCK-1001", BASE_TIME.plusSeconds(60)));
        repository.save(ticket("TCK-1002", BASE_TIME.plusSeconds(120)));

        List<String> ids = repository.findAll().stream().map(Ticket::getId).toList();

        assertEquals(List.of("TCK-1002", "TCK-1001", "TCK-1000"), ids);
    }

    @Test
    void storedTicketIsNotAffectedByMutatingTheReturnedCopy() {
        repository.save(ticket("TCK-1000", BASE_TIME));

        Ticket detached = repository.findById("TCK-1000").orElseThrow();
        detached.setSubject("Rewritten outside the repository");

        assertEquals("Printer not detected", repository.findById("TCK-1000").orElseThrow().getSubject());
    }

    @Test
    void deletesExistingTicketOnly() {
        repository.save(ticket("TCK-1000", BASE_TIME));

        assertTrue(repository.deleteById("TCK-1000"));
        assertFalse(repository.deleteById("TCK-1000"));
        assertFalse(repository.deleteById(null));
        assertEquals(0, repository.count());
    }

    @Test
    void saveAllPersistsEveryTicketInTheBatch() {
        List<Ticket> saved = repository.saveAll(List.of(
                ticket("TCK-1002", BASE_TIME),
                ticket("TCK-1005", BASE_TIME.plusSeconds(60))));

        assertEquals(2, saved.size());
        assertEquals(2, repository.count());
        assertTrue(repository.findById("TCK-1002").isPresent());
        assertTrue(repository.findById("TCK-1005").isPresent());
    }

    @Test
    void saveAllOverwritesExistingTickets() {
        repository.save(ticket("TCK-1002", BASE_TIME));
        Ticket updated = ticket("TCK-1002", BASE_TIME);
        updated.setSubject("Export to CSV times out for large reports");

        repository.saveAll(List.of(updated));

        assertEquals("Export to CSV times out for large reports",
                repository.findById("TCK-1002").orElseThrow().getSubject());
        assertEquals(1, repository.count());
    }

    @Test
    void saveAllStoresDefensiveCopies() {
        Ticket target = ticket("TCK-1002", BASE_TIME);
        repository.saveAll(List.of(target));

        target.setSubject("Rewritten outside the repository");

        assertEquals("Printer not detected", repository.findById("TCK-1002").orElseThrow().getSubject());
    }

    @Test
    void saveAllReturnsDetachedCopies() {
        Ticket returned = repository.saveAll(List.of(ticket("TCK-1002", BASE_TIME))).get(0);

        returned.setSubject("Rewritten outside the repository");

        assertEquals("Printer not detected", repository.findById("TCK-1002").orElseThrow().getSubject());
    }

    @Test
    void saveAllAcceptsAnEmptyBatch() {
        assertEquals(List.of(), repository.saveAll(List.of()));
        assertEquals(0, repository.count());
    }

    @Test
    void saveAllRejectsNullBatchOrNullElement() {
        assertThrows(NullPointerException.class, () -> repository.saveAll(null));
        assertThrows(NullPointerException.class,
                () -> repository.saveAll(Arrays.asList(ticket("TCK-1002", BASE_TIME), null)));
    }

    @Test
    void saveAllWritesNothingWhenTheBatchIsRejected() {
        List<Ticket> batch = new ArrayList<>();
        batch.add(ticket("TCK-1002", BASE_TIME));
        batch.add(null);

        assertThrows(NullPointerException.class, () -> repository.saveAll(batch));

        assertEquals(0, repository.count());
        assertTrue(repository.findById("TCK-1002").isEmpty());
    }

    @Test
    void roundTripsMergeState() {
        Ticket source = ticket("TCK-1005", BASE_TIME);
        source.markMergedInto("TCK-1002", BASE_TIME.plusSeconds(600));
        source.setStatus(TicketStatus.CLOSED);

        repository.saveAll(List.of(source));

        Ticket stored = repository.findById("TCK-1005").orElseThrow();
        assertTrue(stored.isMerged());
        assertEquals("TCK-1002", stored.getMergedIntoId());
        assertEquals(BASE_TIME.plusSeconds(600), stored.getMergedAt());
        assertEquals(TicketStatus.CLOSED, stored.getStatus());
    }

    @Test
    void mutatingAReturnedMergedTicketDoesNotAffectTheStore() {
        Ticket source = ticket("TCK-1005", BASE_TIME);
        source.markMergedInto("TCK-1002", BASE_TIME);
        repository.save(source);

        repository.findById("TCK-1005").orElseThrow().markMergedInto("TCK-9999", BASE_TIME);

        assertEquals("TCK-1002", repository.findById("TCK-1005").orElseThrow().getMergedIntoId());
    }

    @Test
    void unmergedTicketsRoundTripWithoutMergeState() {
        repository.save(ticket("TCK-1000", BASE_TIME));

        Ticket stored = repository.findById("TCK-1000").orElseThrow();
        assertFalse(stored.isMerged());
        assertNull(stored.getMergedIntoId());
        assertNull(stored.getMergedAt());
    }

    private static Ticket ticket(String id, Instant createdAt) {
        return new Ticket(id,
                "Dana Weiss",
                "Printer not detected",
                "The office printer stopped appearing in the device list after the last update.",
                TicketStatus.OPEN,
                TicketPriority.MEDIUM,
                createdAt);
    }
}
