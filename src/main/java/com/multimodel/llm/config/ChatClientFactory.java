package com.multimodel.llm.config;

import com.multimodel.llm.advisors.TokenUsageLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

@Component
public class ChatClientFactory {

    private final OllamaChatModel ollamaChatModel;
    private final OpenAiChatModel openAiChatModel;
    private final OllamaApi ollamaApi;

    public ChatClientFactory(OllamaChatModel ollamaChatModel,
                             OpenAiChatModel openAiChatModel,
                             OllamaApi ollamaApi) {
        this.ollamaChatModel = ollamaChatModel;
        this.openAiChatModel = openAiChatModel;
        this.ollamaApi = ollamaApi;
    }

    public ChatClient.Builder createOllama() {
        return create(ollamaChatModel);
    }

    public ChatClient.Builder createOpenAi() {
        return create(openAiChatModel);
    }

    public ChatClient.Builder createBespokeMinicheck() {
        OllamaChatModel bespokeModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model("bespoke-minicheck")
                        .build())
                .build();
        return ChatClient.builder(bespokeModel);
    }

    public ChatClient.Builder create(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(defaultChatOptions().mutate())
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new TokenUsageLoggerAdvisor());
    }

    private ChatOptions defaultChatOptions() {
        return ChatOptions.builder()
                .temperature(0.7)
                .maxTokens(250)
                .build();
    }
}
