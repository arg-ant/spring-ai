# Runtime Flows

Sequence diagrams for the main request flows in this project. Rendered with [Mermaid](https://mermaid.js.org/) — view directly on GitHub, in VS Code (with a Mermaid preview extension), or in any Markdown viewer that supports it.

Base package: `com.multimodel.llm`. See root `README.md` for the endpoint table and general architecture.

## 1. Plain chat

`MultiModelChatController` (`/api/openai/chat`, `/api/ollama/chat`, `/api/stream`, ...) or `OllamaChatController` (`/api/chat`).

```mermaid
sequenceDiagram
    participant Client
    participant Controller as MultiModelChatController /<br/>OllamaChatController
    participant ChatClient as ChatClient<br/>(openAiChatClient / ollamaChatClient)
    participant Advisors as SimpleLoggerAdvisor +<br/>TokenLoggerAdvisor
    participant Model as OpenAiChatModel /<br/>OllamaChatModel
    participant LLM as OpenAI API / Ollama

    Client->>Controller: HTTP request (prompt)
    Controller->>ChatClient: prompt()
    ChatClient->>Advisors: apply advisor chain
    Advisors->>Model: call()
    Model->>LLM: chat completion request
    LLM-->>Model: completion
    Model-->>Advisors: response (logged)
    Advisors-->>ChatClient: response
    ChatClient-->>Controller: ChatResponse
    Controller-->>Client: HTTP response
```

`ChatClientConfig` builds these clients via `ChatClientFactory.createOpenAi(...)` / `createOllama(...)`, which attaches the default `SimpleLoggerAdvisor` + `TokenLoggerAdvisor` pair.

## 2. Memory chat (with tool calling)

`MemoryChatController` (`/api/chat-memory`), `TimeController` (`/api/tools/local-time`), `HelpDeskController` (`/api/tools/help-desk`).

```mermaid
sequenceDiagram
    participant Client
    participant Controller as MemoryChatController /<br/>TimeController / HelpDeskController
    participant ChatClient as memoryChatClient /<br/>timeChatClient / helpDeskChatClient
    participant MemAdv as MemoryAdvisor<br/>(MessageChatMemoryAdvisor)
    participant ChatMemory as MessageWindowChatMemory<br/>(max 10 msgs)
    participant Repo as JdbcChatMemoryRepository
    participant DB as H2 (file DB)
    participant Tools as HelpDeskTools / TimeTools<br/>(@Tool methods)
    participant Model as OllamaChatModel
    participant Ollama

    Client->>Controller: HTTP request (prompt, conversationId)
    Controller->>ChatClient: prompt()
    ChatClient->>MemAdv: apply advisor
    MemAdv->>ChatMemory: load history
    ChatMemory->>Repo: SELECT messages
    Repo->>DB: JDBC query
    DB-->>Repo: rows
    Repo-->>ChatMemory: messages
    MemAdv->>Model: call() with history + tools
    Model->>Ollama: chat completion request
    Ollama-->>Model: tool call requested (optional)
    Model->>Tools: invoke @Tool method (e.g. createTicket)
    Tools->>Tools: HelpDeskTicketService -> HelpDeskTicketRepository (JPA/H2)
    Tools-->>Model: tool result
    Model->>Ollama: continue with tool result
    Ollama-->>Model: final completion
    Model-->>MemAdv: response
    MemAdv->>ChatMemory: save new messages
    ChatMemory->>Repo: INSERT messages
    Repo->>DB: JDBC write
    MemAdv-->>ChatClient: response
    ChatClient-->>Controller: ChatResponse
    Controller-->>Client: HTTP response
```

Tool methods use `ToolContext` to access the current username. `HelpDeskTools.createTicket` / `getTicketStatus` delegate to `HelpDeskTicketService` → `HelpDeskTicketRepository`.

## 3. RAG — ingestion

`VectorStoreDataController` (`POST/DELETE /api/rag/random/load|remove`, `/document/load|remove`).

```mermaid
sequenceDiagram
    participant Client
    participant Controller as VectorStoreDataController
    participant Service as VectorStoreDataService
    participant Tika as TikaDocumentReader
    participant Splitter as TokenTextSplitter<br/>(200 tokens/chunk)
    participant VS as VectorStore
    participant Qdrant

    Client->>Controller: POST /api/rag/document/load (PDF)
    Controller->>Service: loadPDF() / loadRandom()
    Service->>Tika: parse document
    Tika-->>Service: raw Document(s)
    Service->>Splitter: split into chunks
    Splitter-->>Service: chunked Document(s)
    Service->>VS: add(documents)
    VS->>Qdrant: embed (Ollama bge-m3) + upsert vectors
    Qdrant-->>VS: ack
    VS-->>Service: ack
    Service-->>Controller: result
    Controller-->>Client: HTTP response
```

## 4. RAG — retrieval

Two retrieval paths exist side by side.

**4a. Manual retrieval** — `RagController.randomChat` (`GET /api/rag/random/chat`)

```mermaid
sequenceDiagram
    participant Client
    participant Controller as RagController
    participant VS as VectorStore
    participant Qdrant
    participant Prompt as systemPromptTemplate.st<br/>({documents} placeholder)
    participant ChatClient as ragMemoryChatClient
    participant Ollama

    Client->>Controller: GET /api/rag/random/chat?query=...
    Controller->>VS: similaritySearch(SearchRequest topK=3, threshold=0.5)
    VS->>Qdrant: vector search
    Qdrant-->>VS: matching Document(s)
    VS-->>Controller: Document(s)
    Controller->>Prompt: stuff documents into template
    Prompt-->>Controller: rendered system prompt
    Controller->>ChatClient: prompt(systemPrompt + query)
    ChatClient->>Ollama: chat completion request
    Ollama-->>ChatClient: completion
    ChatClient-->>Controller: ChatResponse
    Controller-->>Client: HTTP response
```

**4b. Advisor-driven retrieval** — `RagController.documentChat` (`GET /api/rag/document/chat`)

```mermaid
sequenceDiagram
    participant Client
    participant Controller as RagController
    participant ChatClient as ragMemoryChatClient
    participant RAA as RetrievalAugmentationAdvisor<br/>(RagAdvisor)
    participant Translate as TranslationQueryTransformer
    participant Retriever as VectorStoreDocumentRetriever
    participant Qdrant
    participant PII as PIIMaskingDocumentPostProcessor
    participant Ollama

    Client->>Controller: GET /api/rag/document/chat?query=...
    Controller->>ChatClient: prompt(query)
    ChatClient->>RAA: apply advisor
    RAA->>Translate: translate query to English
    Translate->>Ollama: translation request
    Ollama-->>Translate: translated query
    RAA->>Retriever: retrieve(translated query, topK=3, threshold=0.5)
    Retriever->>Qdrant: vector search
    Qdrant-->>Retriever: Document(s)
    RAA->>PII: postProcess(documents)
    PII-->>RAA: redacted Document(s) (emails/phones masked)
    RAA->>Ollama: chat completion (query + context)
    Ollama-->>RAA: completion
    RAA-->>ChatClient: response
    ChatClient-->>Controller: ChatResponse
    Controller-->>Client: HTTP response
```

## 5. Web-search RAG

`RagController.webSearchChat` (`GET /api/rag/web-search/chat`).

```mermaid
sequenceDiagram
    participant Client
    participant Controller as RagController
    participant ChatClient as webSearchChatClient
    participant WSA as WebSearchAdvisor
    participant RAA as RetrievalAugmentationAdvisor
    participant Retriever as TavilyWebSearchDocumentRetriever<br/>(active; Firecrawl variant present but disabled)
    participant Tavily as Tavily Search API
    participant Ollama

    Client->>Controller: GET /api/rag/web-search/chat?query=...
    Controller->>ChatClient: prompt(query)
    ChatClient->>WSA: apply advisor
    WSA->>RAA: delegate to RetrievalAugmentationAdvisor
    RAA->>Retriever: retrieve(query)
    Retriever->>Tavily: POST /search (bearer TAVILY_SEARCH_API_KEY)
    Tavily-->>Retriever: search results
    Retriever-->>RAA: mapped Document(s)
    RAA->>Ollama: chat completion (query + web context)
    Ollama-->>RAA: completion
    RAA-->>ChatClient: response
    ChatClient-->>Controller: ChatResponse
    Controller-->>Client: HTTP response
```

> `FirecrawlWebSearchDocumentRetriever` (`https://api.firecrawl.dev/v1/search`, `FIRECRAWL_API_KEY`) implements the same retriever contract but is currently commented out in `WebSearchAdvisor` in favor of Tavily.

## 6. Self-evaluation (fact-checking)

`SelfEvaluatingChatController` (`/api/evaluate/chat`, `/prompt-stuffing`).

```mermaid
sequenceDiagram
    participant Client
    participant Controller as SelfEvaluatingChatController
    participant ChatClient as ollamaChatClient
    participant Ollama
    participant Evaluator as FactCheckingEvaluator<br/>(bespokeMinicheckChatClient)
    participant Handler as GlobalExceptionHandler

    Client->>Controller: request (query + context)
    Controller->>ChatClient: prompt(query)
    ChatClient->>Ollama: chat completion request
    Ollama-->>ChatClient: generated answer
    ChatClient-->>Controller: answer
    Controller->>Evaluator: evaluate(answer, context Document)
    Evaluator->>Ollama: bespoke-minicheck model call
    Ollama-->>Evaluator: fact-check verdict
    alt answer fails fact-check
        Evaluator-->>Controller: throws InvalidAnswerException
        Controller->>Handler: exception propagates
        Handler-->>Client: HTTP 400
    else answer passes
        Evaluator-->>Controller: verified answer
        Controller-->>Client: HTTP response
    end
```

## 7. Image / Audio (simple passthrough)

```mermaid
sequenceDiagram
    participant Client
    participant ImageController
    participant AudioController
    participant ImageModel as ImageModel (gpt-image-1)
    participant SpeechModels as TranscriptionModel /<br/>TextToSpeechModel
    participant OpenAI

    Client->>ImageController: POST /api/image (prompt)
    ImageController->>ImageModel: call()
    ImageModel->>OpenAI: image generation request
    OpenAI-->>ImageModel: image(s)
    ImageModel-->>ImageController: result
    ImageController-->>Client: HTTP response

    Client->>AudioController: POST /api/audio (file or text)
    AudioController->>SpeechModels: call()
    SpeechModels->>OpenAI: transcription / TTS request
    OpenAI-->>SpeechModels: result
    SpeechModels-->>AudioController: result
    AudioController-->>Client: HTTP response
```

---

**Key files by flow:**

| Flow | Files |
|---|---|
| Plain chat | `controller/MultiModelChatController.java`, `controller/OllamaChatController.java`, `config/ChatClientConfig.java`, `config/ChatClientFactory.java`, `advisors/TokenLoggerAdvisor.java` |
| Memory chat | `controller/MemoryChatController.java`, `controller/TimeController.java`, `controller/HelpDeskController.java`, `advisors/MemoryAdvisor.java`, `tools/HelpDeskTools.java`, `tools/TimeTools.java`, `service/HelpDeskTicketService.java` |
| RAG ingestion | `controller/VectorStoreDataController.java`, `service/VectorStoreDataService.java` |
| RAG retrieval | `controller/RagController.java`, `advisors/RagAdvisor.java`, `rag/PIIMaskingDocumentPostProcessor.java` |
| Web-search RAG | `controller/RagController.java`, `advisors/WebSearchAdvisor.java`, `rag/TavilyWebSearchDocumentRetriever.java`, `rag/FirecrawlWebSearchDocumentRetriever.java` |
| Self-evaluation | `controller/SelfEvaluatingChatController.java`, `exception/GlobalExceptionHandler.java`, `exception/InvalidAnswerException.java` |
| Image/Audio | `controller/ImageController.java`, `controller/AudioController.java` |
