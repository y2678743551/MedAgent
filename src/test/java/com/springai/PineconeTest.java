package com.springai;

import com.springai.splitter.MarkdownSplitter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@SpringBootTest
public class PineconeTest {



    private static final List<String> SUPPORTED = List.of("md", "txt", "pdf");



    @Autowired
    private VectorStore vectorStore;
    @Value("${app.rag.knowledge-path:file:D:/knowledge/*.md}")
    private String knowledgePath;
    @Test
    public void testUploadKnowledgeLibraryByMarkdownSplitter() throws IOException {

        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(knowledgePath);
        if (resources.length == 0) {
            System.out.println("[RAG] 未匹配到任何知识文件：" + knowledgePath);
            return;
        }
        System.out.println("[RAG] 知识路径：" + knowledgePath);

        List<Resource> selected = new ArrayList<>();
        List<String> excluded = new ArrayList<>();
        List<String> deduped = new ArrayList<>();
        select(resources, selected, excluded, deduped);

        System.out.println("[RAG] 排除（不在灌库范围）：" + join(excluded));
        System.out.println("[RAG] 同名去重跳过：" + join(deduped));

        MarkdownSplitter splitter = new MarkdownSplitter();
        List<Document> allChunks = new ArrayList<>();
        for (Resource resource : selected) {
            String source = resource.getFilename();
            List<Document> chunks = splitter.split(source, readText(resource, source));
            allChunks.addAll(chunks);
            System.out.println("[RAG] " + source + " -> " + chunks.size() + " 块");
        }

        vectorStore.write(allChunks);
        System.out.println("[RAG] 共灌入 " + allChunks.size() + " 块");
    }
    @Test
    public void testUploadKnowledgeLibraryByTokenSplitter() throws IOException {

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

    /**
     * @param selected 按优先级选出、真正要灌库的文件（保持稳定顺序）
     * @param excluded 命中排除名单的文件名
     * @param deduped  因同名而被跳过的文件名（含实际采用了哪一份）
     */
    private void select(Resource[] resources, List<Resource> selected,
                        List<String> excluded, List<String> deduped) {

        Map<String, List<Resource>> byExtension = new LinkedHashMap<>();
        for (String extension : SUPPORTED) {
            byExtension.put(extension, new ArrayList<>());
        }
        for (Resource resource : resources) {
            String name = resource.getFilename();
            if (name == null) {
                continue;
            }

            String extension = extensionOf(name);
            if (byExtension.containsKey(extension)) {
                byExtension.get(extension).add(resource);
            }
        }

        // 外层按优先级从高到低遍历，同一个基名先到先得
        Map<String, Resource> pickedByBaseName = new LinkedHashMap<>();
        for (String extension : SUPPORTED) {
            for (Resource resource : byExtension.get(extension)) {
                String name = resource.getFilename();
                String baseName = name.substring(0, name.length() - extension.length() - 1);
                Resource alreadyPicked = pickedByBaseName.putIfAbsent(baseName, resource);
                if (alreadyPicked == null) {
                    selected.add(resource);
                } else {
                    deduped.add(name + "（同名已采用 " + alreadyPicked.getFilename() + "）");
                }
            }
        }
    }

    /**
     * 按扩展名读取纯文本：PDF 交给 Tika 抽取，md / txt 直接按 UTF-8 读。
     */
    private String readText(Resource resource, String source) throws IOException {
        if (source.toLowerCase().endsWith(".pdf")) {
            StringBuilder text = new StringBuilder();
            for (Document document : new TikaDocumentReader(resource).read()) {
                text.append(document.getText()).append('\n');
            }
            return text.toString();
        }
        return stripBom(resource.getContentAsString(StandardCharsets.UTF_8));
    }

    /** 去掉 UTF-8 BOM，否则第一个标题会被当成 BOM + "#"，切分结果少一段。 */
    private String stripBom(String text) {
        return text != null && text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    /** 取小写扩展名（不含点）；没有扩展名时返回空串。 */
    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private String join(List<String> items) {
        return items.isEmpty() ? "无" : String.join("、", items);
    }
}
