# Chat memory — sequence diagram

The exact call order behind the activity diagram in
[chat-memory.md](./chat-memory.md), including which object calls which.

```mermaid
sequenceDiagram
    actor Client
    participant Controller as MemoryChatController
    participant ChatClient as ChatClient (memoryChatClient)
    participant MemAdv as MessageChatMemoryAdvisor
    participant Repo as JdbcChatMemoryRepository
    participant H2 as H2 (SPRING_AI_CHAT_MEMORY)
    participant Model as Ollama model

    Client->>Controller: GET /chat-memory?message=... (username header)
    Controller->>ChatClient: prompt().advisors(CONVERSATION_ID).user(message).call()
    ChatClient->>MemAdv: load history(conversationId)
    MemAdv->>Repo: findByConversationId(conversationId)
    Repo->>H2: SELECT ... WHERE conversation_id=?
    H2-->>Repo: rows
    Repo-->>MemAdv: last 10 messages
    MemAdv-->>ChatClient: history + new user message
    ChatClient->>Model: generate(prompt)
    Model-->>ChatClient: answer
    ChatClient->>Repo: save(user message, assistant message)
    Repo->>H2: INSERT
    ChatClient-->>Controller: answer
    Controller-->>Client: 200 OK
```

## Relevant classes

| Participant | Source |
|---|---|
| `MemoryChatController` | `MemoryChatController.java` |
| `ChatClient` (memoryChatClient bean) | `ChatClientConfig.java#memoryChatClient` |
| `MessageChatMemoryAdvisor` | Spring AI, wired via `MemoryAdvisor.java` |
| `JdbcChatMemoryRepository` | Spring AI JDBC chat memory module |
| H2 schema | `schema-h2db.sql` |
