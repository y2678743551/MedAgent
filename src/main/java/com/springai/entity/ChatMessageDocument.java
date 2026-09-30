package com.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDocument {




    /** MessageType.getValue() -> "user" / "assistant" / "system" / "tool"（注意是小写） */
    private String type;

    /** 正文 */
    private String text;

    /** 元数据 */
    private Map<String, Object> metadata;

    /** 仅 ASSISTANT 使用 */
    private List<ToolCallDoc> toolCalls;

    /** 仅 TOOL 使用 */
    private List<ToolResponseDoc> toolResponses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallDoc {
        private String id;
        private String type;
        private String name;
        private String arguments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolResponseDoc {
        private String id;
        private String name;
        private String responseData;
    }

}
