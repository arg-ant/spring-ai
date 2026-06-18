package com.multimodel.llm.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

public class TokenLoggerAdvisor implements CallAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(TokenLoggerAdvisor.class);

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

    @Override
    public String getName() {
        return TokenLoggerAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}