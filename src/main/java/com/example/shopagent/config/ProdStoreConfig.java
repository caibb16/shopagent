package com.example.shopagent.config;

import com.example.shopagent.rag.QdrantVectorIndex;
import com.example.shopagent.rag.VectorIndex;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Prod-profile wiring for Qdrant and the {@link VectorIndex}.
 *
 * <p>NOTE: {@code stringRedisTemplate} is provided by Redisson auto-config;
 * defining it here causes a bean-name conflict with
 * {@code RedissonAutoConfigurationV2}.
 */
@Configuration
@Profile("prod")
public class ProdStoreConfig {

    @Value("${shopagent.vector.qdrant.host}")
    private String qHost;

    @Value("${shopagent.vector.qdrant.port}")
    private int qPort;

    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient() {
        QdrantGrpcClient grpc = QdrantGrpcClient.newBuilder(qHost, qPort).build();
        return new QdrantClient(grpc);
    }

    @Bean
    public VectorIndex vectorIndex(QdrantClient client, EmbeddingModel embeddingModel) {
        return new QdrantVectorIndex(client, embeddingModel);
    }
}