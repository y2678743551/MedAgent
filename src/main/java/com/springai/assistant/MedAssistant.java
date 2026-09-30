package com.springai.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 医疗助手：按 memoryId 做会话隔离，system prompt 来自 classpath 下的 prompt 文件。
 */
@Component
public class MedAssistant {

    private final ChatClient chatClient;
    private final String systemPrompt;

    public MedAssistant(ChatClient chatClient) throws IOException {
        this.chatClient = chatClient;
        this.systemPrompt = StreamUtils.copyToString(
                new ClassPathResource("prompt").getInputStream(),
                StandardCharsets.UTF_8)
                .replace("{{current_date}}", LocalDate.now().toString());
    }

    public String chat(Long memoryId, String message) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(memoryId)))
                .call()
                .content();
    }

}
