package com.example.support;

import com.example.support.dto.CreateTicketRequest;
import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;
import com.example.support.service.TicketService;
import java.util.List;

/** Realistic seed tickets used by the demo runner and by tests. */
public final class SampleData {

    private SampleData() {
    }

    public static List<CreateTicketRequest> tickets() {
        return List.of(
                new CreateTicketRequest(
                        "Aisha Rahman",
                        "Cannot log in after password reset",
                        "I reset my password yesterday and the new one is rejected on the web login "
                                + "with 'invalid credentials'. The reset email arrived twice.",
                        TicketPriority.HIGH),
                new CreateTicketRequest(
                        "Tomas Berg",
                        "Invoice #4471 shows the wrong VAT rate",
                        "Our September invoice applies 25% VAT, but our account is registered in "
                                + "Germany and should be charged 19%.",
                        TicketPriority.MEDIUM),
                new CreateTicketRequest(
                        "Priya Nair",
                        "Export to CSV times out for large reports",
                        "Exporting the monthly usage report (about 80k rows) fails after roughly "
                                + "60 seconds with a gateway timeout. Smaller exports work.",
                        TicketPriority.URGENT),
                new CreateTicketRequest(
                        "Marco Silva",
                        "Request: add dark mode to the mobile app",
                        "The dashboard is hard to read at night. Please consider a dark theme that "
                                + "follows the system setting.",
                        TicketPriority.LOW),
                new CreateTicketRequest(
                        "Aisha Rahman",
                        "Webhook deliveries retry too aggressively",
                        "Our endpoint returned 503 for two minutes and we received more than 400 "
                                + "retries. Please confirm the backoff policy.",
                        TicketPriority.MEDIUM));
    }

    /** Seeds the service and advances a few tickets so the data looks lived-in. */
    public static void seed(TicketService ticketService) {
        List<String> ids = tickets().stream()
                .map(request -> ticketService.createTicket(request).getId())
                .toList();
        ticketService.changeStatus(ids.get(0), TicketStatus.IN_PROGRESS);
        ticketService.changeStatus(ids.get(2), TicketStatus.IN_PROGRESS);
        ticketService.changeStatus(ids.get(1), TicketStatus.RESOLVED);
    }
}
