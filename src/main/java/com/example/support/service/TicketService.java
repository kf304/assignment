package com.example.support.service;

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
import com.example.support.repository.TicketRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Business logic for support tickets: validation, lifecycle rules and querying.
 * Persistence is delegated to a {@link TicketRepository}.
 */
public class TicketService {

    static final TicketPriority DEFAULT_PRIORITY = TicketPriority.MEDIUM;
    static final TicketStatus INITIAL_STATUS = TicketStatus.OPEN;

    private final TicketRepository repository;
    private final TicketIdGenerator idGenerator;
    private final Clock clock;

    public TicketService(TicketRepository repository) {
        this(repository, new SequentialTicketIdGenerator(), Clock.systemUTC());
    }

    public TicketService(TicketRepository repository, TicketIdGenerator idGenerator, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Ticket createTicket(CreateTicketRequest request) {
        if (request == null) {
            throw new ValidationException("request must not be null");
        }
        Instant now = clock.instant();
        Ticket ticket = new Ticket(
                idGenerator.nextId(),
                TicketValidator.requireCustomerName(request.customerName()),
                TicketValidator.requireSubject(request.subject()),
                TicketValidator.requireDescription(request.description()),
                INITIAL_STATUS,
                request.priority() == null ? DEFAULT_PRIORITY : request.priority(),
                now);
        return repository.save(ticket);
    }

    public Ticket getTicket(String ticketId) {
        String id = TicketValidator.requireTicketId(ticketId);
        return repository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    public Ticket updateTicket(String ticketId, UpdateTicketRequest request) {
        if (request == null) {
            throw new ValidationException("request must not be null");
        }
        if (request.isEmpty()) {
            throw new ValidationException("update request must change at least one field");
        }
        Ticket ticket = getTicket(ticketId);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ValidationException("Closed ticket " + ticket.getId() + " cannot be edited");
        }
        if (request.customerName() != null) {
            ticket.setCustomerName(TicketValidator.requireCustomerName(request.customerName()));
        }
        if (request.subject() != null) {
            ticket.setSubject(TicketValidator.requireSubject(request.subject()));
        }
        if (request.description() != null) {
            ticket.setDescription(TicketValidator.requireDescription(request.description()));
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        ticket.setUpdatedAt(clock.instant());
        return repository.save(ticket);
    }

    public Ticket changeStatus(String ticketId, TicketStatus newStatus) {
        if (newStatus == null) {
            throw new ValidationException("status must not be null");
        }
        Ticket ticket = getTicket(ticketId);
        if (!ticket.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(ticket.getStatus(), newStatus);
        }
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(clock.instant());
        return repository.save(ticket);
    }

    public Ticket changePriority(String ticketId, TicketPriority newPriority) {
        if (newPriority == null) {
            throw new ValidationException("priority must not be null");
        }
        return updateTicket(ticketId, new UpdateTicketRequest(null, null, null, newPriority));
    }

    /**
     * Merges the duplicate {@code sourceId} into the surviving {@code targetId}: the target
     * absorbs the duplicate's text and the higher of the two priorities, and the source is
     * closed and permanently marked as merged. Both tickets are written as one operation.
     *
     * <p>This deliberately bypasses {@link #updateTicket} and {@link #changeStatus}: a merge
     * is not a lifecycle transition, an already closed source must stay mergeable and the
     * target's status is not changed at all.
     *
     * @return the merged target
     * @throws ValidationException     if either id is blank
     * @throws TicketMergeException    if the tickets cannot be merged
     * @throws TicketNotFoundException if either ticket does not exist
     */
    public Ticket mergeTickets(String sourceId, String targetId) {
        String source = TicketValidator.requireTicketId(sourceId);
        String target = TicketValidator.requireTicketId(targetId);
        if (source.equals(target)) {
            throw new TicketMergeException("Cannot merge ticket " + source + " into itself");
        }
        Ticket sourceTicket = getTicket(source);
        Ticket targetTicket = getTicket(target);
        TicketMerger.requireMergeable(sourceTicket, targetTicket);

        Instant now = clock.instant();
        // The target absorbs the source's text first, before the source is annotated.
        Ticket mergedTarget = TicketMerger.mergeInto(targetTicket, sourceTicket, now);
        Ticket tombstone = TicketMerger.asTombstone(sourceTicket, target, now);
        return repository.saveAll(List.of(mergedTarget, tombstone)).get(0);
    }

    /** All tickets, newest first. */
    public List<Ticket> listTickets() {
        return repository.findAll();
    }

    /** Tickets matching every non-null criterion of the filter, newest first. */
    public List<Ticket> listTickets(TicketFilter filter) {
        if (filter == null) {
            return listTickets();
        }
        return repository.findAll().stream()
                .filter(ticket -> filter.status() == null || ticket.getStatus() == filter.status())
                .filter(ticket -> filter.priority() == null || ticket.getPriority() == filter.priority())
                .filter(ticket -> filter.customerName() == null
                        || ticket.getCustomerName().equalsIgnoreCase(filter.customerName().trim()))
                .filter(ticket -> !filter.excludeMerged() || !ticket.isMerged())
                .toList();
    }

    public boolean deleteTicket(String ticketId) {
        return repository.deleteById(TicketValidator.requireTicketId(ticketId));
    }

    public long countTickets() {
        return repository.count();
    }
}
