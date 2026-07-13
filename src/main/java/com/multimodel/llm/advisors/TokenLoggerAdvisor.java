package com.multimodel.llm.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * {@link CallAdvisor} that logs token usage (prompt, completion, and total tokens)
 * for each chat call, after the call completes.
 */
public class TokenLoggerAdvisor implements CallAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(TokenLoggerAdvisor.class);

    /**
     * Invokes the next advisor in the chain and logs the resulting token usage,
     * if usage metadata is present on the response.
     *
     * @param chatClientRequest the outgoing chat request
     * @param callAdvisorChain  the chain used to proceed with the call
     * @return the response returned by the chain, unmodified
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        if (chatResponse != null) {
            Usage usage = chatResponse.getMetadata().getUsage();
            logger.info("Token usage — prompt: {}, generation: {}, total: {}",
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens());
        }
        return chatClientResponse;
    }

    /**
     * @return the simple class name, used to identify this advisor in the advisor chain
     */
    @Override
    public String getName() {
        return TokenLoggerAdvisor.class.getSimpleName();
    }

    /**
     * Runs this advisor with the lowest possible order value so it executes
     * outermost in the advisor chain, wrapping all other advisors.
     *
     * @return {@link Integer#MIN_VALUE}
     */
    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}