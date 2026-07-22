# Chat controllers &amp; advisors — sequence diagram

The call order behind five representative endpoints, showing how `TokenLoggerAdvisor` wraps
every `ChatClient` call, plus a separate startup-time section showing where `RagAdvisor`
actually fits in (see
[ai-controllers-advisors-class-diagram.md](./ai-controllers-advisors-class-diagram.md) for the
static structure).

```mermaid
sequenceDiagram
    actor Client
    participant ImageCtrl as ImageController
    participant MemCtrl as MemoryChatController
    participant MultiCtrl as MultiModelChatController
    participant OllamaCtrl as OllamaChatController
    participant TimeCtrl as TimeController
    participant ChatClient
    participant TokenLogger as TokenLoggerAdvisor
    participant Tools as TimeTools
    participant ImageModel
    participant Model as Model (Ollama / OpenAI)

    rect rgb(240, 240, 240)
    note over Client, ImageModel: Image generation
    Client->>ImageCtrl: GET /api/image?message=...
    ImageCtrl->>ImageModel: call(new ImagePrompt(message))
    ImageModel-->>ImageCtrl: imageResponse
    ImageCtrl-->>Client: base64 image JSON
    end

    rect rgb(240, 240, 240)
    note over Client, Model: Memory-scoped chat
    Client->>MemCtrl: GET /api/chat-memory?message=... (username header)
    MemCtrl->>ChatClient: prompt().advisors(CONVERSATION_ID).user(message).call()
    ChatClient->>TokenLogger: adviseCall(request, chain)
    TokenLogger->>Model: nextCall(request) (memory advisor injects prior turns first)
    Model-->>TokenLogger: response + usage
    TokenLogger->>TokenLogger: log token usage
    TokenLogger-->>ChatClient: response
    ChatClient-->>MemCtrl: content()
    MemCtrl-->>Client: 200 OK
    end

    rect rgb(240, 240, 240)
    note over Client, Model: Multi-model chat (e.g. /api/openai/chat)
    Client->>MultiCtrl: GET /api/openai/chat?message=...
    MultiCtrl->>ChatClient: prompt(message).call()
    ChatClient->>TokenLogger: adviseCall(request, chain)
    TokenLogger->>Model: nextCall(request)
    Model-->>TokenLogger: response + usage
    TokenLogger->>TokenLogger: log token usage
    TokenLogger-->>ChatClient: response
    ChatClient-->>MultiCtrl: content()
    MultiCtrl-->>Client: response text
    note right of MultiCtrl: Same shape for /ollama/chat, /email, /prompt-stuffing,<br/>/stream, and the structured-output endpoints
    end

    rect rgb(240, 240, 240)
    note over Client, Model: Plain Ollama chat with inline system prompt
    Client->>OllamaCtrl: GET /api/chat?message=...
    OllamaCtrl->>ChatClient: prompt().system("IT helpdesk assistant...").user(message).call()
    ChatClient->>TokenLogger: adviseCall(request, chain)
    TokenLogger->>Model: nextCall(request)
    Model-->>TokenLogger: response + usage
    TokenLogger->>TokenLogger: log token usage
    TokenLogger-->>ChatClient: response
    ChatClient-->>OllamaCtrl: content()
    OllamaCtrl-->>Client: response text
    end

    rect rgb(240, 240, 240)
    note over Client, Tools: Time-tool chat
    Client->>TimeCtrl: GET /api/tools/local-time?message=... (username header)
    TimeCtrl->>ChatClient: prompt().advisors(CONVERSATION_ID).user(message).call()
    ChatClient->>TokenLogger: adviseCall(request, chain)
    TokenLogger->>Model: nextCall(request) (defaultTools = TimeTools)
    Model->>Tools: getCurrentLocalTime() / getCurrentTime(timeZone)
    Tools-->>Model: current time string
    Model-->>TokenLogger: final answer + usage
    TokenLogger->>TokenLogger: log token usage
    TokenLogger-->>ChatClient: response
    ChatClient-->>TimeCtrl: content()
    TimeCtrl-->>Client: 200 OK
    end

    rect rgb(255, 245, 225)
    note over Client, Model: Application startup — RagAdvisor bean assembly (not per-request)
    participant Config as ChatClientConfig
    participant RagAdv as RagAdvisor
    participant Factory as ChatClientFactory
    Config->>RagAdv: retrievalAugmentationAdvisor(vectorStore, chatClientFactory)
    RagAdv->>Factory: createOllama() (query-translation chat client)
    Factory-->>RagAdv: ChatClient.Builder
    RagAdv-->>Config: RetrievalAugmentationAdvisor bean
    note right of RagAdv: Runs once at startup, not on the request path of<br/>any controller above. The bean is only injected<br/>into ragMemoryChatClient (used by RagController).
    end
```

## Relevant classes

| Participant | Source |
|---|---|
| `ImageController` | `ImageController.java` |
| `MemoryChatController` | `MemoryChatController.java` |
| `MultiModelChatController` | `MultiModelChatController.java` |
| `OllamaChatController` | `OllamaChatController.java` |
| `TimeController` | `TimeController.java` |
| `ChatClient` | Spring AI, beans configured in `ChatClientConfig.java` |
| `TokenLoggerAdvisor` | `TokenLoggerAdvisor.java`, registered by `ChatClientFactory.java` |
| `TimeTools` | `TimeTools.java` |
| `ImageModel` | Spring AI (`org.springframework.ai.image.ImageModel`) |
| `ChatClientConfig` | `ChatClientConfig.java` |
| `RagAdvisor` | `RagAdvisor.java` |
| `ChatClientFactory` | `ChatClientFactory.java` |
