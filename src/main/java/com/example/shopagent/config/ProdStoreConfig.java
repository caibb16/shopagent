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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Prod-profile wiring for Qdrant, the {@link VectorIndex}, and a
 * {@link RedisTemplate} tuned for {@code String}/{@code String} payloads.
 *
 * <p>NOTE: The brief's {@code new QdrantGrpcClient.Builder().host().port().build()}
 * is not valid in {@code io.qdrant:client:1.8.0}. The builder must be obtained
 * from {@link QdrantGrpcClient#newBuilder(String, int)}. This {@code @Configuration}
 * class is only loaded under {@code spring.profiles.active=prod}.
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

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        StringRedisSerializer s = new StringRedisSerializer();
        t.setKeySerializer(s);
        t.setValueSerializer(s);
        t.setHashKeySerializer(s);
        t.setHashValueSerializer(s);
        t.afterPropertiesSet();
        return t;
    }
}