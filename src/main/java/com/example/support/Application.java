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

        printHeader("Merge duplicate tickets");
        String survivorId = controller.createTicket(new CreateTicketRequest(
                "Dana Weiss",
                "Printer not detected",
                "The office printer stopped appearing in the device list after the last update.",
                TicketPriority.MEDIUM)).body().getId();
        String duplicateId = controller.createTicket(new CreateTicketRequest(
                "Dana Weiss",
                "Printer still missing from the device list",
                "Reported this last week too: the printer is still not listed after the update.",
                TicketPriority.HIGH)).body().getId();

        ApiResponse<Ticket> merged = controller.mergeTickets(duplicateId, survivorId);
        Ticket survivor = merged.body();
        Ticket duplicate = controller.getTicket(duplicateId).body();
        System.out.println(merged.statusCode() + " -> survivor " + survivor);
        System.out.println("  priority raised to " + survivor.getPriority() + " by the merge");
        System.out.println("  duplicate " + duplicate.getId() + " is " + duplicate.getStatus()
                + ", MERGED into " + duplicate.getMergedIntoId());
        System.out.println("  absorbed text:");
        printIndented(mergeBlockOf(survivor));

        printHeader("Work queue with merged duplicates hidden");
        printTickets(controller.listTickets(TicketFilter.excludingMerged()).body());

        printHeader("Error handling examples");
        System.out.println("Unknown id      -> " + describe(controller.getTicket("TCK-9999")));
        System.out.println("Blank subject   -> " + describe(controller.createTicket(
                new CreateTicketRequest("Nobody", "  ", "Missing subject"))));
        System.out.println("Bad transition  -> " + describe(controller.changeStatus(newId, TicketStatus.IN_PROGRESS)));
        System.out.println("Self merge      -> " + describe(controller.mergeTickets(survivorId, survivorId)));
        System.out.println("Merged already  -> " + describe(controller.mergeTickets(duplicateId, newId)));
        System.out.println("Other customer  -> " + describe(controller.mergeTickets(newId, survivorId)));

        printHeader("Ticket count");
        System.out.println(service.countTickets() + " tickets in store");
    }

    /** The part of a merged description that came from the duplicate. */
    private static String mergeBlockOf(Ticket ticket) {
        int start = ticket.getDescription().indexOf("--- Merged from");
        return start < 0 ? "" : ticket.getDescription().substring(start);
    }

    private static void printIndented(String text) {
        text.lines().forEach(line -> System.out.println("    " + line));
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
