package com.example.support.repository;

import com.example.support.model.Ticket;
import java.util.List;
import java.util.Optional;

/** Storage abstraction for tickets. */
public interface TicketRepository {

    Ticket save(Ticket ticket);

    /**
     * Saves several tickets as one operation, so a multi-ticket change such as a merge
     * cannot be left half-written by a rejected batch.
     *
     * @return detached copies of the saved tickets, in the order supplied
     */
    List<Ticket> saveAll(List<Ticket> tickets);

    Optional<Ticket> findById(String id);

    /** All tickets, newest first. */
    List<Ticket> findAll();

    boolean deleteById(String id);

    long count();
}
