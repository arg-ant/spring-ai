# Help-desk tool calling — sequence diagram

The exact call order behind the activity diagram in
[helpdesk-tool-calling.md](./helpdesk-tool-calling.md), including which object calls which.

```mermaid
sequenceDiagram
    actor Client
    participant Controller as HelpDeskController
    participant ChatClient as ChatClient (helpDeskChatClient)
    participant Tools as HelpDeskTools
    participant Service as HelpDeskTicketService
    participant Repo as HelpDeskTicketRepository

    Client->>Controller: GET /tools/help-desk?message=... (username header)
    Controller->>ChatClient: prompt().tools().toolContext(username).call()
    ChatClient->>ChatClient: model decides to call a tool
    ChatClient->>Tools: createTicket(ticketRequest, toolContext)
    Tools->>Service: createTicket(ticketInput, username)
    Service->>Repo: save(ticket)
    Repo-->>Service: ticket (with id)
    Service-->>Tools: ticket
    Tools-->>ChatClient: "Ticket #5 created for user ..."
    note right of ChatClient: returnDirect = true, bypasses further model processing
    ChatClient-->>Controller: answer
    Controller-->>Client: 200 OK
```

## Relevant classes

| Participant | Source |
|---|---|
| `HelpDeskController` | `HelpDeskController.java` |
| `ChatClient` (helpDeskChatClient bean) | `ChatClientConfig.java#helpDeskChatClient` |
| `HelpDeskTools` | `HelpDeskTools.java` |
| `HelpDeskTicketService` | `HelpDeskTicketService.java` |
| `HelpDeskTicketRepository` | `HelpDeskTicketRepository.java` |
