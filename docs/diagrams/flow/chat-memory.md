# Chat memory flow

Used by `memoryChatClient` (and any client wrapped with `memoryAdvisor`). The `username` header
becomes the `CONVERSATION_ID`, scoping the JDBC-backed `MessageWindowChatMemory`.

```mermaid
flowchart TD
    A["Request arrives<br/><small>username header, message param</small>"] --> B["Load history<br/><small>Last 10 messages from H2</small>"]
    B --> C["Build prompt<br/><small>History + new user message</small>"]
    C --> D["Call model<br/><small>Ollama chat model</small>"]
    D --> E["Save turn<br/><small>Persisted to SPRING_AI_CHAT_MEMORY</small>"]
    E --> F["Response returned<br/><small>To controller as 200 OK</small>"]
```

## Relevant classes

| Component | Source |
|---|---|
| Memory advisor factory | `MemoryAdvisor.java` |
| Chat memory bean (window size, repository) | `ChatClientConfig.java#messageWindowChatMemory` |
| Endpoint | `MemoryChatController.java` |
| Backing table schema | `schema-h2db.sql` |
| Max messages constant | `Constants.java` (`MAX_MESSAGES`) |
