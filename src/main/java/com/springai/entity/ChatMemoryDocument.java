package com.springai.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Document("chat_memory")
@Data
public class ChatMemoryDocument {
    @Id
    private String conversationId;

    private List<ChatMessageDocument> messages;

    private LocalDateTime updatedTime;
    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expireAt;
}
