package com.multimodel.llm.advisors;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;

/**
 * Factory for {@link Advisor} instances that add conversational memory to a
 * {@link org.springframework.ai.chat.client.ChatClient}.
 */
public class MemoryAdvisor {

    /**
     * Creates an advisor that injects prior messages from the given {@link ChatMemory}
     * into each chat request, enabling multi-turn conversations.
     *
     * @param chatMemory the chat memory store to read/write conversation history from
     * @return an {@link Advisor} backed by a {@link MessageChatMemoryAdvisor}
     */
    public static Advisor memoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}

