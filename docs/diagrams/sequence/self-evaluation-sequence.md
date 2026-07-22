# Self-evaluation / fact-checking — sequence diagram

The exact call order behind the activity diagram in
[self-evaluation.md](./self-evaluation.md), including which object calls which.

```mermaid
sequenceDiagram
    actor Client
    participant Controller as SelfEvaluatingChatController
    participant ChatClient as ChatClient (ollamaChatClient)
    participant Evaluator as FactCheckingEvaluator
    participant BespokeClient as ChatClient (bespokeMinicheckChatClient)
    participant ExHandler as GlobalExceptionHandler

    Client->>Controller: GET /evaluate/chat?message=...
    Controller->>ChatClient: prompt(message).call()
    ChatClient-->>Controller: aiResponse
    Controller->>Evaluator: evaluate(question, context, aiResponse)
    Evaluator->>BespokeClient: prompt(fact-check prompt).call()
    BespokeClient-->>Evaluator: verdict
    Evaluator-->>Controller: EvaluationResponse

    alt evaluation passes
        Controller-->>Client: 200 OK (answer)
    else evaluation fails
        Controller->>ExHandler: throw InvalidAnswerException
        ExHandler-->>Client: 400 Bad Request
    end
```

## Relevant classes

| Participant | Source |
|---|---|
| `SelfEvaluatingChatController` | `SelfEvaluatingChatController.java` |
| `ChatClient` (ollamaChatClient bean) | `ChatClientConfig.java#ollamaChatClient` |
| `FactCheckingEvaluator` | Spring AI evaluation module |
| `ChatClient` (bespokeMinicheckChatClient bean) | `ChatClientFactory.java#createBespokeMinicheck` |
| `GlobalExceptionHandler` | `GlobalExceptionHandler.java` |
| `InvalidAnswerException` | `InvalidAnswerException.java` |
