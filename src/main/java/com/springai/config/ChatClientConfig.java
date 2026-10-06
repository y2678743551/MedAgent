package com.springai.config;

import com.springai.repository.MongoChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ChatClientConfig {

    @Value("${app.rag.knowledge-path:file:D:/knowledge/*.md}")
    private String knowledgePath;

    @Value("${app.rag.load-on-startup:false}")
    private boolean loadOnStartup;

    @Bean
    ChatMemory chatMemory(MongoChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10)
                .build();
    }
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultAdvisors(QuestionAnswerAdvisor
                        .builder(vectorStore)
                        .build())
                .build();
    }
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }


    @Bean
    public ApplicationRunner knowledgeLoader(VectorStore vectorStore) {
        return args -> {
            if (!loadOnStartup) {
                return;
            }
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(knowledgePath);
            if (resources.length == 0) {

                return;
            }
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> allChunks = new ArrayList<>();
            for (Resource resource : resources) {

                List<Document> documents = new TextReader(resource).read();
                allChunks.addAll(splitter.split(documents));

            }
            vectorStore.write(allChunks);

        };
    }

}
