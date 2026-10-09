package com.springai.splitter;

import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;



public class MarkdownSplitter {


    private static final String[] SEPARATORS = {"\n\n", "\n", "。", "；", "，", " "};


    public static final int DEFAULT_MAX_CHARS = 800;

    private final int maxChars;

    public MarkdownSplitter() {
        this(DEFAULT_MAX_CHARS);
    }

    public MarkdownSplitter(int maxChars) {
        if (maxChars <= 0) {
            throw new IllegalArgumentException("maxChars must be positive");
        }
        this.maxChars = maxChars;
    }

    /**
     * 把一篇 Markdown 切成带 metadata 的块。
     *
     * @param source   文件名（写进 metadata.source，用于溯源和评测）
     * @param markdown 正文
     */
    public List<Document> split(String source, String markdown) {
        List<Document> chunks = new ArrayList<>();
        if (!StringUtils.hasText(markdown)) {
            return chunks;
        }
        int chunkIndex = 0;
        for (Section section : splitByHeading(markdown)) {
            List<String> parts = splitByLength(section.body());
            if (parts.isEmpty()) {
                // 只有标题没有正文的小节，直接跳过（不占用块序号）
                continue;
            }
            for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("source", source);
                metadata.put("heading", section.heading());
                metadata.put("chunk_index", chunkIndex);
                metadata.put("part_index", partIndex);
                if (parts.size() > 1) {
                    metadata.put("part_count", parts.size());
                }
                chunks.add(new Document(source + "#" + chunkIndex + "-" + partIndex, parts.get(partIndex), metadata));
            }
            chunkIndex++;
        }
        return chunks;
    }

    private record Section(String heading, String body) {
    }

    /**
     * 按 Markdown 标题层级切段，并把标题路径拼成 {@code 一级 > 二级 > 三级}。
     */
    private List<Section> splitByHeading(String markdown) {
        List<Section> sections = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        StringBuilder body = new StringBuilder();
        String heading = "";
        for (String line : markdown.split("\r?\n", -1)) {
            int level = headingLevel(line);
            if (level == 0) {
                body.append(line).append('\n');
                continue;
            }
            sections.add(new Section(heading, body.toString()));
            body.setLength(0);
            // 回退到当前层级的父级：level=2 时只保留 1 层标题
            while (titles.size() >= level) {
                titles.remove(titles.size() - 1);
            }
            titles.add(line.substring(level).trim());
            heading = String.join(" > ", titles);
        }
        sections.add(new Section(heading, body.toString()));
        return sections;
    }

    /**
     * 返回标题层级；不是标题（或 {@code #} 后面没有空格）时返回 0。
     */
    private int headingLevel(String line) {
        if (line == null || line.isEmpty() || line.charAt(0) != '#') {
            return 0;
        }
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        // 必须是 "# 标题" 这种形式，"#标签" 不算
        if (level > 6 || level >= line.length() || line.charAt(level) != ' ') {
            return 0;
        }
        return level;
    }

    private List<String> splitByLength(String body) {
        String text = body.strip();
        if (text.isEmpty()) {
            return List.of();
        }
        return splitRecursively(text, 0);
    }

    /**
     * 递归降级切分：先用最自然的分隔符切，合并到 maxChars 以内；
     * 还超长的片段换下一级分隔符继续切，最后兜底按长度硬切。
     */
    private List<String> splitRecursively(String text, int separatorIndex) {
        if (text.length() <= maxChars) {
            return List.of(text);
        }
        if (separatorIndex >= SEPARATORS.length) {
            return hardSplit(text);
        }
        List<String> pieces = splitKeepingSeparator(text, SEPARATORS[separatorIndex]);
        if (pieces.size() <= 1) {
            // 这个分隔符切不动，换下一级
            return splitRecursively(text, separatorIndex + 1);
        }
        List<String> result = new ArrayList<>();
        for (String packed : pack(pieces)) {
            if (packed.length() > maxChars) {
                result.addAll(splitRecursively(packed, separatorIndex + 1));
            } else {
                result.add(packed);
            }
        }
        return result;
    }

    /**
     * 按分隔符切，把分隔符留在前一片的末尾，保证拼回去还是原文。
     */
    private List<String> splitKeepingSeparator(String text, String separator) {
        List<String> pieces = new ArrayList<>();
        int from = 0;
        int at;
        while ((at = text.indexOf(separator, from)) >= 0) {
            pieces.add(text.substring(from, at + separator.length()));
            from = at + separator.length();
        }
        if (from < text.length()) {
            pieces.add(text.substring(from));
        }
        return pieces;
    }

    /**
     * 贪心合并：把碎片按顺序塞进不超过 maxChars 的桶里。
     */
    private List<String> pack(List<String> pieces) {
        List<String> packed = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String piece : pieces) {
            if (buffer.length() > 0 && buffer.length() + piece.length() > maxChars) {
                packed.add(buffer.toString());
                buffer.setLength(0);
            }
            buffer.append(piece);
        }
        if (buffer.length() > 0) {
            packed.add(buffer.toString());
        }
        return packed;
    }

    private List<String> hardSplit(String text) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < text.length(); i += maxChars) {
            parts.add(text.substring(i, Math.min(i + maxChars, text.length())));
        }
        return parts;
    }
}
