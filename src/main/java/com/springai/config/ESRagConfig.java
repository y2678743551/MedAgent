package com.springai.config;

import com.springai.vectorStore.EsHybridVectorStore;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ESRagConfig {
    @Bean
    public VectorStore esHybridVectorStore(RestClient restClient, EmbeddingModel embeddingModel) {
        try {
            return new EsHybridVectorStore(restClient, embeddingModel);
        } catch (Exception e) {
            throw new RuntimeException("初始化ES Hybrid Vector Store失败",e);
        }

    }
}
