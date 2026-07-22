# Chat controllers &amp; advisors — class diagram

Structure of five representative REST controllers, the two cross-cutting advisors
(`TokenLoggerAdvisor`, `RagAdvisor`), and the shared infrastructure (`ChatClientFactory`,
`ChatClient`) they're built on (see
[ai-controllers-advisors-sequence.md](./ai-controllers-advisors-sequence.md) for the runtime
call order).

```mermaid
classDiagram
    class ImageController {
      -ImageModel imageModel
      +generateImage(message) String
      +generateImageWithOptions(message) String
    }
    class MemoryChatController {
      -ChatClient chatClient
      +chatMemory(conversationId, message) ResponseEntity~String~
    }
    class MultiModelChatController {
      -Resource userPromptTemplate
      -Resource hrPolicyTemplate
      -Resource systemPromptTemplate
      -ChatClient openAiChatClient
      -ChatClient ollamaChatClient
      +openAiChat(message) String
      +chat(message) String
      +email(customerName, customerMessage) String
      +promptStuff(message) String
      +stream(message) Flux~String~
      +chatBean(message) ResponseEntity~CountryCities~
      +chatList(message) ResponseEntity~List~String~~
      +chatMap(message) ResponseEntity~Map~
      +chatBeanList(message) ResponseEntity~List~CountryCities~~
    }
    class OllamaChatController {
      -ChatClient ollamaChatClient
      +ollamaChat(message) String
    }
    class TimeController {
      -ChatClient chatClient
      +localTime(conversationId, message) ResponseEntity~String~
    }
    class TokenLoggerAdvisor {
      +adviseCall(request, chain) ChatClientResponse
      +getName() String
      +getOrder() int
    }
    class CallAdvisor {
      <<interface>>
      +adviseCall(request, chain) ChatClientResponse
      +getName() String
      +getOrder() int
    }
    class RagAdvisor {
      <<Configuration>>
      +retrievalAugmentationAdvisor(vectorStore, chatClientFactory) RetrievalAugmentationAdvisor
    }
    class ChatClientFactory {
      +createOllama(advisors) ChatClient.Builder
      +createOpenAi(advisors) ChatClient.Builder
      +createBespokeMinicheck() ChatClient.Builder
      +create(chatModel, advisors) ChatClient.Builder
    }
    class ChatClient {
      <<interface>>
      +prompt() ChatClientRequestSpec
    }
    class TimeTools {
      +getCurrentLocalTime() String
      +getCurrentTime(timeZone) String
    }
    class ImageModel {
      <<interface>>
      +call(prompt) ImageResponse
    }
    class RetrievalAugmentationAdvisor {
      +adviseCall(request, chain) ChatClientResponse
    }

    TokenLoggerAdvisor ..|> CallAdvisor
    RetrievalAugmentationAdvisor ..|> CallAdvisor

    ImageController --> ImageModel
    MemoryChatController --> ChatClient
    MultiModelChatController --> ChatClient
    OllamaChatController --> ChatClient
    TimeController --> ChatClient
    TimeController ..> TimeTools : via timeChatClient's tools

    ChatClientFactory --> ChatClient : builds
    ChatClientFactory ..> TokenLoggerAdvisor : default advisor on every client
    RagAdvisor ..> RetrievalAugmentationAdvisor : builds bean
    RagAdvisor --> ChatClientFactory : uses (query translation)
```

## Relevant classes

| Class | Source |
|---|---|
| `ImageController` | `ImageController.java` |
| `MemoryChatController` | `MemoryChatController.java` |
| `MultiModelChatController` | `MultiModelChatController.java` |
| `OllamaChatController` | `OllamaChatController.java` |
| `TimeController` | `TimeController.java` |
| `TokenLoggerAdvisor` | `TokenLoggerAdvisor.java` |
| `RagAdvisor` | `RagAdvisor.java` |
| `ChatClientFactory` | `ChatClientFactory.java` |
| `ChatClient` / `CallAdvisor` / `ImageModel` / `RetrievalAugmentationAdvisor` | Spring AI |
| `TimeTools` | `TimeTools.java` |

**Note:** `RetrievalAugmentationAdvisor` (built by `RagAdvisor`) is only wired into the
`ragMemoryChatClient` bean, used by `RagController` — none of the five controllers here go
through it. `TokenLoggerAdvisor`, on the other hand, is registered by `ChatClientFactory` on
every client it builds, so it wraps calls from all five.
