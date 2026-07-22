# Overall architecture flow

Every REST controller talks to a `ChatClient` built by `ChatClientFactory`, which wraps the
underlying model (Ollama or OpenAI) with a default advisor chain (logging, token logging, plus
memory/RAG/tools depending on the client bean).

```mermaid
flowchart TD
    A["REST controller<br/><small>e.g. HelpDeskController</small>"] --> B["ChatClient<br/><small>Built by ChatClientFactory</small>"]
    B --> C["Advisor chain<br/><small>Logger, memory, RAG, web search</small>"]
    C --> D["Model provider<br/><small>Ollama or OpenAI</small>"]
    D --> E{{Infrastructure}}
    E --> E1["H2<br/><small>Chat memory</small>"]
    E --> E2["Qdrant<br/><small>Vector store</small>"]
    E --> E3["Tavily<br/><small>Web search</small>"]
    E1 --> F["Response<br/><small>Logged, returned to controller</small>"]
    E2 --> F
    E3 --> F
```

## Relevant classes

| Component | Source |
|---|---|
| Chat client assembly | `ChatClientFactory.java` |
| Client bean wiring | `ChatClientConfig.java` |
| Shared constants (temperature, max tokens) | `Constants.java` |
| Token usage logging | `TokenLoggerAdvisor.java` |
