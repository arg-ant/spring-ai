# Vector store data loading — class diagram

Structure of the classes involved in loading and removing sample data (random sentences and
the HR policies PDF) from the vector store, as used in the on-demand data-loading endpoints
(see [vector-store-data-sequence.md](./vector-store-data-sequence.md)).

```mermaid
classDiagram
    class VectorStoreDataController {
      -VectorStoreDataService randomDataService
      +loadRandom() String
      +removeRandom() String
      +loadPDF() String
      +removePDF() String
    }
    class VectorStoreDataService {
      -String RANDOM_DATA$
      -String PDF_DATA$
      -Resource policyFile
      -VectorStore vectorStore
      +loadRandom() void
      +removeRandom() void
      +loadPDF() void
      +removePDF() void
      -getSentences() List~String~
    }
    class VectorStore {
      <<interface>>
      +add(documents) void
      +delete(filterExpression) void
    }
    class Document {
      -Map~String,Object~ metadata
    }
    class TikaDocumentReader {
      +read() List~Document~
    }
    class TokenTextSplitter {
      +split(documents) List~Document~
    }
    class FilterExpressionBuilder {
      +eq(key, value) FilterExpressionBuilder
      +build() Filter.Expression
    }

    VectorStoreDataController --> VectorStoreDataService
    VectorStoreDataService --> VectorStore
    VectorStoreDataService ..> TikaDocumentReader : loadPDF
    VectorStoreDataService ..> TokenTextSplitter : loadPDF
    VectorStoreDataService ..> FilterExpressionBuilder : removeRandom, removePDF
    VectorStoreDataService ..> Document
    TikaDocumentReader ..> Document
    TokenTextSplitter ..> Document
```

## Relevant classes

| Class | Source |
|---|---|
| `VectorStoreDataController` | `VectorStoreDataController.java` |
| `VectorStoreDataService` | `VectorStoreDataService.java` |
| `VectorStore` | Spring AI (`org.springframework.ai.vectorstore.VectorStore`) |
| `Document` | Spring AI (`org.springframework.ai.document.Document`) |
| `TikaDocumentReader` | Spring AI (`org.springframework.ai.reader.tika.TikaDocumentReader`) |
| `TokenTextSplitter` | Spring AI (`org.springframework.ai.transformer.splitter.TokenTextSplitter`) |
| `FilterExpressionBuilder` | Spring AI (`org.springframework.ai.vectorstore.filter.FilterExpressionBuilder`) |
