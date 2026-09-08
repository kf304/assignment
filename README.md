# Support Ticket System

A small customer support ticket management application written in plain Java 17 with Maven.
No web or DI framework is used — the layers are ordinary Java classes, which keeps the code easy
to read and easy to extend.

## Features

- Create a ticket (validated input, generated id `TCK-1000`, `TCK-1001`, …)
- Retrieve a ticket by id
- Update a ticket (partial update: only the provided fields change)
- Change ticket status, guarded by lifecycle rules
- Change ticket priority
- List tickets, newest first, optionally filtered by status, priority, customer or by
  excluding merged duplicates
- Merge a duplicate ticket into the ticket Support keeps working on
- Delete a ticket and count tickets

## Ticket model

| Field          | Type             | Notes                                          |
|----------------|------------------|------------------------------------------------|
| `id`           | `String`         | Immutable, generated (`TCK-####`)              |
| `customerName` | `String`         | Required, max 100 characters                   |
| `subject`      | `String`         | Required, max 150 characters                   |
| `description`  | `String`         | Required, max 5000 characters                  |
| `status`       | `TicketStatus`   | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`    |
| `priority`     | `TicketPriority` | `LOW`, `MEDIUM`, `HIGH`, `URGENT`              |
| `createdAt`    | `Instant`        | Immutable, set on creation                     |
| `updatedAt`    | `Instant`        | Stamped on every change                        |
| `mergedIntoId` | `String`         | Null unless the ticket was merged into another |
| `mergedAt`     | `Instant`        | Null unless the ticket was merged              |

### Status lifecycle

```
OPEN        -> IN_PROGRESS | RESOLVED | CLOSED
IN_PROGRESS -> OPEN | RESOLVED | CLOSED
RESOLVED    -> IN_PROGRESS | CLOSED
CLOSED      -> OPEN                     (reopen only)
```

Any other transition — including a transition to the current status — is rejected with
`InvalidStatusTransitionException`, which the controller maps to HTTP-style `409`.

## Merging duplicate tickets

`mergeTickets(sourceId, targetId)` merges a duplicate (**source**) into the ticket Support keeps
working on (**target**). Nothing is deleted: the source is closed and kept as a tombstone that
points at the survivor, so the customer's original ticket number still resolves.

| | Target (survivor) | Source (duplicate) |
|---|---|---|
| `id`, `createdAt`, `customerName`, `subject` | unchanged | unchanged |
| `status`      | unchanged                                  | forced to `CLOSED` |
| `priority`    | raised to the higher of the two            | unchanged |
| `description` | original text plus the duplicate's subject and description | annotated with the merge target |
| `updatedAt`   | stamped                                    | stamped |
| `mergedIntoId` / `mergedAt` | stay null                    | set to the target and the merge time |

The appended block looks like this, and only the appended part is truncated if the result would
exceed the 5000-character description limit — the target's own text is never cut:

```
--- Merged from TCK-1007 (Dana Weiss, 2026-09-08T20:22:32Z) ---
Printer still missing from the device list

Reported this last week too: the printer is still not listed after the update.
```

A merge is rejected with `TicketMergeException` (HTTP-style `409`) when:

- the two ids are the same — a ticket cannot be merged with itself
- the source has already been merged (merges are permanent; there is no unmerge)
- the target has already been merged — merge chains are not followed automatically
- the target is `CLOSED`, matching the rule that closed tickets cannot be edited
- the two tickets belong to different customers

A merge is not a lifecycle transition, so it never produces `InvalidStatusTransitionException`, and
a source that is already `CLOSED` can still be merged. Both tickets are written in a single
`saveAll` call, so a rejected merge writes nothing.

Merged tickets stay in listings and in `countTickets()` by default;
`TicketFilter.excludingMerged()` hides them from a work queue.

## Project structure

```
src/main/java/com/example/support/
  Application.java              Console demo runner (main class)
  SampleData.java               Realistic seed tickets
  api/
    ApiResponse.java            Status code + body + error message
    TicketController.java       Boundary; maps exceptions to status codes
  dto/
    CreateTicketRequest.java    Input for creation
    UpdateTicketRequest.java    Partial update (null = unchanged)
    TicketFilter.java           Optional list criteria (incl. excludeMerged)
  exception/
    TicketNotFoundException.java
    ValidationException.java
    InvalidStatusTransitionException.java
    TicketMergeException.java
  model/
    Ticket.java                 Domain object
    TicketStatus.java           Lifecycle + allowed transitions
    TicketPriority.java
  repository/
    TicketRepository.java       Storage abstraction (incl. saveAll for multi-ticket writes)
    InMemoryTicketRepository.java  Thread-safe in-memory implementation
  service/
    TicketService.java          Business logic and validation
    TicketValidator.java        Field validation rules
    TicketMerger.java           Duplicate-merge rules
    TicketIdGenerator.java      Id abstraction
    SequentialTicketIdGenerator.java

src/test/java/com/example/support/   Mirrors the main source layout
```

Layering is one-directional: `api` → `service` → `repository` → `model`.

## Error handling

| Situation                       | Service                              | Controller |
|---------------------------------|--------------------------------------|------------|
| Missing / blank / oversized field | `ValidationException`              | `400`      |
| Unknown ticket id               | `TicketNotFoundException`            | `404`      |
| Illegal status transition       | `InvalidStatusTransitionException`   | `409`      |
| Tickets cannot be merged        | `TicketMergeException`               | `409`      |
| Success                         | returns the ticket                   | `200` / `201` / `204` |

## Requirements

- JDK 17 or newer
- Maven is **not** required: the project ships the Maven Wrapper (`mvnw`, `mvnw.cmd`),
  which downloads the correct Maven version on first use.

### Installing a JDK

Windows (PowerShell or Command Prompt):

```powershell
winget install --id EclipseAdoptium.Temurin.17.JDK -e
```

Close and reopen the terminal afterwards, then check:

```powershell
java -version
```

macOS (Homebrew): `brew install temurin@17`
Debian/Ubuntu: `sudo apt install openjdk-17-jdk`

## Build

Windows:

```powershell
.\mvnw.cmd clean package
```

macOS / Linux:

```bash
./mvnw clean package
```

## Test

Windows:

```powershell
.\mvnw.cmd test
```

macOS / Linux:

```bash
./mvnw test
```

Runs the JUnit 5 suite (193 tests) covering the model, repository, service and controller layers.

## Run

After packaging:

```bash
java -jar target/support-ticket-system-1.0.0.jar
```

Or without packaging (Windows: `.\mvnw.cmd`):

```bash
./mvnw compile exec:java -Dexec.mainClass=com.example.support.Application
```

If a global Maven install is present, `mvn` can be used in place of `./mvnw` everywhere above.

The demo seeds the sample tickets, lists them, creates and updates a ticket, changes its status,
merges a duplicate pair and shows the validation, not-found, illegal-transition and merge-conflict
responses.
