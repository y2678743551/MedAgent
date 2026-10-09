package com.springai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
public class RetrievalEvalTest {

    private static final int TOP_K = 5;

    @Autowired
    private VectorStore vectorStore;

    @Test
    public void evaluateRetrieval() throws IOException {

        List<EvalCase> cases = loadCases();
        assertFalse(cases.isEmpty(), "评测集为空，检查 src/test/resources/eval/retrieval_eval.tsv");

        int hitCount = 0;
        double mrrSum = 0;
        boolean sawSourceMetadata = false;
        List<String> misses = new ArrayList<>();

        for (EvalCase c : cases) {

            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(c.question()).topK(TOP_K).build());

            if (docs != null && docs.stream().anyMatch(d -> d.getMetadata().containsKey("source"))) {
                sawSourceMetadata = true;
            }

            int rank = firstHitRank(docs, c);
            if (rank > 0) {
                hitCount++;
                mrrSum += 1.0 / rank;
            } else {
                misses.add(c.question() + "  -> 期望 " + c.expectedSource() + " 含「" + c.expectedKeyword() + "」");
            }
            System.out.printf("[%s] %s%n", rank > 0 ? ("rank " + rank) : "MISS ", c.question());
        }

        double hitRate = (double) hitCount / cases.size();
        double mrr = mrrSum / cases.size();

        System.out.println();
        System.out.println("================ 检索评测 ================");
        System.out.printf("题目数        : %d%n", cases.size());
        System.out.printf("命中          : %d%n", hitCount);
        System.out.printf("HitRate@%d    : %.3f%n", TOP_K, hitRate);
        System.out.printf("MRR@%d        : %.3f%n", TOP_K, mrr);
        System.out.println("==========================================");

        if (!misses.isEmpty()) {
            System.out.println("未命中列表：");
            misses.forEach(m -> System.out.println("  - " + m));
        }

        if (!sawSourceMetadata) {
            System.out.println();
            System.out.println("  前 " + TOP_K + " 条里没有一条带 source 元数据。");
            System.out.println("  说明向量库还是旧数据 —— 请先跑 PineconeTest 用 MarkdownSplitter 重新灌库，");
            System.out.println("  否则本评测的 source 判定条件永远不成立，指标必然为 0。");
        }
    }

    /**
     * 返回第一条同时命中「来源文件 + 关键词」的排名（从 1 开始）；没有则返回 0。
     */
    private int firstHitRank(List<Document> docs, EvalCase c) {
        if (docs == null) {
            return 0;
        }
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            Object source = doc.getMetadata().get("source");
            if (source == null || !String.valueOf(source).equals(c.expectedSource())) {
                continue;
            }
            String text = doc.getText();
            if (text != null && text.contains(c.expectedKeyword())) {
                return i + 1;
            }
        }
        return 0;
    }

    private List<EvalCase> loadCases() throws IOException {
        ClassPathResource resource = new ClassPathResource("eval/retrieval_eval.tsv");
        List<EvalCase> cases = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    line = line.replace("\uFEFF", "");
                    firstLine = false;
                }
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                if (fields.length < 3 || "question".equals(fields[0].trim())) {
                    continue;
                }
                cases.add(new EvalCase(
                        fields[0].trim(),
                        fields[1].trim(),
                        fields[2].trim(),
                        fields.length > 3 ? fields[3].trim() : ""));
            }
        }
        return cases;
    }

    private record EvalCase(String question, String expectedSource, String expectedKeyword, String note) {
    }
}
