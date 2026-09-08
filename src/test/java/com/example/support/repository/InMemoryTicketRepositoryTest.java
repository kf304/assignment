package com.example.support.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.support.model.Ticket;
import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;
import java.time.Instant;
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
