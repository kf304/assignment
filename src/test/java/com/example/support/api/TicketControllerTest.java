package com.example.support.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.support.SampleData;
import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.TicketFilter;
import com.example.support.dto.UpdateTicketRequest;
import com.example.support.model.Ticket;
import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;
import com.example.support.repository.InMemoryTicketRepository;
import com.example.support.service.TicketService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketControllerTest {

    private TicketController controller;

    @BeforeEach
    void setUp() {
        TicketService service = new TicketService(new InMemoryTicketRepository());
        controller = new TicketController(service);
    }

    @Test
    void createReturns201WithTicket() {
        ApiResponse<Ticket> response = controller.createTicket(new CreateTicketRequest(
                "Tomas Berg",
                "Invoice #4471 shows the wrong VAT rate",
                "The invoice applies 25% VAT instead of 19%.",
                TicketPriority.MEDIUM));

        assertEquals(201, response.statusCode());
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertNull(response.errorMessage());
    }

    @Test
    void createReturns400ForInvalidInput() {
        ApiResponse<Ticket> response = controller.createTicket(
                new CreateTicketRequest("Tomas Berg", "  ", "Description"));

        assertEquals(400, response.statusCode());
        assertFalse(response.isSuccessful());
        assertTrue(response.errorMessage().contains("subject"));
    }

    @Test
    void getReturns200Or404() {
        String id = controller.createTicket(validRequest()).body().getId();

        assertEquals(200, controller.getTicket(id).statusCode());
        assertEquals(404, controller.getTicket("TCK-9999").statusCode());
        assertEquals(400, controller.getTicket("   ").statusCode());
    }

    @Test
    void updateReturnsUpdatedTicket() {
        String id = controller.createTicket(validRequest()).body().getId();

        ApiResponse<Ticket> response = controller.updateTicket(id,
                new UpdateTicketRequest(null, null, "Additional detail from the customer.", null));

        assertEquals(200, response.statusCode());
        assertEquals("Additional detail from the customer.", response.body().getDescription());
    }

    @Test
    void updateReturns404ForUnknownTicketAnd400ForEmptyRequest() {
        String id = controller.createTicket(validRequest()).body().getId();

        assertEquals(404, controller.updateTicket("TCK-9999",
                new UpdateTicketRequest(null, "Subject", null, null)).statusCode());
        assertEquals(400, controller.updateTicket(id,
                new UpdateTicketRequest(null, null, null, null)).statusCode());
    }

    @Test
    @DisplayName("an illegal status change reports 409 rather than throwing")
    void statusChangeReturns409ForIllegalTransition() {
        String id = controller.createTicket(validRequest()).body().getId();
        controller.changeStatus(id, TicketStatus.CLOSED);

        ApiResponse<Ticket> response = controller.changeStatus(id, TicketStatus.RESOLVED);

        assertEquals(409, response.statusCode());
        assertNull(response.body());
    }

    @Test
    void statusChangeReturns200OnSuccess() {
        String id = controller.createTicket(validRequest()).body().getId();

        ApiResponse<Ticket> response = controller.changeStatus(id, TicketStatus.IN_PROGRESS);

        assertEquals(200, response.statusCode());
        assertEquals(TicketStatus.IN_PROGRESS, response.body().getStatus());
    }

    @Test
    void changePriorityReturns200() {
        String id = controller.createTicket(validRequest()).body().getId();

        ApiResponse<Ticket> response = controller.changePriority(id, TicketPriority.HIGH);

        assertEquals(200, response.statusCode());
        assertEquals(TicketPriority.HIGH, response.body().getPriority());
    }

    @Test
    void listReturnsSeededTicketsAndSupportsFilters() {
        TicketService service = new TicketService(new InMemoryTicketRepository());
        SampleData.seed(service);
        TicketController seeded = new TicketController(service);

        ApiResponse<List<Ticket>> all = seeded.listTickets();
        ApiResponse<List<Ticket>> open = seeded.listTickets(TicketFilter.byStatus(TicketStatus.OPEN));

        assertEquals(200, all.statusCode());
        assertEquals(SampleData.tickets().size(), all.body().size());
        assertEquals(2, open.body().size());
    }

    @Test
    void deleteReturns204ThenNotFound() {
        String id = controller.createTicket(validRequest()).body().getId();

        assertEquals(204, controller.deleteTicket(id).statusCode());
        assertEquals(404, controller.deleteTicket(id).statusCode());
        assertEquals(400, controller.deleteTicket(null).statusCode());
    }

    @Test
    void mergeReturns200WithTheMergedTarget() {
        String targetId = controller.createTicket(validRequest()).body().getId();
        String sourceId = controller.createTicket(duplicateRequest()).body().getId();

        ApiResponse<Ticket> response = controller.mergeTickets(sourceId, targetId);

        assertEquals(200, response.statusCode());
        assertTrue(response.isSuccessful());
        assertNull(response.errorMessage());
        assertEquals(targetId, response.body().getId());
        assertFalse(response.body().isMerged());
        assertEquals(TicketStatus.CLOSED, controller.getTicket(sourceId).body().getStatus());
        assertEquals(targetId, controller.getTicket(sourceId).body().getMergedIntoId());
    }

    @Test
    void mergeReturns404ForUnknownSourceOrTarget() {
        String targetId = controller.createTicket(validRequest()).body().getId();
        String sourceId = controller.createTicket(duplicateRequest()).body().getId();

        assertEquals(404, controller.mergeTickets("TCK-9999", targetId).statusCode());
        assertEquals(404, controller.mergeTickets(sourceId, "TCK-9999").statusCode());
        assertNull(controller.mergeTickets("TCK-9999", targetId).body());
    }

    @Test
    void mergeReturns400ForBlankOrNullIds() {
        String targetId = controller.createTicket(validRequest()).body().getId();

        assertEquals(400, controller.mergeTickets("   ", targetId).statusCode());
        assertEquals(400, controller.mergeTickets(targetId, "   ").statusCode());
        assertEquals(400, controller.mergeTickets(null, targetId).statusCode());
        assertEquals(400, controller.mergeTickets(targetId, null).statusCode());
    }

    @Test
    @DisplayName("merging a ticket with itself reports 409 rather than throwing")
    void mergeReturns409ForSelfMerge() {
        String targetId = controller.createTicket(validRequest()).body().getId();

        ApiResponse<Ticket> response = controller.mergeTickets(targetId, targetId);

        assertEquals(409, response.statusCode());
        assertNull(response.body());
        assertTrue(response.errorMessage().contains("itself"), response.errorMessage());
    }

    @Test
    void mergeReturns409ForAnAlreadyMergedSource() {
        String targetId = controller.createTicket(validRequest()).body().getId();
        String sourceId = controller.createTicket(duplicateRequest()).body().getId();
        controller.mergeTickets(sourceId, targetId);
        String otherTargetId = controller.createTicket(validRequest()).body().getId();

        ApiResponse<Ticket> response = controller.mergeTickets(sourceId, otherTargetId);

        assertEquals(409, response.statusCode());
        assertTrue(response.errorMessage().contains("already been merged"), response.errorMessage());
    }

    @Test
    void mergeReturns409ForAnAlreadyMergedTarget() {
        String survivorId = controller.createTicket(validRequest()).body().getId();
        String middleId = controller.createTicket(duplicateRequest()).body().getId();
        controller.mergeTickets(middleId, survivorId);
        String newDuplicateId = controller.createTicket(duplicateRequest()).body().getId();

        ApiResponse<Ticket> response = controller.mergeTickets(newDuplicateId, middleId);

        assertEquals(409, response.statusCode());
        assertTrue(response.errorMessage().contains(survivorId), response.errorMessage());
    }

    @Test
    void mergeReturns409ForAClosedTarget() {
        String targetId = controller.createTicket(validRequest()).body().getId();
        controller.changeStatus(targetId, TicketStatus.CLOSED);
        String sourceId = controller.createTicket(duplicateRequest()).body().getId();

        ApiResponse<Ticket> response = controller.mergeTickets(sourceId, targetId);

        assertEquals(409, response.statusCode());
        assertTrue(response.errorMessage().contains(targetId), response.errorMessage());
    }

    @Test
    void mergeReturns409ForTicketsOfDifferentCustomers() {
        String targetId = controller.createTicket(validRequest()).body().getId();
        String sourceId = controller.createTicket(new CreateTicketRequest(
                "Marco Silva",
                "Export to CSV still times out",
                "Our monthly export times out as well.")).body().getId();

        ApiResponse<Ticket> response = controller.mergeTickets(sourceId, targetId);

        assertEquals(409, response.statusCode());
        assertTrue(response.errorMessage().contains("different customers"), response.errorMessage());
    }

    @Test
    @DisplayName("no merge failure escapes the controller as an exception")
    void mergeNeverThrows() {
        String targetId = controller.createTicket(validRequest()).body().getId();

        assertNotEquals(0, controller.mergeTickets(null, null).statusCode());
        assertNotEquals(0, controller.mergeTickets("", "").statusCode());
        assertNotEquals(0, controller.mergeTickets(targetId, targetId).statusCode());
        assertNotEquals(0, controller.mergeTickets("TCK-9999", "TCK-8888").statusCode());
        assertNotEquals(0, controller.mergeTickets(targetId, "TCK-9999").statusCode());
    }

    @Test
    void listCanExcludeMergedTickets() {
        String targetId = controller.createTicket(validRequest()).body().getId();
        String sourceId = controller.createTicket(duplicateRequest()).body().getId();
        controller.mergeTickets(sourceId, targetId);

        assertEquals(2, controller.listTickets().body().size());
        assertEquals(1, controller.listTickets(TicketFilter.excludingMerged()).body().size());
    }

    private static CreateTicketRequest duplicateRequest() {
        return new CreateTicketRequest(
                "Priya Nair",
                "Export to CSV still times out",
                "Retried this morning on the 80k-row report, same gateway timeout at ~60s.",
                TicketPriority.HIGH);
    }

    private static CreateTicketRequest validRequest() {
        return new CreateTicketRequest(
                "Priya Nair",
                "Export to CSV times out for large reports",
                "The monthly usage export fails after roughly 60 seconds.");
    }
}
