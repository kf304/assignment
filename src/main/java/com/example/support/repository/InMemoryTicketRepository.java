package com.example.support.repository;

import com.example.support.model.Ticket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store. Tickets are copied on the way in and out so the
 * stored state can only change through {@link #save(Ticket)}.
 */
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<String, Ticket> ticketsById = new ConcurrentHashMap<>();
    private final Object batchLock = new Object();

    @Override
    public Ticket save(Ticket ticket) {
        ticketsById.put(ticket.getId(), ticket.copy());
        return ticket.copy();
    }

    /**
     * Validates and copies the whole batch before publishing any of it, so a rejected
     * batch writes nothing and two concurrent batches cannot interleave. Single-key
     * {@link #save(Ticket)} calls are still not serialised against a batch.
     */
    @Override
    public List<Ticket> saveAll(List<Ticket> tickets) {
        Objects.requireNonNull(tickets, "tickets");
        List<Ticket> toStore = new ArrayList<>(tickets.size());
        for (Ticket ticket : tickets) {
            toStore.add(Objects.requireNonNull(ticket, "tickets must not contain null").copy());
        }
        List<Ticket> saved = new ArrayList<>(toStore.size());
        synchronized (batchLock) {
            for (Ticket ticket : toStore) {
                ticketsById.put(ticket.getId(), ticket);
                saved.add(ticket.copy());
            }
        }
        return saved;
    }

    @Override
    public Optional<Ticket> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ticketsById.get(id)).map(Ticket::copy);
    }

    @Override
    public List<Ticket> findAll() {
        List<Ticket> tickets = new ArrayList<>(ticketsById.size());
        for (Ticket ticket : ticketsById.values()) {
            tickets.add(ticket.copy());
        }
        tickets.sort(Comparator.comparing(Ticket::getCreatedAt).reversed()
                .thenComparing(Ticket::getId));
        return tickets;
    }

    @Override
    public boolean deleteById(String id) {
        return id != null && ticketsById.remove(id) != null;
    }

    @Override
    public long count() {
        return ticketsById.size();
    }
}
