package com.example.support.repository;

import com.example.support.model.Ticket;
import java.util.List;
import java.util.Optional;

/** Storage abstraction for tickets. */
public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(String id);

    /** All tickets, newest first. */
    List<Ticket> findAll();

    boolean deleteById(String id);

    long count();
}
