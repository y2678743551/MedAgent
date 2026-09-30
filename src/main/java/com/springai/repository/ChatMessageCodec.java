package com.springai.repository;

import com.springai.entity.ChatMessageDocument;
import org.springframework.ai.chat.messages.*;

import java.util.List;

final public class ChatMessageCodec {
    static public ChatMessageDocument toDocument(Message message) {


        ChatMessageDocument messageDocument = new ChatMessageDocument();
        messageDocument.setType(message.getMessageType().getValue());
        messageDocument.setMetadata(message.getMetadata());
        messageDocument.setText(message.getText());
        if(message instanceof AssistantMessage assistantMessage)
            messageDocument.setToolCalls(assistantMessage.getToolCalls().stream()
                    .map(tc -> new ChatMessageDocument.ToolCallDoc(
                            tc.id(), tc.type(), tc.name(), tc.arguments()))
                    .toList());

        if (message instanceof ToolResponseMessage toolResponseMessage)
            messageDocument.setToolResponses(toolResponseMessage.getResponses().stream()
                    .map(tr -> new ChatMessageDocument.ToolResponseDoc(
                            tr.id(), tr.name(), tr.responseData()))
                    .toList());

        return messageDocument;
    }
    static public Message toMessage(ChatMessageDocument messageDocument) {
        String type = messageDocument.getType();
        return switch (MessageType.fromValue(type)) {
            case SYSTEM -> SystemMessage.builder()
                    .text(messageDocument.getText())
                    .metadata(messageDocument.getMetadata())
                    .build();
            case ASSISTANT -> {
                List<AssistantMessage.ToolCall> toolCalls = messageDocument.getToolCalls().stream()
                        .map(tc -> new AssistantMessage.ToolCall(
                                tc.getId(), tc.getType(), tc.getName(), tc.getArguments()))
                        .toList();
                yield new AssistantMessage(messageDocument.getText(), messageDocument.getMetadata(), toolCalls);
            }
            case USER -> UserMessage.builder()
                    .text(messageDocument.getText())
                    .metadata(messageDocument.getMetadata())
                    .build();
            case TOOL-> {
                List<ToolResponseMessage.ToolResponse> toolResponses = messageDocument.getToolResponses().stream()
                        .map(tr -> new ToolResponseMessage.ToolResponse(
                                tr.getId(), tr.getName(), tr.getResponseData()))
                        .toList();
                yield new ToolResponseMessage(toolResponses, messageDocument.getMetadata());
            }

        };
    }
    static public List<ChatMessageDocument> toDocument(List<Message> messages) {

        return messages==null?List.of():messages.stream().map(ChatMessageCodec::toDocument).toList();
    }
    static public List<Message> toMessage(List<ChatMessageDocument> chatMemoryDocument) {
        return chatMemoryDocument==null?List.of():chatMemoryDocument.stream().map(ChatMessageCodec::toMessage).toList();
    }
}
