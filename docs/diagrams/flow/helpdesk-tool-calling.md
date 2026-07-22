# Help-desk tool calling flow

`HelpDeskController` passes the username into `ToolContext`, and the model decides whether to
invoke `createTicket` (returns directly, bypassing further model processing) or
`getTicketStatus`.

```mermaid
flowchart TD
    A["User message<br/><small>username in tool context</small>"] --> B["Model decides<br/><small>Create ticket or check status</small>"]
    B --> C["HelpDeskTools invoked<br/><small>createTicket or getTicketStatus</small>"]
    C --> D["Ticket repository<br/><small>H2 helpdesk_tickets table</small>"]
    D --> E["Result returned<br/><small>Ticket create bypasses re-processing</small>"]
```

## Relevant classes

| Component | Source |
|---|---|
| Endpoint, tool context wiring | `HelpDeskController.java` |
| Tool definitions | `HelpDeskTools.java` |
| Business logic | `HelpDeskTicketService.java` |
| Persistence | `HelpDeskTicketRepository.java`, `HelpDeskTicket.java` |
| Request payload | `TicketRequest.java` |
| Persona / rules for the assistant | `helpDeskSystemPromptTemplate.st` |

Note: `createTicket` is annotated `returnDirect = true`, so its string result is returned to the
caller as-is instead of being fed back to the model for further processing.
