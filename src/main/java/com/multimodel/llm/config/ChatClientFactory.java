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

/**
 * Factory for building pre-configured {@link ChatClient.Builder} instances.
 * <p>
 * Centralizes default {@link ChatOptions} (temperature, max tokens) and default
 * advisors (request/response logging and token usage logging) so that callers
 * don't need to repeat this wiring for each {@link ChatModel} they use.
 */
@Component
public class ChatClientFactory {

    private final OllamaChatModel ollamaChatModel;
    private final OpenAiChatModel openAiChatModel;
    private final OllamaApi ollamaApi;

    /**
     * Creates the factory with the chat models and API client it will build clients from.
     *
     * @param ollamaChatModel the default Ollama chat model
     * @param openAiChatModel the default OpenAI chat model
     * @param ollamaApi       the Ollama API client used to build bespoke Ollama models
     */
    public ChatClientFactory(
            OllamaChatModel ollamaChatModel,
            OpenAiChatModel openAiChatModel,
            OllamaApi ollamaApi) {
        this.ollamaChatModel = ollamaChatModel;
        this.openAiChatModel = openAiChatModel;
        this.ollamaApi = ollamaApi;
    }

    /**
     * Builds a {@link ChatClient.Builder} backed by the default Ollama chat model.
     *
     * @param advisors additional advisors to register alongside the default ones
     * @return a configured {@link ChatClient.Builder}
     */
    public ChatClient.Builder createOllama(Advisor... advisors) {
        return create(ollamaChatModel, advisors);
    }

    /**
     * Builds a {@link ChatClient.Builder} backed by the default OpenAI chat model.
     *
     * @param advisors additional advisors to register alongside the default ones
     * @return a configured {@link ChatClient.Builder}
     */
    public ChatClient.Builder createOpenAi(Advisor... advisors) {
        return create(openAiChatModel, advisors);
    }

    /**
     * Builds a {@link ChatClient.Builder} backed by a dedicated Ollama chat model
     * configured to use the bespoke Minicheck model.
     *
     * @return a configured {@link ChatClient.Builder}
     */
    public ChatClient.Builder createBespokeMinicheck() {
        OllamaChatModel bespokeModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(BESPOKE_MINICHECK_MODEL)
                        .build())
                .build();

        return create(bespokeModel);
    }

    /**
     * Builds a {@link ChatClient.Builder} for the given chat model, applying the
     * default chat options and default advisors ({@link SimpleLoggerAdvisor} and
     * {@link TokenLoggerAdvisor}), plus any additional advisors supplied.
     *
     * @param chatModel          the chat model to back the client with
     * @param additionalAdvisors optional extra advisors appended to the defaults
     * @return a configured {@link ChatClient.Builder}
     */
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

    /**
     * Builds the default {@link ChatOptions} shared by all clients created by this factory.
     *
     * @return chat options configured with the default temperature and max tokens
     */
    private ChatOptions defaultChatOptions() {
        return ChatOptions.builder()
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .build();
    }
}
