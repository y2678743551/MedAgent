package com.springai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class PineconeTest {
    @Autowired
    private VectorStore vectorStore;
    @Value("${app.rag.knowledge-path:file:D:/knowledge/*.md}")
    private String knowledgePath;
    @Test
    public void testUploadKnowledgeLibrary() throws IOException {

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

    }
}
