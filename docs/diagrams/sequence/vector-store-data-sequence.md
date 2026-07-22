# Vector store data loading — sequence diagram

The exact call order behind the four on-demand data-loading endpoints exposed by
`VectorStoreDataController`, including which object calls which (see
[vector-store-data-class-diagram.md](./vector-store-data-class-diagram.md) for the static
structure).

```mermaid
sequenceDiagram
    actor Client
    participant Controller as VectorStoreDataController
    participant Service as VectorStoreDataService
    participant Tika as TikaDocumentReader
    participant Splitter as TokenTextSplitter
    participant Store as VectorStore

    rect rgb(240, 240, 240)
    note over Client, Store: Load random sentences
    Client->>Controller: POST /api/rag/random/load
    Controller->>Service: loadRandom()
    Service->>Service: getSentences()
    Service->>Service: wrap each sentence in a Document tagged source=random-data
    Service->>Store: add(documents)
    Store-->>Service: void
    Service-->>Controller: void
    Controller-->>Client: "Random data loaded"
    end

    rect rgb(240, 240, 240)
    note over Client, Store: Remove random sentences
    Client->>Controller: DELETE /api/rag/random/remove
    Controller->>Service: removeRandom()
    Service->>Service: build filter source == random-data
    Service->>Store: delete(filterExpression)
    Store-->>Service: void
    Service-->>Controller: void
    Controller-->>Client: "Random data removed"
    end

    rect rgb(240, 240, 240)
    note over Client, Store: Load HR policy PDF
    Client->>Controller: POST /api/rag/document/load
    Controller->>Service: loadPDF()
    Service->>Tika: read()
    Tika-->>Service: documents
    Service->>Splitter: split(documents)
    Splitter-->>Service: chunks
    Service->>Service: tag each chunk source=hr-policy-pdf
    Service->>Store: add(chunks)
    Store-->>Service: void
    Service-->>Controller: void
    Controller-->>Client: "Document data loaded"
    end

    rect rgb(240, 240, 240)
    note over Client, Store: Remove HR policy PDF
    Client->>Controller: DELETE /api/rag/document/remove
    Controller->>Service: removePDF()
    Service->>Service: build filter source == hr-policy-pdf
    Service->>Store: delete(filterExpression)
    Store-->>Service: void
    Service-->>Controller: void
    Controller-->>Client: "Document data removed"
    end
```

## Relevant classes

| Participant | Source |
|---|---|
| `VectorStoreDataController` | `VectorStoreDataController.java` |
| `VectorStoreDataService` | `VectorStoreDataService.java` |
| `TikaDocumentReader` | Spring AI (`org.springframework.ai.reader.tika.TikaDocumentReader`) |
| `TokenTextSplitter` | Spring AI (`org.springframework.ai.transformer.splitter.TokenTextSplitter`) |
| `VectorStore` | Spring AI (`org.springframework.ai.vectorstore.VectorStore`) |
