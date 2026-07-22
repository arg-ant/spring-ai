# ChatClient / advisor architecture — class diagram

How `ChatClientConfig` wires together `ChatClientFactory` and the advisor factories to produce
each `ChatClient` bean (see [overall-architecture.md](./overall-architecture.md) for the
runtime request flow).

```mermaid
classDiagram
    class ChatClientFactory {
      +createOllama(advisors) ChatClient.Builder
      +createOpenAi(advisors) ChatClient.Builder
      +createBespokeMinicheck() ChatClient.Builder
      +create(chatModel, advisors) ChatClient.Builder
    }
    class ChatClientConfig {
      +openAiChatClient() ChatClient
      +ollamaChatClient() ChatClient
      +bespokeMinicheckChatClient() ChatClient
      +messageWindowChatMemory() ChatMemory
      +memoryChatClient() ChatClient
      +ragMemoryChatClient() ChatClient
      +timeChatClient() ChatClient
      +helpDeskChatClient() ChatClient
      +webSearchChatClient() ChatClient
    }
    class MemoryAdvisor {
      +memoryAdvisor(chatMemory)$ Advisor
    }
    class RagAdvisor {
      +retrievalAugmentationAdvisor(vectorStore, chatClientFactory) RetrievalAugmentationAdvisor
    }
    class WebSearchAdvisor {
      +webSearchAdvisor(restClientBuilder)$ Advisor
    }
    class TokenLoggerAdvisor {
      +adviseCall(request, chain) ChatClientResponse
      +getName() String
      +getOrder() int
    }
    class Constants {
      +double TEMPERATURE$
      +int MAX_TOKENS$
      +int MAX_MESSAGES$
      +int TOP_K$
      +double SIMILARITY_THRESHOLD$
    }

    ChatClientConfig --> ChatClientFactory
    ChatClientConfig --> MemoryAdvisor
    ChatClientConfig --> RagAdvisor
    ChatClientConfig --> WebSearchAdvisor
    ChatClientFactory --> TokenLoggerAdvisor
    ChatClientFactory ..> Constants
```

## Relevant classes

| Class | Source |
|---|---|
| `ChatClientFactory` | `ChatClientFactory.java` |
| `ChatClientConfig` | `ChatClientConfig.java` |
| `MemoryAdvisor` | `MemoryAdvisor.java` |
| `RagAdvisor` | `RagAdvisor.java` |
| `WebSearchAdvisor` | `WebSearchAdvisor.java` |
| `TokenLoggerAdvisor` | `TokenLoggerAdvisor.java` |
| `Constants` | `Constants.java` |
