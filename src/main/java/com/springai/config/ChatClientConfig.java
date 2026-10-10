package com.springai.config;

import com.springai.advisor.TokenCountAdvisor;
import com.springai.repository.MongoChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ChatClientConfig {

    @Bean
    ChatMemory chatMemory(MongoChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, @Qualifier("esHybridVectorStore") VectorStore vectorStore) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultAdvisors(QuestionAnswerAdvisor
                        .builder(vectorStore)
                        .build())
                .defaultAdvisors(new TokenCountAdvisor())
                .build();
    }
    @Bean
    public BatchingStrategy batchingStrategy() {
        return documents -> {
            List<List<Document>> batches = new ArrayList<>();
            for (int i = 0; i < documents.size(); i += 10) {
                batches.add(documents.subList(i, Math.min(i + 10, documents.size())));
            }
            return batches;
        };
    }

}
