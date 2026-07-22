# RAG document chat — sequence diagram

The exact call order behind the activity diagram in
[rag-pipeline.md](./rag-pipeline.md), including which object calls which.

```mermaid
sequenceDiagram
    actor Client
    participant Controller as RagController
    participant ChatClient as ChatClient (ragMemoryChatClient)
    participant RagAdv as RetrievalAugmentationAdvisor
    participant Translator as TranslationQueryTransformer
    participant Retriever as VectorStoreDocumentRetriever
    participant Qdrant
    participant PII as PIIMaskingDocumentPostProcessor
    participant Model as Ollama model

    Client->>Controller: GET /rag/document/chat?message=... (username header)
    Controller->>ChatClient: prompt().advisors(CONVERSATION_ID).user(message).call()
    ChatClient->>RagAdv: augment request
    RagAdv->>Translator: translate(query)
    Translator-->>RagAdv: english query
    RagAdv->>Retriever: retrieve(query)
    Retriever->>Qdrant: similaritySearch(topK=3, threshold=0.5)
    Qdrant-->>Retriever: matching documents
    Retriever-->>RagAdv: documents
    RagAdv->>PII: process(documents)
    PII-->>RagAdv: masked documents
    RagAdv-->>ChatClient: prompt + context
    ChatClient->>Model: generate(prompt)
    Model-->>ChatClient: answer
    ChatClient-->>Controller: answer
    Controller-->>Client: 200 OK
```

## Relevant classes

| Participant | Source |
|---|---|
| `RagController` | `RagController.java` |
| `ChatClient` (ragMemoryChatClient bean) | `ChatClientConfig.java#ragMemoryChatClient` |
| `RetrievalAugmentationAdvisor` | `RagAdvisor.java` |
| `TranslationQueryTransformer` | Spring AI RAG module, configured in `RagAdvisor.java` |
| `VectorStoreDocumentRetriever` | Spring AI RAG module, configured in `RagAdvisor.java` |
| `PIIMaskingDocumentPostProcessor` | `PIIMaskingDocumentPostProcessor.java` |
