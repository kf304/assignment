package com.example.support;

import com.example.support.api.ApiResponse;
import com.example.support.api.TicketController;
import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.TicketFilter;
import com.example.support.dto.UpdateTicketRequest;
import com.example.support.model.Ticket;
import com.example.support.model.TicketPriority;
import com.example.support.model.TicketStatus;
import com.example.support.repository.InMemoryTicketRepository;
import com.example.support.repository.TicketRepository;
import com.example.support.service.TicketService;
import java.util.List;

/** Console demo that exercises the ticket operations against seeded sample data. */
public final class Application {

    public static void main(String[] args) {
        TicketRepository repository = new InMemoryTicketRepository();
        TicketService service = new TicketService(repository);
        TicketController controller = new TicketController(service);

        SampleData.seed(service);

        printHeader("All tickets (newest first)");
        printTickets(controller.listTickets().body());

        printHeader("Open tickets");
        printTickets(controller.listTickets(TicketFilter.byStatus(TicketStatus.OPEN)).body());

        printHeader("Create a new ticket");
        ApiResponse<Ticket> created = controller.createTicket(new CreateTicketRequest(
                "Lena Kowalski",
                "Two-factor codes arrive late",
                "SMS codes take three to four minutes, so they expire before I can use them.",
                TicketPriority.HIGH));
        System.out.println(created.statusCode() + " -> " + created.body());

        String newId = created.body().getId();

        printHeader("Update the new ticket");
        System.out.println(controller.updateTicket(newId,
                new UpdateTicketRequest(null, null, null, TicketPriority.URGENT)).body());

        printHeader("Move it to IN_PROGRESS");
        System.out.println(controller.changeStatus(newId, TicketStatus.IN_PROGRESS).body());

        printHeader("Error handling examples");
        System.out.println("Unknown id      -> " + describe(controller.getTicket("TCK-9999")));
        System.out.println("Blank subject   -> " + describe(controller.createTicket(
                new CreateTicketRequest("Nobody", "  ", "Missing subject"))));
        System.out.println("Bad transition  -> " + describe(controller.changeStatus(newId, TicketStatus.IN_PROGRESS)));

        printHeader("Ticket count");
        System.out.println(service.countTickets() + " tickets in store");
    }

    private static String describe(ApiResponse<?> response) {
        return response.statusCode() + " " + response.errorMessage();
    }

    private static void printTickets(List<Ticket> tickets) {
        tickets.forEach(ticket -> System.out.printf("  %-9s %-12s %-7s %-16s %s%n",
                ticket.getId(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCustomerName(),
                ticket.getSubject()));
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println("== " + title + " ==");
    }

    private Application() {
    }
}
