package com.example.support.repository;

import com.example.support.model.Ticket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store. Tickets are copied on the way in and out so the
 * stored state can only change through {@link #save(Ticket)}.
 */
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<String, Ticket> ticketsById = new ConcurrentHashMap<>();

    @Override
    public Ticket save(Ticket ticket) {
        ticketsById.put(ticket.getId(), ticket.copy());
        return ticket.copy();
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
