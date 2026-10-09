package com.springai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

public class TokenCountAdvisor implements BaseAdvisor {
    private static final Logger log = LoggerFactory.getLogger(TokenCountAdvisor.class);
    private final int order;

    public TokenCountAdvisor() { this(Ordered.LOWEST_PRECEDENCE - 100); }
    public TokenCountAdvisor(int order) { this.order = order; }
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        if (chatClientResponse.chatResponse() == null || chatClientResponse.chatResponse().getMetadata() == null) {
            return chatClientResponse;
        }
        Usage usage = chatClientResponse.chatResponse().getMetadata().getUsage();
        if (usage != null && usage.getPromptTokens() != null && usage.getCompletionTokens() != null) {
            log.info("[TOKEN] prompt={} completion={} total={}",
                    usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        }
        return chatClientResponse;
    }




    @Override
    public int getOrder() {
        return order;
    }
}
