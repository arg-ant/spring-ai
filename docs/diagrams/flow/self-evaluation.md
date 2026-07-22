# Self-evaluation / fact-checking flow

`SelfEvaluatingChatController` generates an answer, then re-checks it with
`FactCheckingEvaluator` (backed by the `bespoke-minicheck` model) before deciding whether to
return it or throw `InvalidAnswerException`.

```mermaid
flowchart TD
    A["User question<br/><small>Plain or HR-policy stuffed</small>"] --> B["Generate answer<br/><small>Ollama chat client</small>"]
    B --> C["Fact-check answer<br/><small>bespoke-minicheck model</small>"]
    C -->|"passes"| D["Return answer<br/><small>200 OK</small>"]
    C -->|"fails"| E["Throw exception<br/><small>InvalidAnswerException</small>"]
    E --> F["GlobalExceptionHandler<br/><small>400 Bad Request</small>"]
```

## Relevant classes

| Component | Source |
|---|---|
| Controller, evaluation logic | `SelfEvaluatingChatController.java` |
| Exception thrown on failed evaluation | `InvalidAnswerException.java` |
| Exception → HTTP mapping | `GlobalExceptionHandler.java` |
| Bespoke fact-checking model bean | `ChatClientFactory.java#createBespokeMinicheck`, `ChatClientConfig.java#bespokeMinicheckChatClient` |
| HR context used for prompt-stuffing variant | `hrPolicyTemplate.st` |
