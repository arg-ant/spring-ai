# RAG pipeline flow

This is the flow behind `ragMemoryChatClient` / `RagAdvisor`: a query gets translated, searched
against Qdrant, PII-masked, then injected into the prompt.

```mermaid
flowchart TD
    A["User query<br/><small>Via chat endpoint</small>"] --> B["Translate query<br/><small>Normalize to english</small>"]
    B --> C["Vector search<br/><small>Top-3, similarity ≥ 0.5</small>"]
    C --> D["Mask PII<br/><small>Redact emails, phone numbers</small>"]
    D --> E["Inject context<br/><small>Documents into system prompt</small>"]
    E --> F["Generate answer<br/><small>Ollama LLM response</small>"]
```

## Relevant classes

| Stage | Source |
|---|---|
| Query translation | `TranslationQueryTransformer` (configured in `RagAdvisor.java`) |
| Vector retrieval | `VectorStoreDocumentRetriever` (configured in `RagAdvisor.java`) |
| PII masking | `PIIMaskingDocumentPostProcessor.java` |
| Advisor assembly | `RagAdvisor.java` |
| Manual variant (no advisor) | `RagController.java#randomChat` |
| Similarity / top-K constants | `Constants.java` (`TOP_K`, `SIMILARITY_THRESHOLD`) |
