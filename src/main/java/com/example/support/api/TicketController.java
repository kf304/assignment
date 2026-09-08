package com.example.support.api;

import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.TicketFilter;
import com.example.support.dto.UpdateTicketRequest;
import com.example.support.exception.InvalidStatusTransitionException;
import com.example.support.exception.TicketNotFoundException;
import com.example.support.exception.ValidationException;
import com.example.support.model.Ticket;
import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;
import com.example.support.service.TicketService;
import java.util.List;
import java.util.Objects;

/**
 * Entry point for callers. It translates service exceptions into
 * {@link ApiResponse} status codes so no exception escapes the boundary.
 */
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = Objects.requireNonNull(ticketService, "ticketService");
    }

    public ApiResponse<Ticket> createTicket(CreateTicketRequest request) {
        try {
            return ApiResponse.created(ticketService.createTicket(request));
        } catch (ValidationException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    public ApiResponse<Ticket> getTicket(String ticketId) {
        try {
            return ApiResponse.ok(ticketService.getTicket(ticketId));
        } catch (TicketNotFoundException e) {
            return ApiResponse.notFound(e.getMessage());
        } catch (ValidationException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    public ApiResponse<Ticket> updateTicket(String ticketId, UpdateTicketRequest request) {
        try {
            return ApiResponse.ok(ticketService.updateTicket(ticketId, request));
        } catch (TicketNotFoundException e) {
            return ApiResponse.notFound(e.getMessage());
        } catch (ValidationException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    public ApiResponse<Ticket> changeStatus(String ticketId, TicketStatus newStatus) {
        try {
            return ApiResponse.ok(ticketService.changeStatus(ticketId, newStatus));
        } catch (TicketNotFoundException e) {
            return ApiResponse.notFound(e.getMessage());
        } catch (InvalidStatusTransitionException e) {
            return ApiResponse.conflict(e.getMessage());
        } catch (ValidationException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    public ApiResponse<Ticket> changePriority(String ticketId, TicketPriority newPriority) {
        try {
            return ApiResponse.ok(ticketService.changePriority(ticketId, newPriority));
        } catch (TicketNotFoundException e) {
            return ApiResponse.notFound(e.getMessage());
        } catch (ValidationException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    public ApiResponse<List<Ticket>> listTickets() {
        return ApiResponse.ok(ticketService.listTickets());
    }

    public ApiResponse<List<Ticket>> listTickets(TicketFilter filter) {
        return ApiResponse.ok(ticketService.listTickets(filter));
    }

    public ApiResponse<Void> deleteTicket(String ticketId) {
        try {
            if (!ticketService.deleteTicket(ticketId)) {
                return ApiResponse.notFound("Ticket not found: " + ticketId);
            }
            return ApiResponse.noContent();
        } catch (ValidationException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }
}
