package com.multimodel.llm.config;

import com.multimodel.llm.advisors.TokenLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import static com.multimodel.llm.config.Constants.*;

@Component
public class ChatClientFactory {

    private final OllamaChatModel ollamaChatModel;
    private final OpenAiChatModel openAiChatModel;
    private final OllamaApi ollamaApi;

    public ChatClientFactory(
            OllamaChatModel ollamaChatModel,
            OpenAiChatModel openAiChatModel,
            OllamaApi ollamaApi) {
        this.ollamaChatModel = ollamaChatModel;
        this.openAiChatModel = openAiChatModel;
        this.ollamaApi = ollamaApi;
    }

    public ChatClient.Builder createOllama(Advisor... advisors) {
        return create(ollamaChatModel, advisors);
    }

    public ChatClient.Builder createOpenAi(Advisor... advisors) {
        return create(openAiChatModel, advisors);
    }

    public ChatClient.Builder createBespokeMinicheck() {
        OllamaChatModel bespokeModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(BESPOKE_MINICHECK_MODEL)
                        .build())
                .build();

        return create(bespokeModel);
    }

    public ChatClient.Builder create(
            ChatModel chatModel,
            Advisor... additionalAdvisors) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultOptions(defaultChatOptions().mutate())
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new TokenLoggerAdvisor()
                );

        if (additionalAdvisors != null && additionalAdvisors.length > 0) {
            builder.defaultAdvisors(additionalAdvisors);
        }

        return builder;
    }

    private ChatOptions defaultChatOptions() {
        return ChatOptions.builder()
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .build();
    }
}
