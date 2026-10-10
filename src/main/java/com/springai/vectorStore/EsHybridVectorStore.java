package com.springai.vectorStore;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;


public class EsHybridVectorStore implements VectorStore {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger log = org.slf4j.LoggerFactory.getLogger(EsHybridVectorStore.class);
    private final RestClient restClient;
    private final EmbeddingModel embeddingModel;
    private final String indexName="med_knowledge";
    private final int candidateTopK;
    private final int numCandidates;
    private static final int RRF_K=60;
    double W_KNN = 1.0, W_BM25 = 0.5;
    public EsHybridVectorStore(RestClient restClient, EmbeddingModel embeddingModel) throws Exception {
        this(restClient, embeddingModel, 50, 500);
    }
    public EsHybridVectorStore(RestClient restClient, EmbeddingModel embeddingModel, int candidateTopK,int numCandidates) throws Exception {

        this.restClient = restClient;
        this.embeddingModel = embeddingModel;
        this.candidateTopK = candidateTopK;
        this.numCandidates = numCandidates;
        this.ensureIndex();
    }

    private void ensureIndex() throws Exception {
        try {
            restClient.performRequest(new Request("HEAD", "/" + indexName));
            return;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() != 404) throw e;
        }
        String mapping = """
        {
          "mappings": {
            "properties": {
              "content":     {"type":"text","analyzer":"cjk"},
              "heading":     {"type":"text","analyzer":"cjk"},
              "source":      {"type":"keyword"},
              "chunk_index": {"type":"integer"},
              "part_index":  {"type":"integer"},
              "embedding":   {"type":"dense_vector","dims":1024,"index":true,"similarity":"cosine"}
            }
          },
          "_source": {"excludes": ["embedding"]}
        }
        """;
        Request put = new Request("PUT", "/" + indexName);
        put.setJsonEntity(mapping);
        restClient.performRequest(put);
    }



    @Override
    public void add(List<Document> documents) {
        if (documents.isEmpty())
            return;
        try {
            List<String> texts = documents.stream().map(Document::getText).toList();
            List<float[]> embeddings = new ArrayList<>(documents.size()) ;
            for (int from=0;from<documents.size();from+=10){
                int to=Math.min(from+10,documents.size());
                embeddings.addAll(embeddingModel.embed(texts.subList(from, to)));
            }

            StringBuilder nd=new StringBuilder();
            for (int i=0;i<embeddings.size();i++) {
                Document document = documents.get(i);
                Map<String, Object> indexMeta = new HashMap<>();
                indexMeta.put("_id", document.getId());
                indexMeta.put("_index", indexName);
                Map<String, Object> head=new HashMap<>();
                head.put("index", indexMeta);
                nd.append(objectMapper.writeValueAsString(head)).append('\n');
                Map<String, Object> body = new HashMap<>();
                body.put("content", document.getText());
                body.put("heading", document.getMetadata().get("heading"));
                body.put("source", document.getMetadata().get("source"));
                body.put("chunk_index", document.getMetadata().get("chunk_index"));
                body.put("part_index", document.getMetadata().get("part_index"));
                body.put("embedding", embeddings.get(i));

                nd.append(objectMapper.writeValueAsString(body)).append('\n');
            }
            Request request = new Request("POST", "/_bulk?refresh=true");
            request.setEntity(new NStringEntity(nd.toString(), ContentType.create("application/x-ndjson", "UTF-8")));

            Response response = restClient.performRequest(request);


            JsonNode root = objectMapper.readTree(EntityUtils.toString(response.getEntity()));
            if (root.path("errors").asBoolean(false)) {
                log.error("部分文档写入失败：{}", root.path("items"));
            } else {
                log.info("写入 {} 条，耗时 {}ms", root.path("items").size(), root.path("took").asInt());
            }

        } catch (Exception e) {
            throw new RuntimeException("写入ES失败",e);
        }
    }

    @Override
    public void delete(List<String> idList) {
        if (idList.isEmpty()) {
            return;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.set("query", objectMapper.createObjectNode().set("terms", objectMapper.createObjectNode().set("_id", objectMapper.valueToTree(idList))));
            Request req = new Request("POST", "/" + indexName + "/_delete_by_query");
            req.setJsonEntity(body.toString());
            restClient.performRequest(req);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        if (filterExpression == null) {
            return;
        }
        try {
            ObjectNode bool = objectMapper.createObjectNode();
            bool.set("filter", objectMapper.createArrayNode().add(toEsFilter(filterExpression)));
            ObjectNode body = objectMapper.createObjectNode();
            body.set("query", objectMapper.createObjectNode().set("bool", bool));
            Request req = new Request("POST", "/" + indexName + "/_delete_by_query");
            req.setJsonEntity(body.toString());
            restClient.performRequest(req);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private JsonNode toEsFilter(Filter.Expression expr) {
        Filter.ExpressionType type = expr.type();
        ObjectNode bool = objectMapper.createObjectNode();
        switch (type) {
            case AND, OR -> {
                String key = type == Filter.ExpressionType.AND ? "must" : "should";
                bool.set(key, objectMapper.createArrayNode()
                        .add(toEsFilter((Filter.Expression) expr.left()))
                        .add(toEsFilter((Filter.Expression) expr.right())));
            }
            case NOT -> bool.set("must_not",
                    objectMapper.createArrayNode().add(toEsFilter((Filter.Expression) expr.left())));
            case EQ, NE, GT, GTE, LT, LTE, IN, NIN -> {
                String field = ((Filter.Key) expr.left()).key();
                Object value = ((Filter.Value) expr.right()).value();
                if (type == Filter.ExpressionType.EQ) {
                    bool.set("filter", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .set("term", objectMapper.createObjectNode()
                                            .put(field, String.valueOf(value)))));
                } else if (type == Filter.ExpressionType.NE) {
                    bool.set("must_not", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .set("term", objectMapper.createObjectNode()
                                            .put(field, String.valueOf(value)))));
                } else if (type == Filter.ExpressionType.IN) {
                    ArrayNode terms = objectMapper.createArrayNode();
                    if (value instanceof List<?> list) {
                        list.forEach(v -> terms.add(String.valueOf(v)));
                    } else if (value instanceof Object[] arr) {
                        for (Object v : arr) {
                            terms.add(String.valueOf(v));
                        }
                    } else {
                        terms.add(String.valueOf(value));
                    }
                    bool.set("filter", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .set("terms", objectMapper.createObjectNode()
                                            .set(field, terms))));
                } else if (type == Filter.ExpressionType.NIN) {
                    ArrayNode terms = objectMapper.createArrayNode();
                    if (value instanceof List<?> list) {
                        list.forEach(v -> terms.add(String.valueOf(v)));
                    } else if (value instanceof Object[] arr) {
                        for (Object v : arr) {
                            terms.add(String.valueOf(v));
                        }
                    } else {
                        terms.add(String.valueOf(value));
                    }
                    bool.set("must_not", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .set("terms", objectMapper.createObjectNode()
                                            .set(field, terms))));
                } else {
                    // GT / GTE / LT / LTE
                    String rangeOp = switch (type) {
                        case GT -> "gt";
                        case GTE -> "gte";
                        case LT -> "lt";
                        case LTE -> "lte";
                        default -> throw new IllegalStateException("unexpected: " + type);
                    };
                    bool.set("filter", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .set("range", objectMapper.createObjectNode()
                                            .set(field, objectMapper.createObjectNode()
                                                    .put(rangeOp, String.valueOf(value))))));
                }
            }
        }
        return bool;
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        try {
            String queryText = request.getQuery();
            int finalK = request.getTopK();
            float[] vector = embeddingModel.embed(queryText);
            ObjectNode bm25JSON = objectMapper.createObjectNode();
            bm25JSON.put("size",candidateTopK);
            bm25JSON.putObject("query").putObject("match").put("content",queryText);
            Request bm25request = new Request("POST", "/" + indexName + "/_search");
            bm25request.setJsonEntity(bm25JSON.toString());
            Response bm25Response = restClient.performRequest(bm25request);

            ObjectNode knnJSON = objectMapper.createObjectNode();
            knnJSON.put("size", candidateTopK);

            ObjectNode knn = knnJSON.putObject("knn");
            knn.put("field", "embedding");
            knn.set("query_vector", objectMapper.valueToTree(vector));
            knn.put("k", candidateTopK);
            knn.put("num_candidates", numCandidates);
            Request knnrequest = new Request("POST", "/" + indexName + "/_search");
            knnrequest.setJsonEntity(knnJSON.toString());
            Response knnResponse = restClient.performRequest(knnrequest);


            JsonNode bm25Hits = objectMapper.readTree(EntityUtils.toString(bm25Response.getEntity()))
                    .path("hits").path("hits");
            JsonNode knnHits  = objectMapper.readTree(EntityUtils.toString(knnResponse.getEntity()))
                    .path("hits").path("hits");
            Map<String, JsonNode> byId = new HashMap<>();

            int limit = request.getTopK() > 0 ? request.getTopK() : finalK;
            Map<String, Double> fused = new HashMap<>();
            for (int i = 0; i < knnHits.size(); i++) {
                JsonNode hit = knnHits.get(i);
                byId.put(hit.path("_id").asText(), hit.path("_source"));
                fused.merge(hit.path("_id").asText(), W_KNN/ (RRF_K + i + 1), Double::sum);
            }
            for (int i = 0; i < bm25Hits.size(); i++) {
                JsonNode hit = bm25Hits.get(i);
                byId.put(hit.path("_id").asText(), hit.path("_source"));
                fused.merge(hit.path("_id").asText(), W_BM25 / (RRF_K + i + 1), Double::sum);
            }
            return fused.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(finalK)
                    .map(e -> {
                        JsonNode src = byId.get(e.getKey());
                        if (src == null) return null;
                        Map<String, Object> meta = new HashMap<>();
                        meta.put("source", src.path("source").asText());
                        meta.put("heading", src.path("heading").asText());
                        meta.put("chunk_index", src.path("chunk_index").asInt());
                        return new Document(e.getKey(), src.path("content").asText(), meta);
                    })
                    .filter(Objects::nonNull)
                    .toList();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }


}

