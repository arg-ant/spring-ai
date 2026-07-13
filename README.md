# Spring AI Multimodel

A feature-rich Spring AI demonstration project showcasing multi-model LLM interactions, Retrieval-Augmented Generation (RAG), conversation memory, tool calling, image/audio processing, and full observability.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.6, Java 21 |
| AI | Spring AI 2.0.0-M6 |
| LLM Providers | OpenAI, Ollama (llama3.1, bespoke-minicheck) |
| Embeddings | Ollama bge-m3 |
| Vector Store | Qdrant |
| Chat Memory | H2 (file-based) via JDBC |
| Observability | Prometheus, Grafana, Zipkin |
| Document Parsing | Apache Tika |
| Web Search | Firecrawl API, Tavily API |

## Prerequisites

- Java 21+
- Docker (for infrastructure services)
- Docker Desktop
- OpenAI API key
- Firecrawl API key (for web search RAG)
- Tavily API key (for alternative web search RAG)
- Ollama volume pre-pulled with `llama3.1`, `bge-m3`, and `bespoke-minicheck` models

## Running

Run MultiModelApplication.java in debug mode and it will automatically start all infrastructure services.

Set required environment variables and start the app:

```bash
export OPENAI_API_KEY=...
export FIRECRAWL_API_KEY=...
export TAVILY_SEARCH_API_KEY=...

./mvnw spring-boot:run
```

## Infrastructure Services

| Service    | Port                     | Purpose              |
|------------|--------------------------|----------------------|
| Ollama     | 11434                    | Local LLM inference  |
| Qdrant     | 6333 (REST), 6334 (gRPC) | Vector store for RAG |
| Actuator   | 8080                     | Metrics tracker      |
| Prometheus | 9090                     | Metrics scraping     |
| Grafana    | 3000                     | Metrics dashboards   |
| Zipkin     | 9411                     | Distributed tracing  |

All services share a `spring_ai` Docker bridge network. Prometheus scrapes `/actuator/prometheus` on the Spring Boot app every 5 seconds.

## API Endpoints

### Basic Chat — `/api`

| Method | Path | Description |
|---|---|---|
| GET | `/api/openai/chat?message=` | OpenAI chat |
| GET | `/api/ollama/chat?message=` | Ollama chat |
| GET | `/api/chat?message=` | IT helpdesk-scoped Ollama chat |
| GET | `/api/stream?message=` | Streaming response |
| GET | `/api/email?customerName=&customerMessage=` | Templated email generation |
| GET | `/api/prompt-stuffing?message=` | Chat with HR policy context |
| GET | `/api/chat-bean?message=` | Structured output as `CountryCities` |
| GET | `/api/chat-list?message=` | Structured output as `List<String>` |
| GET | `/api/chat-map?message=` | Structured output as `Map<String, Object>` |
| GET | `/api/chat-bean-list?message=` | Structured output as `List<CountryCities>` |

### Memory — `/api`

| Method | Path | Headers | Description |
|---|---|---|---|
| GET | `/api/chat-memory?message=` | `username` | Chat with per-user conversation memory |

### RAG — `/api/rag`

| Method | Path | Headers | Description |
|---|---|---|---|
| GET | `/api/rag/random/chat?message=` | `username` | Vector similarity search + memory |
| GET | `/api/rag/document/chat?message=` | `username` | HR document context + memory |
| GET | `/api/rag/web-search/chat?message=` | `username` | Live web search + memory |

### Tool Calling — `/api/tools`

| Method | Path | Headers | Description |
|---|---|---|---|
| GET | `/api/tools/help-desk?message=` | `username` | Helpdesk ticket creation and lookup |
| GET | `/api/tools/local-time?message=` | `username` | Timezone-aware time retrieval |

### Answer Evaluation — `/api/evaluate`

| Method | Path | Description |
|---|---|---|
| GET | `/api/evaluate/chat?message=` | Chat with fact-checking validation |
| GET | `/api/evaluate/prompt-stuffing?message=` | Prompt stuffing with evaluation |

### Image — `/api`

| Method | Path | Description |
|---|---|---|
| GET | `/api/image?message=` | Generate image (base64 JSON) |
| GET | `/api/image-options?message=` | Generate image with size/quality options |

### Audio — `/api`

| Method | Path | Description |
|---|---|---|
| GET | `/api/transcribe` | Transcribe embedded `SpringAI.mp3` |
| GET | `/api/transcribe-options` | Transcribe with language/format options (VTT) |
| GET | `/api/speech?message=` | Synthesize text to MP3 |
| GET | `/api/speech-options?message=` | Synthesize with voice/speed options |

## Architecture

### ChatClientFactory

