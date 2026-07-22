# Help-desk subsystem — class diagram

Structure of the classes involved in ticket creation and lookup, as used by the help-desk tool
calling flow (see [helpdesk-sequence.md](./helpdesk-sequence.md)).

```mermaid
classDiagram
    class HelpDeskController {
      +helpDesk(username, message) ResponseEntity~String~
    }
    class HelpDeskTools {
      +createTicket(ticketRequest, toolContext) String
      +getTicketStatus(toolContext) List~HelpDeskTicket~
    }
    class HelpDeskTicketService {
      +createTicket(ticketInput, username) HelpDeskTicket
      +getTicketsByUsername(username) List~HelpDeskTicket~
    }
    class HelpDeskTicketRepository {
      <<interface>>
      +findByUsername(username) List~HelpDeskTicket~
    }
    class HelpDeskTicket {
      -Long id
      -String username
      -String issue
      -String status
      -LocalDateTime createdAt
      -LocalDateTime eta
    }
    class TicketRequest {
      <<record>>
      +String issue
    }

    HelpDeskController --> HelpDeskTools
    HelpDeskTools --> HelpDeskTicketService
    HelpDeskTicketService --> HelpDeskTicketRepository
    HelpDeskTicketRepository ..> HelpDeskTicket
    HelpDeskTools ..> TicketRequest
    HelpDeskTicketService ..> TicketRequest
```

## Relevant classes

| Class | Source |
|---|---|
| `HelpDeskController` | `HelpDeskController.java` |
| `HelpDeskTools` | `HelpDeskTools.java` |
| `HelpDeskTicketService` | `HelpDeskTicketService.java` |
| `HelpDeskTicketRepository` | `HelpDeskTicketRepository.java` |
| `HelpDeskTicket` | `HelpDeskTicket.java` |
| `TicketRequest` | `TicketRequest.java` |
