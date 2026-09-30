package com.springai.repository;


import com.springai.entity.ChatMemoryDocument;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MongoChatMemoryRepository  implements ChatMemoryRepository {
    private final MongoTemplate mongoTemplate;

    public MongoChatMemoryRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    @Override
    public List<String> findConversationIds() {

        return mongoTemplate.findDistinct("_id", ChatMemoryDocument.class, String.class);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Criteria criteria = Criteria.where("_id").is(conversationId);
        Query query = new Query(criteria);
        ChatMemoryDocument memoryDocument = mongoTemplate.findOne(query, ChatMemoryDocument.class);
        if (memoryDocument != null) {
            return ChatMessageCodec.toMessage(memoryDocument.getMessages());
        }
        return List.of();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        ChatMemoryDocument doc = new ChatMemoryDocument(
                conversationId,
                ChatMessageCodec.toDocument(messages));
        mongoTemplate.save(doc);

    }

    @Override
    public void deleteByConversationId(String conversationId) {
        Criteria criteria = Criteria.where("_id").is(conversationId);
        mongoTemplate.remove(new Query(criteria), ChatMemoryDocument.class);
    }
}