Central factory that creates pre-configured `ChatClient.Builder` instances. Every client gets `SimpleLoggerAdvisor` and `TokenLoggerAdvisor` by default. Callers supply additional advisors (memory, RAG, web search) at bean creation time.

Available factory methods:
- `createOllama(advisors)` — Ollama/llama3.1
- `createOpenAi(advisors)` — OpenAI
- `createBespokeMinicheck()` — Ollama/bespoke-minicheck (used for fact-checking)

### Advisors

| Advisor | Purpose |
|---|---|
| `SimpleLoggerAdvisor` | Spring AI built-in request/response logging |
| `TokenLoggerAdvisor` | Logs prompt, completion, and total token counts |
| `MemoryAdvisor` | Wraps `MessageChatMemoryAdvisor` for conversation history |
| `RagAdvisor` | Vector retrieval with query translation and PII masking |
| `WebSearchAdvisor` | Real-time web retrieval via Firecrawl |

### RAG Pipeline

1. **Query Translation** — `TranslationQueryTransformer` normalises queries to English
2. **Retrieval** — Qdrant vector search (top-K=3, similarity threshold=0.5) or web search
3. **Post-processing** — `PIIMaskingDocumentPostProcessor` redacts emails and phone numbers before the context is sent to the LLM

### Tool Calling

Tools are plain Spring beans annotated with `@Tool`. The LLM decides when to invoke them.

| Tool class | Tools | Notes |
|---|---|---|
| `HelpDeskTools` | Create ticket, get tickets by user | Uses `ToolContext` for username injection; ticket creation returns directly without re-processing |
| `TimeTools` | Current local time, time in specified timezone | Demonstrates timezone-aware tool results |

### Conversation Memory

- Backend: `MessageWindowChatMemory` with `JdbcChatMemoryRepository` (H2 file database)
- Scoped per user via the `username` request header as `conversation_id`
- Window: last 10 messages retained

### Answer Evaluation

`SelfEvaluatingChatController` uses `FactCheckingEvaluator` backed by `bespokeMinicheckChatClient` to validate LLM responses. Responses that fail validation throw `InvalidAnswerException`, returning HTTP 400.

### Structured Output

The project demonstrates all Spring AI structured output strategies:
- `BeanOutputConverter` — single entity
- `ListOutputConverter` — `List<String>`
- `MapOutputConverter` — `Map<String, Object>`
- `BeanOutputConverter` with `ParameterizedTypeReference` — `List<CountryCities>`

## Configuration Reference

Key constants (`Constants.java`):

| Constant | Value |
|---|---|
| Temperature | 0.7 |
| Max tokens | 250 |
| Max memory messages | 10 |
| RAG top-K | 3 |
| RAG similarity threshold | 0.5 |
| Chunk size | 200 tokens |
| Max web search results | 3 |
| Image size | 1024×1024 |

## Resources (`src/main/resources`)

| File | Purpose |
|---|---|
| `application.properties` | Spring Boot configuration: datasource, AI provider settings, actuator/metrics, and tracing |
| `schema/schema-h2db.sql` | DDL for the `SPRING_AI_CHAT_MEMORY` table backing JDBC-based conversation memory |
| `HR_Policies.pdf` | Sample HR policy document, loaded into the vector store by `HRPolicyLoader` for RAG demos |
| `SpringAI.mp3` | Sample audio clip used by `AudioController`'s transcription endpoints |
| `promptTemplates/systemPromptTemplate.st` | System prompt (with `{documents}` placeholder) used for HR-policy RAG chat in `RagController#randomChat` |
| `promptTemplates/systemPromptRandomDataTemplate.st` | System prompt template (with `{documents}` placeholder) intended for RAG over the `RandomDataLoader` sentence dataset; currently unreferenced in code |
| `promptTemplates/hrPolicyTemplate.st` | HR policy summary (leave, working hours, benefits, etc.) used as the system prompt for prompt-stuffing demos in `MultiModelChatController#promptStuff` and `SelfEvaluatingChatController#promptStuff` |
| `promptTemplates/helpDeskSystemPromptTemplate.st` | System prompt defining the virtual help-desk assistant persona and ticket-handling rules, used by the `helpDeskChatClient` bean |
| `promptTemplates/userPromptTemplate.st` | Template (with `{customerName}`/`{customerMessage}` placeholders) for generating customer support emails in `MultiModelChatController#email` |

## Observability

- **Metrics**: Exposed at `/actuator/prometheus`, scraped by Prometheus
- **Tracing**: 100% sampling (`management.tracing.sampling.probability=1.0`), exported to Zipkin
- **Token usage**: Logged per request by `TokenLoggerAdvisor`
- **H2 Console**: Available at `/h2-console`