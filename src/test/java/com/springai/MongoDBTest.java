package com.springai;

import com.springai.entity.ChatMemoryDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootTest
public class MongoDBTest {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Test
    public void testMongoDB() {

        System.out.println(mongoTemplate.findDistinct("_id", ChatMemoryDocument.class, String.class)) ;
    }
}
