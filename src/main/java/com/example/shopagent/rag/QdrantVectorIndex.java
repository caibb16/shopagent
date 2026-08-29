package com.example.shopagent.rag;

import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prod-profile {@link VectorIndex} backed by Qdrant (gRPC, port 6334 by default).
 *
 * <p>NOTE: This class is loaded only under {@code spring.profiles.active=prod}.
 * The brief's {@code QdrantGrpcClient.Builder().host().port()} pattern does not
 * exist in {@code io.qdrant:client:1.8.0} — the actual builder is created via
 * the static factory {@code QdrantGrpcClient.newBuilder(host, port)} (see
 * {@code ProdStoreConfig#qdrantClient}). Likewise {@code QdrantClient#searchAsync}
 * requires a {@link Points.SearchPoints} message — there is no 3-arg overload.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class QdrantVectorIndex implements VectorIndex {

    private final QdrantClient qdrantClient;
    private final EmbeddingModel embeddingModel;

    @Value("${shopagent.vector.qdrant.collection}")
    private String collection;

    @Override
    public void upsert(String id, String text, Map<String, Object> metadata) {
        float[] vec = embeddingModel.embed(text);
        Map<String, JsonWithInt.Value> payload = new HashMap<>();
        if (metadata != null) {
            metadata.forEach((k, v) -> payload.put(k, strValue(v == null ? "" : v.toString())));
        }
        payload.put("text", strValue(text));

        Points.PointStruct point = Points.PointStruct.newBuilder()
                .setId(PointIdFactory.id((long) (id.hashCode() & 0x7fffffff)))
                .setVectors(VectorsFactory.vectors(vec))
                .putAllPayload(payload)
                .build();
        try {
            qdrantClient.upsertAsync(collection, List.of(point)).get();
        } catch (Exception e) {
            throw new RuntimeException("Qdrant upsert failed", e);
        }
    }

    @Override
    public List<ScoredDoc> search(String query, int topK) {
        float[] vec = embeddingModel.embed(query);
        Points.SearchPoints req = Points.SearchPoints.newBuilder()
                .setCollectionName(collection)
                .addAllVector(toFloatList(vec))
                .setLimit(topK)
                .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                .build();
        try {
            List<Points.ScoredPoint> resp = qdrantClient.searchAsync(req).get();
            List<ScoredDoc> out = new ArrayList<>(resp.size());
            for (Points.ScoredPoint s : resp) {
                String id = s.getId().hasUuid()
                        ? s.getId().getUuid()
                        : String.valueOf(s.getId().getNum());
                String text = s.getPayloadOrDefault("text", strValue("")).getStringValue();
                out.add(new ScoredDoc(id, text, s.getScore(), Map.of()));
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Qdrant search failed", e);
        }
    }

    @Override
    public int size() {
        // Cheap, optional: count is not part of the contract and prod code should
        // rely on Qdrant admin tools. Return 0 to keep the impl side-effect free.
        return 0;
    }

    private static JsonWithInt.Value strValue(String s) {
        return JsonWithInt.Value.newBuilder().setStringValue(s).build();
    }

    private static List<Float> toFloatList(float[] v) {
        List<Float> out = new ArrayList<>(v.length);
        for (float f : v) out.add(f);
        return out;
    }
}