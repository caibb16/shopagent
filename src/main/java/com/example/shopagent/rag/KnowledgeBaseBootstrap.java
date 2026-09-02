package com.example.shopagent.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads the FAQ knowledge base from the classpath resource
 * {@code knowledge/faq.jsonl} on application startup and indexes every entry
 * into both the {@link VectorIndex} and {@link InMemoryBm25Index}.
 *
 * <p>Defensively scoped to the {@code dev} profile because the dependencies it
 * autowires (vector + BM25) are dev-only implementations. Task 19 will introduce
 * a production vector index (Qdrant) and replace the BM25 concrete wiring here.
 *
 * <p>Doubles as the {@code @Configuration} that declares the BM25 index and the
 * {@link HybridRetriever} as beans. Both are deliberately plain-Java classes
 * (no stereotype annotations) so tests can construct them directly, so they
 * have to be registered here via {@code @Bean} factory methods.
 */
@Slf4j
@Configuration
@Profile("!prod")
@RequiredArgsConstructor
public class KnowledgeBaseBootstrap {
    private final VectorIndex vectorIndex;
    // Concrete type on purpose — bootstrap needs to populate the keyword index too.
    // Production bootstrap (Task 19) will swap this for whatever full-text engine
    // replaces the in-memory BM25 impl at that point.
    private final InMemoryBm25Index bm25Index;
    private final ObjectMapper mapper = new ObjectMapper();

    /** BM25 keyword index (dev profile). Plain-Java class, so registered here. */
    @Bean
    static InMemoryBm25Index inMemoryBm25Index() {
        return new InMemoryBm25Index();
    }

    /** Hybrid RRF retriever over the vector + BM25 legs. */
    @Bean
    static HybridRetriever hybridRetriever(VectorIndex vectorIndex, InMemoryBm25Index bm25Index) {
        return new HybridRetriever(vectorIndex, bm25Index);
    }

    @PostConstruct
    public void load() throws Exception {
        log.info("Loading knowledge base from faq.jsonl...");
        int count = 0;
        // Note: explicit UTF-8 (Windows default charset is not UTF-8) and a
        // line-based reader (so CRLF normalization by git does not matter).
        try (var in = new ClassPathResource("knowledge/faq.jsonl").getInputStream();
             var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                var node = mapper.readTree(line);
                String id = node.get("id").asText();
                String text = node.get("question").asText() + " " + node.get("answer").asText();
                Map<String, Object> meta = Map.of("category", node.get("category").asText());
                vectorIndex.upsert(id, text, meta);
                bm25Index.add(id, text, meta);
                count++;
            }
        }
        log.info("Knowledge base loaded: {} entries", count);
    }
}
