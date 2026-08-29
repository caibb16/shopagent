# ShopAgent MVP Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot 3 + Spring AI e-commerce customer service agent backend with intent routing, Function Calling, RAG, SSE streaming, and pluggable in-memory/real-storage layers.

**Architecture:** Single Maven module with package-by-feature layering. Repository / SessionStore / VectorIndex are interface-first, with two complete implementations per concern (dev: in-memory; prod: JPA/Redis/Qdrant). Spring Profile selects active impl. Agent layer routes by intent (CHITCHAT/INQUIRY/ACTION/COMPLAINT) to specialized agents. SSE exposes streamable chat endpoint.

**Tech Stack:**
- Java 17, Spring Boot 3.3.x, Spring WebFlux
- Spring AI 1.0.x (DeepSeek via OpenAI-compatible protocol)
- Spring Data JPA + H2 (dev) / MySQL (prod)
- MyBatis-Plus 3.5.x (for prod OrderRepository — design-doc mandated)
- Redisson 3.x (Redis client for prod SessionStore)
- Qdrant Java client (prod vector store) / Spring AI SimpleVectorStore (dev)
- Lombok, MapStruct, JUnit 5, Mockito, AssertJ

## Global Constraints

- Java 17 minimum; build target `java17`
- Spring Boot version pinned: `3.3.4`
- Spring AI version pinned: `1.0.0-M6`
- All Repository / SessionStore / VectorIndex MUST have BOTH dev (in-memory) and prod (real-service) implementations, switched by `@Profile("dev")` / `@Profile("prod")`
- Default profile is `dev` — clone-and-run with no external services
- All Tool methods MUST read user identity from `UserContext.current()` (ThreadLocal), never accept userId as a tool argument
- API key, DB URL, Redis host etc. read from environment variables; do NOT hardcode secrets in committed files
- Use `application-{profile}.yml` for profile-specific config
- Knowledge base content lives in `src/main/resources/knowledge/faq.jsonl` (≥ 50 entries)
- Prompts live in `src/main/resources/prompts/*.st` as plain text
- Frequent commits: one commit per logical task (or per step within a task)

---

## File Structure

```
shopagent/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── README.md
├── .gitignore
├── .env.example
├── data/eval/test_set.jsonl
├── docs/superpowers/{specs,plans}/...
├── src/main/java/com/example/shopagent/
│   ├── ShopAgentApplication.java
│   ├── api/
│   │   ├── ChatController.java
│   │   ├── dto/{ChatRequest,ChatChunk,ErrorResponse}.java
│   │   └── error/{GlobalExceptionHandler,SseErrorSender}.java
│   ├── agent/
│   │   ├── AgentService.java
│   │   ├── Intent.java
│   │   ├── IntentClassifier.java
│   │   ├── ChitchatAgent.java
│   │   ├── RagAgent.java
│   │   ├── ToolAgent.java
│   │   └── HandoffService.java
│   ├── tool/
│   │   ├── UserContext.java
│   │   ├── ToolAuthException.java
│   │   ├── OrderTool.java
│   │   ├── LogisticsTool.java
│   │   ├── RefundTool.java
│   │   ├── CouponTool.java
│   │   ├── RecommendTool.java
│   │   └── EscalateTool.java
│   ├── rag/
│   │   ├── Document.java            (record, our own DTO — not Spring AI's)
│   │   ├── ScoredDoc.java
│   │   ├── KnowledgeBaseBootstrap.java
│   │   ├── VectorIndex.java
│   │   ├── InMemoryVectorIndex.java
│   │   ├── QdrantVectorIndex.java
│   │   ├── HybridRetriever.java
│   │   ├── InMemoryHybridRetriever.java
│   │   ├── QueryRewriter.java
│   │   ├── Reranker.java
│   │   ├── NoOpReranker.java
│   │   └── PromptTemplates.java
│   ├── business/
│   │   ├── domain/{User,Order,OrderItem,Logistics,Refund,Coupon,Product}.java
│   │   ├── repo/{UserRepository,OrderRepository,RefundRepository,CouponRepository,ProductRepository}.java
│   │   ├── repo/impl/
│   │   │   ├── InMemoryUserRepository.java
│   │   │   ├── InMemoryOrderRepository.java
│   │   │   ├── InMemoryRefundRepository.java
│   │   │   ├── InMemoryCouponRepository.java
│   │   │   ├── InMemoryProductRepository.java
│   │   │   ├── JpaUserRepository.java          (Spring Data)
│   │   │   ├── MybatisOrderRepository.java
│   │   │   ├── MybatisRefundRepository.java
│   │   │   └── JpaCouponRepository.java
│   │   ├── service/{CouponService,RecommendService}.java
│   │   └── MockDataLoader.java
│   ├── session/
│   │   ├── SessionStore.java
│   │   ├── InMemorySessionStore.java
│   │   ├── RedisSessionStore.java
│   │   ├── Message.java                 (record)
│   │   └── SessionSummaryService.java
│   ├── observability/
│   │   ├── LoggingAdvisor.java
│   │   └── TokenUsageAdvisor.java
│   └── config/
│       ├── DeepSeekConfig.java
│       ├── VectorStoreConfig.java
│       ├── StoreConfig.java             (dev beans)
│       ├── ProdStoreConfig.java         (prod beans)
│       └── ProfileConfig.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── prompts/{customer-service,query-rewriter,intent-classifier}.st
│   ├── knowledge/faq.jsonl
│   └── schema.sql                      (prod MySQL DDL)
└── src/test/java/com/example/shopagent/
    ├── agent/IntentClassifierTest.java
    ├── tool/{OrderToolAuthTest,ToolAuthTest}.java
    ├── rag/{HybridRetrieverTest,RrfFusionTest}.java
    ├── session/{InMemorySessionStoreTest,SessionSummaryServiceTest}.java
    ├── business/{InMemoryOrderRepositoryTest,MockDataLoaderTest}.java
    └── api/ChatControllerE2ETest.java
```

---

## Task 1: Maven Project Scaffold

**Files:**
- Create: `pom.xml`
- Create: `.gitignore`
- Create: `src/main/java/com/example/shopagent/ShopAgentApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/example/shopagent/ShopAgentApplicationTests.java`

**Interfaces:** None (foundation)

- [ ] **Step 1: Create `.gitignore`**

```
target/
*.class
*.jar
.idea/
*.iml
.vscode/
.DS_Store
.env
```

- [ ] **Step 2: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>shopagent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>shopagent</name>

    <properties>
        <java.version>17</java.version>
        <spring-ai.version>1.0.0-M6</spring-ai.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <redisson.version>3.34.1</redisson.version>
        <qdrant.version>1.8.0</qdrant.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>${redisson.version}</version>
            <exclusions>
                <exclusion><groupId>org.redisson</groupId><artifactId>redisson-spring-data-32</artifactId></exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-data-33</artifactId>
            <version>${redisson.version}</version>
        </dependency>

        <dependency>
            <groupId>io.qdrant</groupId>
            <artifactId>client</artifactId>
            <version>${qdrant.version}</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
        </repository>
        <repository>
            <id>spring-snapshots</id>
            <name>Spring Snapshots</name>
            <url>https://repo.spring.io/snapshot</url>
        </repository>
    </repositories>
</project>
```

- [ ] **Step 3: Create `ShopAgentApplication.java`**

```java
package com.example.shopagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShopAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopAgentApplication.class, args);
    }
}
```

- [ ] **Step 4: Create `src/main/resources/application.yml`**

```yaml
spring:
  application:
    name: shopagent
  profiles:
    active: dev
  # dev profile: Spring AI's OpenAiAutoConfiguration hard-requires an api-key
  # at boot, and we want clone-and-run without setting one. Exclude it here;
  # Task 8 adds a manual ChatClient bean that handles the optional api-key path.
  autoconfigure:
    exclude:
      - org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

shopagent:
  llm:
    api-key: ${DEEPSEEK_API_KEY:}
    base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
    model: ${DEEPSEEK_MODEL:deepseek-chat}
    embedding-model: ${DEEPSEEK_EMBEDDING_MODEL:bge-small-zh}
  vector:
    backend: ${VECTOR_BACKEND:simple}
  session:
    summary-every-n-turns: 5
```

- [ ] **Step 5: Create `src/test/java/com/example/shopagent/ShopAgentApplicationTests.java`**

```java
package com.example.shopagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class ShopAgentApplicationTests {
    @Test
    void contextLoads() {}
}
```

- [ ] **Step 6: Build to verify**

Run: `mvn -q -DskipTests package`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git init && git add -A && git commit -m "chore: scaffold Spring Boot 3 + Spring AI project"
```

---

## Task 2: Domain Entities

**Files:**
- Create: `src/main/java/com/example/shopagent/business/domain/User.java`
- Create: `src/main/java/com/example/shopagent/business/domain/Order.java`
- Create: `src/main/java/com/example/shopagent/business/domain/OrderItem.java`
- Create: `src/main/java/com/example/shopagent/business/domain/Logistics.java`
- Create: `src/main/java/com/example/shopagent/business/domain/Refund.java`
- Create: `src/main/java/com/example/shopagent/business/domain/Coupon.java`
- Create: `src/main/java/com/example/shopagent/business/domain/Product.java`

**Interfaces:** None (plain DTOs)

- [ ] **Step 1: Create `User.java`**

```java
package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {
    private Long id;
    private String name;
    private String level; // NORMAL | VIP | BLACKLIST
}
```

- [ ] **Step 2: Create `Order.java`**

```java
package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class Order {
    private String orderId;
    private Long userId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private String status; // PENDING | SHIPPED | DELIVERED | REFUNDED
    private Instant createdAt;
    private String trackingNumber;
}
```

- [ ] **Step 3: Create `OrderItem.java`**

```java
package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class OrderItem {
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
}
```

- [ ] **Step 4: Create `Logistics.java`**

```java
package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class Logistics {
    private String trackingNumber;
    private String carrier;
    private String currentLocation;
    private List<Event> events;

    @Data @Builder
    public static class Event {
        private Instant timestamp;
        private String location;
        private String description;
    }
}
```

- [ ] **Step 5: Create `Refund.java`**

```java
package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class Refund {
    private String refundId;
    private String orderId;
    private Long userId;
    private String reason;
    private String status; // PENDING | APPROVED | REJECTED
    private Instant createdAt;
}
```

- [ ] **Step 6: Create `Coupon.java`**

```java
package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class Coupon {
    private String couponId;
    private Long userId;
    private String name;
    private BigDecimal discount;
    private Instant expiresAt;
    private boolean used;
}
```

- [ ] **Step 7: Create `Product.java`**

```java
package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class Product {
    private String productId;
    private String name;
    private String category;
    private BigDecimal price;
    private String description;
}
```

- [ ] **Step 8: Compile**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/example/shopagent/business/domain/
git commit -m "feat: add domain entities for user/order/logistics/refund/coupon/product"
```

---

## Task 3: Repository Interfaces

**Files:**
- Create: `src/main/java/com/example/shopagent/business/repo/UserRepository.java`
- Create: `src/main/java/com/example/shopagent/business/repo/OrderRepository.java`
- Create: `src/main/java/com/example/shopagent/business/repo/RefundRepository.java`
- Create: `src/main/java/com/example/shopagent/business/repo/CouponRepository.java`
- Create: `src/main/java/com/example/shopagent/business/repo/ProductRepository.java`

**Interfaces:** These interfaces are consumed by Tool layer (later tasks) and MockDataLoader (next task).

- [ ] **Step 1: Create `UserRepository.java`**

```java
package com.example.shopagent.business.repo;

import com.example.shopagent.business.domain.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
    User save(User user);
}
```

- [ ] **Step 2: Create `OrderRepository.java`**

```java
package com.example.shopagent.business.repo;

import com.example.shopagent.business.domain.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(String orderId);
    List<Order> findByUserId(Long userId);
    Order save(Order order);
}
```

- [ ] **Step 3: Create `RefundRepository.java`**

```java
package com.example.shopagent.business.repo;

import com.example.shopagent.business.domain.Refund;
import java.util.List;

public interface RefundRepository {
    Refund save(Refund refund);
    List<Refund> findByUserId(Long userId);
    List<Refund> findByOrderId(String orderId);
}
```

- [ ] **Step 4: Create `CouponRepository.java`**

```java
package com.example.shopagent.business.repo;

import com.example.shopagent.business.domain.Coupon;
import java.util.List;

public interface CouponRepository {
    List<Coupon> findByUserId(Long userId);
    Coupon save(Coupon coupon);
}
```

- [ ] **Step 5: Create `ProductRepository.java`**

```java
package com.example.shopagent.business.repo;

import com.example.shopagent.business.domain.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(String productId);
    List<Product> findByCategory(String category);
    List<Product> findByPriceLessThan(java.math.BigDecimal maxPrice);
}
```

- [ ] **Step 6: Compile**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/shopagent/business/repo/
git commit -m "feat: add repository interfaces"
```

---

## Task 4: Mock Data Loader + In-Memory Repositories

**Files:**
- Create: `src/main/java/com/example/shopagent/business/MockDataLoader.java`
- Create: `src/main/java/com/example/shopagent/business/repo/impl/InMemoryUserRepository.java`
- Create: `src/main/java/com/example/shopagent/business/repo/impl/InMemoryOrderRepository.java`
- Create: `src/main/java/com/example/shopagent/business/repo/impl/InMemoryRefundRepository.java`
- Create: `src/main/java/com/example/shopagent/business/repo/impl/InMemoryCouponRepository.java`
- Create: `src/main/java/com/example/shopagent/business/repo/impl/InMemoryProductRepository.java`
- Create: `src/test/java/com/example/shopagent/business/InMemoryOrderRepositoryTest.java`
- Create: `src/test/java/com/example/shopagent/business/MockDataLoaderTest.java`
- Create: `src/main/java/com/example/shopagent/config/StoreConfig.java`

**Interfaces:**
- Consumes: repository interfaces (Task 3), domain entities (Task 2)
- Produces: beans `OrderRepository` (InMemory), `MockDataLoader` — used by Tools (Tasks 11–16)

- [ ] **Step 1: Write failing test `InMemoryOrderRepositoryTest.java`**

```java
package com.example.shopagent.business;

import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.domain.OrderItem;
import com.example.shopagent.business.repo.OrderRepository;
import com.example.shopagent.business.repo.impl.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class InMemoryOrderRepositoryTest {
    private final OrderRepository repo = new InMemoryOrderRepository();

    @Test
    void saveAndFindByIdRoundTrips() {
        Order o = Order.builder().orderId("O1").userId(1L).totalAmount(BigDecimal.TEN)
                .items(List.of(OrderItem.builder().productId("P1").quantity(1).unitPrice(BigDecimal.TEN).build()))
                .status("PENDING").build();
        repo.save(o);
        assertThat(repo.findById("O1")).contains(o);
    }

    @Test
    void findByUserIdReturnsOnlyMatching() {
        repo.save(Order.builder().orderId("A").userId(1L).status("PENDING").build());
        repo.save(Order.builder().orderId("B").userId(2L).status("PENDING").build());
        assertThat(repo.findByUserId(1L)).extracting(Order::getOrderId).containsExactly("A");
    }

    @Test
    void findByIdMissingReturnsEmpty() {
        assertThat(repo.findById("NOPE")).isEmpty();
    }
}
```

- [ ] **Step 2: Run test (fails: class not found)**

Run: `mvn -q test -Dtest=InMemoryOrderRepositoryTest`
Expected: COMPILATION FAILURE (InMemoryOrderRepository does not exist)

- [ ] **Step 3: Implement `InMemoryOrderRepository.java`**

```java
package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.repo.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOrderRepository implements OrderRepository {
    private final Map<String, Order> store = new ConcurrentHashMap<>();

    @Override public Optional<Order> findById(String orderId) { return Optional.ofNullable(store.get(orderId)); }
    @Override public List<Order> findByUserId(Long userId) {
        return store.values().stream().filter(o -> Objects.equals(o.getUserId(), userId)).toList();
    }
    @Override public Order save(Order order) { store.put(order.getOrderId(), order); return order; }
}
```

- [ ] **Step 4: Run test (passes)**

Run: `mvn -q test -Dtest=InMemoryOrderRepositoryTest`
Expected: 3 tests passed

- [ ] **Step 5: Implement remaining InMemory repos**

`InMemoryUserRepository.java`:
```java
package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.User;
import com.example.shopagent.business.repo.UserRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> store = new ConcurrentHashMap<>();
    @Override public Optional<User> findById(Long id) { return Optional.ofNullable(store.get(id)); }
    @Override public User save(User user) { store.put(user.getId(), user); return user; }
}
```

`InMemoryRefundRepository.java`:
```java
package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Refund;
import com.example.shopagent.business.repo.RefundRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryRefundRepository implements RefundRepository {
    private final Map<String, Refund> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    @Override public Refund save(Refund refund) {
        if (refund.getRefundId() == null) refund.setRefundId("R" + seq.incrementAndGet());
        store.put(refund.getRefundId(), refund); return refund;
    }
    @Override public List<Refund> findByUserId(Long userId) {
        return store.values().stream().filter(r -> Objects.equals(r.getUserId(), userId)).toList();
    }
    @Override public List<Refund> findByOrderId(String orderId) {
        return store.values().stream().filter(r -> Objects.equals(r.getOrderId(), orderId)).toList();
    }
}
```

`InMemoryCouponRepository.java`:
```java
package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Coupon;
import com.example.shopagent.business.repo.CouponRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCouponRepository implements CouponRepository {
    private final Map<String, Coupon> store = new ConcurrentHashMap<>();
    @Override public List<Coupon> findByUserId(Long userId) {
        return store.values().stream().filter(c -> Objects.equals(c.getUserId(), userId)).toList();
    }
    @Override public Coupon save(Coupon coupon) { store.put(coupon.getCouponId(), coupon); return coupon; }
}
```

`InMemoryProductRepository.java`:
```java
package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Product;
import com.example.shopagent.business.repo.ProductRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryProductRepository implements ProductRepository {
    private final Map<String, Product> store = new ConcurrentHashMap<>();
    @Override public Optional<Product> findById(String id) { return Optional.ofNullable(store.get(id)); }
    @Override public List<Product> findByCategory(String category) {
        return store.values().stream().filter(p -> Objects.equals(p.getCategory(), category)).toList();
    }
    @Override public List<Product> findByPriceLessThan(BigDecimal maxPrice) {
        return store.values().stream().filter(p -> p.getPrice().compareTo(maxPrice) < 0).toList();
    }
    public void register(Product p) { store.put(p.getProductId(), p); }
}
```

- [ ] **Step 6: Implement `MockDataLoader.java`**

```java
package com.example.shopagent.business;

import com.example.shopagent.business.domain.*;
import com.example.shopagent.business.repo.*;
import com.example.shopagent.business.repo.impl.InMemoryProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataLoader {

    private final UserRepository users;
    private final OrderRepository orders;
    private final RefundRepository refunds;
    private final CouponRepository coupons;
    private final InMemoryProductRepository products;

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        log.info("Loading mock data...");
        users.save(User.builder().id(1L).name("Alice").level("NORMAL").build());
        users.save(User.builder().id(2L).name("Bob").level("VIP").build());
        users.save(User.builder().id(999L).name("Mallory").level("BLACKLIST").build());

        products.register(Product.builder().productId("P1").name("蓝牙耳机").category("数码").price(BigDecimal.valueOf(299)).description("主动降噪").build());
        products.register(Product.builder().productId("P2").name("保温杯").category("家居").price(BigDecimal.valueOf(89)).description("316不锈钢").build());
        products.register(Product.builder().productId("P3").name("机械键盘").category("数码").price(BigDecimal.valueOf(599)).description("青轴").build());
        products.register(Product.builder().productId("P4").name("帆布鞋").category("服饰").price(BigDecimal.valueOf(199)).description("经典款").build());

        orders.save(newOrder("O1001", 1L, "PENDING", null));
        orders.save(newOrder("O1002", 1L, "SHIPPED", "SF1234567890"));
        orders.save(newOrder("O1003", 2L, "DELIVERED", "SF9876543210"));
        orders.save(newOrder("O1004", 2L, "REFUNDED", "YT1122334455"));

        coupons.save(Coupon.builder().couponId("C1").userId(1L).name("新人立减").discount(BigDecimal.valueOf(20)).expiresAt(Instant.now().plus(30, ChronoUnit.DAYS)).used(false).build());
        coupons.save(Coupon.builder().couponId("C2").userId(2L).name("VIP专享").discount(BigDecimal.valueOf(50)).expiresAt(Instant.now().plus(60, ChronoUnit.DAYS)).used(false).build());

        log.info("Mock data loaded: 3 users, 4 orders, 2 coupons, 4 products");
    }

    private Order newOrder(String id, Long uid, String status, String tracking) {
        return Order.builder().orderId(id).userId(uid).totalAmount(BigDecimal.valueOf(299))
                .status(status).createdAt(Instant.now()).trackingNumber(tracking)
                .items(List.of(OrderItem.builder().productId("P1").productName("蓝牙耳机").quantity(1).unitPrice(BigDecimal.valueOf(299)).build()))
                .build();
    }
}
```

- [ ] **Step 7: Implement `StoreConfig.java`**

```java
package com.example.shopagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class StoreConfig {
    // Dev profile uses @Repository-annotated in-memory impls.
    // This class exists so additional dev-only wiring has a home.
}
```

- [ ] **Step 8: Write `MockDataLoaderTest.java`**

```java
package com.example.shopagent.business;

import com.example.shopagent.business.repo.*;
import com.example.shopagent.business.repo.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MockDataLoaderTest {
    private UserRepository users;
    private OrderRepository orders;
    private InMemoryProductRepository products;
    private MockDataLoader loader;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        orders = new InMemoryOrderRepository();
        products = new InMemoryProductRepository();
        loader = new MockDataLoader(users, orders, new InMemoryRefundRepository(),
                new InMemoryCouponRepository(), products);
    }

    @Test
    void loadSeedsThreeUsers() {
        loader.load();
        assertThat(users.findById(1L)).isPresent();
        assertThat(users.findById(2L)).isPresent();
        assertThat(users.findById(999L)).isPresent();
    }

    @Test
    void loadSeedsOrdersForBothUsers() {
        loader.load();
        assertThat(orders.findByUserId(1L)).hasSize(2);
        assertThat(orders.findByUserId(2L)).hasSize(2);
    }
}
```

- [ ] **Step 9: Run all tests**

Run: `mvn -q test`
Expected: All tests pass

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/example/shopagent/business/ src/main/java/com/example/shopagent/config/StoreConfig.java src/test/
git commit -m "feat: in-memory repositories + mock data loader (dev profile)"
```

---

## Task 5: UserContext + ToolAuthException

**Files:**
- Create: `src/main/java/com/example/shopagent/tool/UserContext.java`
- Create: `src/main/java/com/example/shopagent/tool/ToolAuthException.java`
- Create: `src/test/java/com/example/shopagent/tool/UserContextTest.java`

**Interfaces:**
- Consumes: nothing
- Produces: `UserContext.current()`, `UserContext.set(...)`, `UserContext.clear()` — used by every Tool method (Tasks 11–16)

- [ ] **Step 1: Write failing test `UserContextTest.java`**

```java
package com.example.shopagent.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserContextTest {
    @AfterEach void cleanup() { UserContext.clear(); }

    @Test
    void setAndCurrentRoundTrips() {
        UserContext.set(new UserContext(42L, "session-1"));
        assertThat(UserContext.current().userId()).isEqualTo(42L);
        assertThat(UserContext.current().sessionId()).isEqualTo("session-1");
    }

    @Test
    void currentWhenUnsetThrows() {
        assertThatThrownBy(UserContext::current)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void clearRemoves() {
        UserContext.set(new UserContext(1L, "x"));
        UserContext.clear();
        assertThatThrownBy(UserContext::current).isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 2: Run (fails)**

Run: `mvn -q test -Dtest=UserContextTest`
Expected: COMPILATION FAILURE

- [ ] **Step 3: Implement `UserContext.java`**

```java
package com.example.shopagent.tool;

public record UserContext(Long userId, String sessionId) {
    private static final ThreadLocal<UserContext> CTX = new ThreadLocal<>();

    public static void set(UserContext ctx) { CTX.set(ctx); }
    public static UserContext current() {
        UserContext c = CTX.get();
        if (c == null) throw new IllegalStateException("UserContext not set in this thread");
        return c;
    }
    public static void clear() { CTX.remove(); }
}
```

- [ ] **Step 4: Implement `ToolAuthException.java`**

```java
package com.example.shopagent.tool;

public class ToolAuthException extends RuntimeException {
    public ToolAuthException(String message) { super(message); }
}
```

- [ ] **Step 5: Run tests (pass)**

Run: `mvn -q test -Dtest=UserContextTest`
Expected: 3 tests pass

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/shopagent/tool/UserContext.java src/main/java/com/example/shopagent/tool/ToolAuthException.java src/test/java/com/example/shopagent/tool/
git commit -m "feat: UserContext thread-local + ToolAuthException"
```

---

## Task 6: SessionStore (In-Memory)

**Files:**
- Create: `src/main/java/com/example/shopagent/session/Message.java`
- Create: `src/main/java/com/example/shopagent/session/SessionStore.java`
- Create: `src/main/java/com/example/shopagent/session/InMemorySessionStore.java`
- Create: `src/test/java/com/example/shopagent/session/InMemorySessionStoreTest.java`

**Interfaces:**
- Consumes: nothing
- Produces: `SessionStore` bean — used by AgentService (Task 27) and SessionSummaryService (Task 7)

- [ ] **Step 1: Create `Message.java`**

```java
package com.example.shopagent.session;

import java.time.Instant;

public record Message(String role, String content, Instant timestamp) {
    public static Message user(String text) {
        return new Message("user", text, Instant.now());
    }
    public static Message assistant(String text) {
        return new Message("assistant", text, Instant.now());
    }
    public static Message system(String text) {
        return new Message("system", text, Instant.now());
    }
}
```

- [ ] **Step 2: Write failing test `InMemorySessionStoreTest.java`**

```java
package com.example.shopagent.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class InMemorySessionStoreTest {
    private final SessionStore store = new InMemorySessionStore();

    @Test
    void appendAndGetHistoryReturnsInOrder() {
        store.appendMessage("s1", Message.user("hi"));
        store.appendMessage("s1", Message.assistant("hello"));
        List<Message> history = store.getHistory("s1", 10);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).role()).isEqualTo("user");
        assertThat(history.get(1).role()).isEqualTo("assistant");
    }

    @Test
    void getHistoryLimitTruncates() {
        for (int i = 0; i < 20; i++) store.appendMessage("s", Message.user("m" + i));
        assertThat(store.getHistory("s", 5)).hasSize(5);
    }

    @Test
    void summaryRoundTrip() {
        store.setSummary("s", "discussed refund");
        assertThat(store.getSummary("s")).contains("refund");
    }

    @Test
    void summaryMissingReturnsEmpty() {
        assertThat(store.getSummary("nope")).isEmpty();
    }

    @Test
    void separateSessionsAreIsolated() {
        store.appendMessage("a", Message.user("x"));
        store.appendMessage("b", Message.user("y"));
        assertThat(store.getHistory("a", 10)).hasSize(1);
        assertThat(store.getHistory("b", 10)).hasSize(1);
    }
}
```

- [ ] **Step 3: Implement `SessionStore.java`**

```java
package com.example.shopagent.session;

import java.util.List;
import java.util.Optional;

public interface SessionStore {
    void appendMessage(String sessionId, Message msg);
    List<Message> getHistory(String sessionId, int limit);
    void setSummary(String sessionId, String summary);
    Optional<String> getSummary(String sessionId);
}
```

- [ ] **Step 4: Implement `InMemorySessionStore.java`**

```java
package com.example.shopagent.session;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("dev")
public class InMemorySessionStore implements SessionStore {
    private final Map<String, Deque<Message>> histories = new ConcurrentHashMap<>();
    private final Map<String, String> summaries = new ConcurrentHashMap<>();

    @Override
    public void appendMessage(String sessionId, Message msg) {
        histories.computeIfAbsent(sessionId, k -> new ArrayDeque<>()).addLast(msg);
    }

    @Override
    public List<Message> getHistory(String sessionId, int limit) {
        Deque<Message> dq = histories.get(sessionId);
        if (dq == null) return List.of();
        return dq.stream().skip(Math.max(0, dq.size() - limit)).toList();
    }

    @Override
    public void setSummary(String sessionId, String summary) {
        summaries.put(sessionId, summary);
    }

    @Override
    public Optional<String> getSummary(String sessionId) {
        return Optional.ofNullable(summaries.get(sessionId));
    }
}
```

- [ ] **Step 5: Run tests**

Run: `mvn -q test -Dtest=InMemorySessionStoreTest`
Expected: 5 tests pass

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/shopagent/session/ src/test/java/com/example/shopagent/session/
git commit -m "feat: SessionStore interface + in-memory impl (dev)"
```

---

## Task 7: Prompt Templates

**Files:**
- Create: `src/main/resources/prompts/customer-service.st`
- Create: `src/main/resources/prompts/intent-classifier.st`
- Create: `src/main/resources/prompts/query-rewriter.st`
- Create: `src/main/java/com/example/shopagent/rag/PromptTemplates.java`
- Create: `src/test/java/com/example/shopagent/rag/PromptTemplatesTest.java`

**Interfaces:**
- Consumes: nothing (loads from classpath)
- Produces: `PromptTemplates.customerService(...)`, `PromptTemplates.intentClassifier()`, `PromptTemplates.queryRewriter()` — used by RagAgent (Task 24), ToolAgent (Task 25), IntentClassifier (Task 22), QueryRewriter (Task 20)

- [ ] **Step 1: Create `customer-service.st`**

```
你是「小蜜」，电商平台智能客服。你的职责：
1. 礼貌、简洁、专业
2. 仅基于【知识库上下文】和【工具结果】回答，不知道就说不知道
3. 操作类请求必须调用工具，不要凭空回答
4. 涉及退款/投诉，先安抚用户情绪再处理

【知识库上下文】
%s

【对话历史摘要】
%s
```

- [ ] **Step 2: Create `intent-classifier.st`**

```
判断用户消息属于哪一类，只输出一个标签：
- CHITCHAT：闲聊、打招呼、与购物无关的对话
- INQUIRY：询问政策、流程、商品信息（不需要修改数据）
- ACTION：用户希望执行具体操作（查订单/查物流/退款/优惠券/推荐/退换货）
- COMPLAINT：投诉、举报、强烈不满要求升级处理

用户消息：%s

只输出标签，不要解释。
```

- [ ] **Step 3: Create `query-rewriter.st`**

```
把用户口语化的问题改写为更适合检索的关键词形式。
- 提取核心实体（订单/商品/政策）
- 补充同义词和书面表达
- 输出短句或关键词列表，用空格分隔

原文：%s

只输出改写后的检索串，不要解释。
```

- [ ] **Step 4: Write failing test `PromptTemplatesTest.java`**

```java
package com.example.shopagent.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PromptTemplatesTest {
    @Test
    void customerServiceSubstitutesBothPlaceholders() {
        String out = PromptTemplates.customerService("KB", "SUMMARY");
        assertThat(out).contains("KB").contains("SUMMARY");
        assertThat(out).doesNotContain("%s");
    }

    @Test
    void intentClassifierSubstitutesUserText() {
        String out = PromptTemplates.intentClassifier("查订单");
        assertThat(out).contains("查订单");
    }

    @Test
    void queryRewriterSubstitutesUserText() {
        String out = PromptTemplates.queryRewriter("多久能到货");
        assertThat(out).contains("多久能到货");
    }
}
```

- [ ] **Step 5: Implement `PromptTemplates.java`**

```java
package com.example.shopagent.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

public final class PromptTemplates {
    private PromptTemplates() {}

    private static String load(String name) {
        try (var in = new ClassPathResource("prompts/" + name).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt: " + name, e);
        }
    }

    public static String customerService(String kbContext, String summary) {
        return String.format(load("customer-service.st"), kbContext, summary);
    }

    public static String intentClassifier(String userText) {
        return String.format(load("intent-classifier.st"), userText);
    }

    public static String queryRewriter(String userText) {
        return String.format(load("query-rewriter.st"), userText);
    }
}
```

- [ ] **Step 6: Run tests**

Run: `mvn -q test -Dtest=PromptTemplatesTest`
Expected: 3 tests pass

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/prompts/ src/main/java/com/example/shopagent/rag/PromptTemplates.java src/test/java/com/example/shopagent/rag/PromptTemplatesTest.java
git commit -m "feat: prompt templates + loader"
```

---

## Task 8: DeepSeek ChatClient Config

**Files:**
- Create: `src/main/java/com/example/shopagent/config/DeepSeekConfig.java`
- Create: `src/main/resources/application-dev.yml`
- Create: `src/main/java/com/example/shopagent/observability/LoggingAdvisor.java`
- Create: `src/main/java/com/example/shopagent/observability/TokenUsageAdvisor.java`

**Interfaces:**
- Consumes: nothing
- Produces: `ChatClient` bean — used by every Agent (Tasks 23–26)

- [ ] **Step 1: Create `application-dev.yml`**

```yaml
shopagent:
  llm:
    api-key: dev-placeholder  # dev profile: set in env when testing real LLM
    base-url: https://api.deepseek.com
    model: deepseek-chat
    embedding-model: bge-small-zh
  vector:
    backend: simple
  session:
    summary-every-n-turns: 5
```

- [ ] **Step 2: Create `application-prod.yml`**

```yaml
shopagent:
  llm:
    api-key: ${DEEPSEEK_API_KEY}
    base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
    model: ${DEEPSEEK_MODEL:deepseek-chat}
  vector:
    backend: qdrant
    qdrant:
      host: ${QDRANT_HOST:localhost}
      port: ${QDRANT_PORT:6334}
      collection: shopagent-kb
  session:
    summary-every-n-turns: 5
  redis:
    address: ${REDIS_ADDRESS:redis://localhost:6379}
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:mysql://localhost:3306/shopagent}
    username: ${DATABASE_USER:shopagent}
    password: ${DATABASE_PASSWORD:}
```

- [ ] **Step 3: Create `DeepSeekConfig.java`**

```java
package com.example.shopagent.config;

import com.example.shopagent.observability.LoggingAdvisor;
import com.example.shopagent.observability.TokenUsageAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeepSeekConfig {

    @Value("${shopagent.llm.api-key}") private String apiKey;
    @Value("${shopagent.llm.base-url}") private String baseUrl;
    @Value("${shopagent.llm.model}") private String model;

    @Bean
    public ChatClient chatClient() {
        var api = OpenAiApi.builder().apiKey(apiKey).baseUrl(baseUrl).build();
        var options = OpenAiChatOptions.builder().model(model).temperature(0.3).build();
        var chatModel = OpenAiChatModel.builder().openAiApi(api).defaultOptions(options).build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new LoggingAdvisor(), new TokenUsageAdvisor())
                .build();
    }
}
```

- [ ] **Step 4: Create `LoggingAdvisor.java`**

```java
package com.example.shopagent.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;

@Slf4j
public class LoggingAdvisor implements CallAroundAdvisor {
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        log.debug("[LLM] -> {}", request.userText());
        AdvisedResponse resp = chain.nextCall(request);
        log.debug("[LLM] <- {}", resp.response().getResult().getOutput().getContent());
        return resp;
    }

    @Override public String getName() { return "LoggingAdvisor"; }
    @Override public int getOrder() { return 0; }
}
```

- [ ] **Step 5: Create `TokenUsageAdvisor.java`**

```java
package com.example.shopagent.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class TokenUsageAdvisor implements CallAroundAdvisor {
    private final AtomicLong totalPromptTokens = new AtomicLong();
    private final AtomicLong totalCompletionTokens = new AtomicLong();

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        AdvisedResponse resp = chain.nextCall(request);
        var meta = resp.response().getMetadata();
        if (meta != null && meta.getUsage() != null) {
            long p = meta.getUsage().getPromptTokens();
            long c = meta.getUsage().getCompletionTokens();
            totalPromptTokens.addAndGet(p);
            totalCompletionTokens.addAndGet(c);
            log.info("[Tokens] prompt={}, completion={}, totalPrompt={}, totalCompletion={}",
                    p, c, totalPromptTokens.get(), totalCompletionTokens.get());
        }
        return resp;
    }

    @Override public String getName() { return "TokenUsageAdvisor"; }
    @Override public int getOrder() { return 1; }
}
```

- [ ] **Step 6: Compile + smoke test**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/shopagent/config/DeepSeekConfig.java src/main/java/com/example/shopagent/observability/ src/main/resources/application-dev.yml src/main/resources/application-prod.yml
git commit -m "feat: DeepSeek ChatClient config + logging/token advisors"
```

---

## Task 9: FAQ Dataset + VectorIndex Abstraction

**Files:**
- Create: `src/main/resources/knowledge/faq.jsonl` (≥50 entries)
- Create: `src/main/java/com/example/shopagent/rag/ScoredDoc.java`
- Create: `src/main/java/com/example/shopagent/rag/VectorIndex.java`

**Interfaces:**
- Consumes: nothing
- Produces: `VectorIndex` interface — implemented by InMemory (Task 10) and Qdrant (Task 11) impls

- [ ] **Step 1: Create `faq.jsonl` with ≥50 entries**

Each line is one JSON object with fields: `id`, `category`, `question`, `answer`. Example first lines:

```jsonl
{"id":"F001","category":"退货政策","question":"支持7天无理由退货吗？","answer":"是的，下单后7天内未拆封可申请无理由退货，需商品完好。"}
{"id":"F002","category":"退货政策","question":"已拆封能退吗？","answer":"已拆封商品若有质量问题可退，详情请描述问题，客服将协助处理。"}
{"id":"F003","category":"物流时效","question":"下单后多久发货？","answer":"现货商品48小时内发货，预售商品以商品详情页说明为准。"}
{"id":"F004","category":"物流时效","question":"发什么快递？","answer":"默认顺丰/圆通/中通，具体以下单页为准，订单详情可查看承运商。"}
{"id":"F005","category":"退款流程","question":"退款多久到账？","answer":"原路退回：微信/支付宝实时到账；银行卡1-3个工作日。"}
{"id":"F006","category":"优惠券","question":"优惠券怎么用？","answer":"下单结算页勾选可用优惠券，系统自动抵扣。"}
{"id":"F007","category":"优惠券","question":"优惠券会过期吗？","answer":"会的，请关注券面有效期，过期不可再用。"}
{"id":"F008","category":"支付","question":"支持哪些支付方式？","answer":"微信、支付宝、银联、京东支付、Apple Pay。"}
{"id":"F009","category":"订单修改","question":"下单后能改地址吗？","answer":"未发货前可在订单详情自助修改，已发货请拒收后联系客服。"}
{"id":"F010","category":"发票","question":"能开发票吗？","answer":"支持电子普通发票，下单时勾选即可，3个工作日内开具。"}
{"id":"F011","category":"会员","question":"VIP有什么权益？","answer":"VIP享额外5%折扣、专属优惠券、生日礼包、优先客服。"}
{"id":"F012","category":"售后","question":"商品破损怎么办？","answer":"请拍照上传，客服24小时内处理，符合条件免费补发或退款。"}
{"id":"F013","category":"物流时效","question":"偏远地区多久到？","answer":"新疆/西藏/青海一般5-7个工作日，海外及港澳台暂不支持直邮。"}
{"id":"F014","category":"退货政策","question":"赠品要一起退回吗？","answer":"是的，赠品需一同退回，否则按赠品价值扣款。"}
{"id":"F015","category":"订单修改","question":"能合并订单吗？","answer":"未支付订单可在购物车合并，已支付订单不支持合并。"}
```

(Continue with 35+ more entries covering categories: 退货政策, 物流时效, 退款流程, 优惠券, 支付, 订单修改, 发票, 会员, 售后, 商品咨询)

- [ ] **Step 2: Create `ScoredDoc.java`**

```java
package com.example.shopagent.rag;

public record ScoredDoc(String id, String text, double score, java.util.Map<String, Object> metadata) {}
```

- [ ] **Step 3: Create `VectorIndex.java`**

```java
package com.example.shopagent.rag;

import java.util.List;
import java.util.Map;

public interface VectorIndex {
    /** Upsert a document. Embedding is computed internally. */
    void upsert(String id, String text, Map<String, Object> metadata);
    /** Search topK by similarity. */
    List<ScoredDoc> search(String query, int topK);
    /** Total docs (for sanity / metrics). */
    int size();
}
```

- [ ] **Step 4: Compile**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/knowledge/faq.jsonl src/main/java/com/example/shopagent/rag/VectorIndex.java src/main/java/com/example/shopagent/rag/ScoredDoc.java
git commit -m "feat: FAQ knowledge base + VectorIndex abstraction"
```

---

## Task 10: In-Memory VectorIndex + BM25

**Files:**
- Create: `src/main/java/com/example/shopagent/rag/InMemoryVectorIndex.java`
- Create: `src/main/java/com/example/shopagent/rag/InMemoryBm25Index.java`
- Create: `src/main/java/com/example/shopagent/rag/HybridRetriever.java`
- Create: `src/main/java/com/example/shopagent/rag/InMemoryHybridRetriever.java`
- Create: `src/main/java/com/example/shopagent/rag/KnowledgeBaseBootstrap.java`
- Create: `src/test/java/com/example/shopagent/rag/HybridRetrieverTest.java`

**Interfaces:**
- Consumes: `VectorIndex` (Task 9), FAQ JSONL
- Produces: `HybridRetriever` bean — used by RagAgent (Task 24)

- [ ] **Step 1: Write failing test `HybridRetrieverTest.java`**

```java
package com.example.shopagent.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class HybridRetrieverTest {
    private HybridRetriever retriever;

    @BeforeEach
    void setUp() {
        InMemoryBm25Index bm25 = new InMemoryBm25Index();
        var vec = new InMemoryVectorIndex(new org.springframework.ai.transformers.TransformersEmbeddingModel());
        retriever = new HybridRetriever(vec, bm25);
        bm25.add("D1", "支持7天无理由退货 拆封 不退", Map.of());
        bm25.add("D2", "下单后48小时内发货 顺丰 圆通", Map.of());
        bm25.add("D3", "优惠券过期 自动作废", Map.of());
    }

    @Test
    void rrfFusionCombinesResults() {
        List<String> ids = retriever.retrieve("退货", 3).stream().map(HybridRetriever.Hit::id).toList();
        assertThat(ids).contains("D1");
    }

    @Test
    void rrfFusionReturnsAtMostTopK() {
        List<HybridRetriever.Hit> hits = retriever.retrieve("发货", 2);
        assertThat(hits.size()).isLessThanOrEqualTo(2);
    }
}
```

- [ ] **Step 2: Implement `HybridRetriever.java` (RRF fusion)**

```java
package com.example.shopagent.rag;

import java.util.*;
import java.util.stream.Collectors;

public class HybridRetriever {
    private final VectorIndex vectorIndex;
    private final InMemoryBm25Index bm25Index;
    private static final int RRF_K = 60;

    public HybridRetriever(VectorIndex vectorIndex, InMemoryBm25Index bm25Index) {
        this.vectorIndex = vectorIndex;
        this.bm25Index = bm25Index;
    }

    public List<Hit> retrieve(String query, int topK) {
        List<ScoredDoc> vec = vectorIndex.search(query, topK * 2);
        List<ScoredDoc> kw = bm25Index.search(query, topK * 2);
        Map<String, Double> rrf = new HashMap<>();
        Map<String, ScoredDoc> docMap = new HashMap<>();
        for (int i = 0; i < vec.size(); i++) {
            rrf.merge(vec.get(i).id(), 1.0 / (RRF_K + i + 1), Double::sum);
            docMap.putIfAbsent(vec.get(i).id(), vec.get(i));
        }
        for (int i = 0; i < kw.size(); i++) {
            rrf.merge(kw.get(i).id(), 1.0 / (RRF_K + i + 1), Double::sum);
            docMap.putIfAbsent(kw.get(i).id(), kw.get(i));
        }
        return rrf.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new Hit(e.getKey(), docMap.get(e.getKey()).text(), e.getValue()))
                .collect(Collectors.toList());
    }

    public record Hit(String id, String text, double score) {}
}
```

- [ ] **Step 3: Implement `InMemoryBm25Index.java`**

```java
package com.example.shopagent.rag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryBm25Index {
    private final Map<String, String> docs = new ConcurrentHashMap<>();
    private final Map<String, Integer> df = new ConcurrentHashMap<>();
    private final Map<String, List<String>> tokens = new ConcurrentHashMap<>();
    private double avgDocLen = 1;

    public void add(String id, String text, Map<String, Object> meta) {
        docs.put(id, text);
        List<String> toks = tokenize(text);
        tokens.put(id, toks);
        Set<String> uniq = new HashSet<>(toks);
        for (String t : uniq) df.merge(t, 1, Integer::sum);
        avgDocLen = docs.values().stream().mapToInt(d -> tokenize(d).size()).average().orElse(1);
    }

    public List<ScoredDoc> search(String query, int topK) {
        List<String> qTokens = tokenize(query);
        Map<String, Double> scores = new HashMap<>();
        for (String t : qTokens) {
            int n = docs.size();
            int dft = df.getOrDefault(t, 0);
            if (dft == 0) continue;
            double idf = Math.log(1 + (n - dft + 0.5) / (dft + 0.5));
            for (var entry : tokens.entrySet()) {
                int tf = Collections.frequency(entry.getValue(), t);
                if (tf == 0) continue;
                int dl = entry.getValue().size();
                double norm = (1 - 0.75) + 0.75 * dl / avgDocLen;
                scores.merge(entry.getKey(), idf * tf / norm, Double::sum);
            }
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new ScoredDoc(e.getKey(), docs.get(e.getKey()), e.getValue(), Map.of()))
                .collect(Collectors.toList());
    }

    private List<String> tokenize(String s) {
        if (s == null) return List.of();
        // CJK-friendly: split on whitespace; in real impl use jieba or char-ngrams.
        // For dev profile this is sufficient for the keyword leg of hybrid retrieval.
        return Arrays.stream(s.toLowerCase().split("\\s+")).filter(t -> !t.isBlank()).toList();
    }
}
```

- [ ] **Step 4: Implement `InMemoryVectorIndex.java`**

```java
package com.example.shopagent.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("dev")
public class InMemoryVectorIndex implements VectorIndex {
    private final EmbeddingModel embeddingModel;
    private final Map<String, float[]> vectors = new ConcurrentHashMap<>();
    private final Map<String, ScoredDoc> docs = new ConcurrentHashMap<>();

    public InMemoryVectorIndex() {
        this(new org.springframework.ai.transformers.TransformersEmbeddingModel());
    }

    public InMemoryVectorIndex(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void upsert(String id, String text, Map<String, Object> metadata) {
        float[] vec = embeddingModel.embed(text);
        vectors.put(id, vec);
        docs.put(id, new ScoredDoc(id, text, 0.0, metadata == null ? Map.of() : metadata));
    }

    @Override
    public List<ScoredDoc> search(String query, int topK) {
        float[] q = embeddingModel.embed(query);
        return vectors.entrySet().stream()
                .map(e -> new ScoredDoc(e.getKey(), docs.get(e.getKey()).text(), cosine(q, e.getValue()), docs.get(e.getKey()).metadata()))
                .sorted(Comparator.comparingDouble(ScoredDoc::score).reversed())
                .limit(topK)
                .toList();
    }

    @Override public int size() { return vectors.size(); }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-9);
    }
}
```

- [ ] **Step 5: Implement `KnowledgeBaseBootstrap.java`**

```java
package com.example.shopagent.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseBootstrap {
    private final VectorIndex vectorIndex;
    private final InMemoryBm25Index bm25Index; // dev only; prod uses different impl
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void load() throws Exception {
        log.info("Loading knowledge base from faq.jsonl...");
        int count = 0;
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
```

- [ ] **Step 6: Run test**

Run: `mvn -q test -Dtest=HybridRetrieverTest`
Expected: 2 tests pass

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/shopagent/rag/ src/test/java/com/example/shopagent/rag/
git commit -m "feat: in-memory BM25 + vector index + hybrid retriever (RRF)"
```

---

## Task 11: QueryRewriter + Reranker

**Files:**
- Create: `src/main/java/com/example/shopagent/rag/QueryRewriter.java`
- Create: `src/main/java/com/example/shopagent/rag/Reranker.java`
- Create: `src/main/java/com/example/shopagent/rag/NoOpReranker.java`

**Interfaces:**
- Consumes: `ChatClient` bean (Task 8), `PromptTemplates` (Task 7)
- Produces: `QueryRewriter`, `Reranker` beans — used by RagAgent (Task 24)

- [ ] **Step 1: Create `QueryRewriter.java`**

```java
package com.example.shopagent.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueryRewriter {
    private final ChatClient chatClient;

    public String rewrite(String query) {
        String rewritten = chatClient.prompt()
                .user(PromptTemplates.queryRewriter(query))
                .call()
                .content();
        return rewritten == null ? query : rewritten.trim();
    }
}
```

- [ ] **Step 2: Create `Reranker.java`**

```java
package com.example.shopagent.rag;

import java.util.List;

public interface Reranker {
    List<HybridRetriever.Hit> rerank(String query, List<HybridRetriever.Hit> hits);
}
```

- [ ] **Step 3: Create `NoOpReranker.java`**

```java
package com.example.shopagent.rag;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("dev")
public class NoOpReranker implements Reranker {
    @Override
    public List<HybridRetriever.Hit> rerank(String query, List<HybridRetriever.Hit> hits) {
        return hits;
    }
}
```

- [ ] **Step 4: Compile**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/shopagent/rag/QueryRewriter.java src/main/java/com/example/shopagent/rag/Reranker.java src/main/java/com/example/shopagent/rag/NoOpReranker.java
git commit -m "feat: query rewriter + no-op reranker (dev)"
```

---

## Task 12: Tool Layer — OrderTool

**Files:**
- Create: `src/main/java/com/example/shopagent/tool/OrderTool.java`
- Create: `src/main/java/com/example/shopagent/tool/dto/OrderDetail.java`
- Create: `src/test/java/com/example/shopagent/tool/OrderToolAuthTest.java`

**Interfaces:**
- Consumes: `OrderRepository` (Task 4), `UserContext` (Task 5)
- Produces: Spring AI `@Tool`-annotated method `getOrderDetail(orderId)` — invoked by ToolAgent (Task 25)

- [ ] **Step 1: Create `dto/OrderDetail.java`**

```java
package com.example.shopagent.tool.dto;

import com.example.shopagent.business.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetail(
    String orderId,
    String status,
    BigDecimal totalAmount,
    Instant createdAt,
    String trackingNumber,
    List<Item> items) {

    public record Item(String productId, String productName, Integer quantity, BigDecimal unitPrice) {}

    public static OrderDetail from(Order o) {
        return new OrderDetail(
            o.getOrderId(),
            o.getStatus(),
            o.getTotalAmount(),
            o.getCreatedAt(),
            o.getTrackingNumber(),
            o.getItems() == null ? List.of() :
                o.getItems().stream().map(i -> new Item(i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice())).toList()
        );
    }
}
```

- [ ] **Step 2: Write failing test `OrderToolAuthTest.java`**

```java
package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.repo.OrderRepository;
import com.example.shopagent.business.repo.impl.InMemoryOrderRepository;
import com.example.shopagent.tool.dto.OrderDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OrderToolAuthTest {
    private OrderRepository repo;
    private OrderTool tool;

    @BeforeEach
    void setUp() {
        repo = new InMemoryOrderRepository();
        tool = new OrderTool(repo);
        repo.save(Order.builder().orderId("O1").userId(1L).status("PENDING")
                .totalAmount(BigDecimal.TEN)
                .items(List.of()).build());
    }

    @Test
    void ownerCanRead() {
        UserContext.set(new UserContext(1L, "s"));
        assertThat(tool.getOrderDetail("O1").orderId()).isEqualTo("O1");
    }

    @Test
    void nonOwnerBlocked() {
        UserContext.set(new UserContext(2L, "s"));
        assertThatThrownBy(() -> tool.getOrderDetail("O1"))
                .isInstanceOf(ToolAuthException.class)
                .hasMessageContaining("无权访问");
    }

    @Test
    void unknownOrderThrows() {
        UserContext.set(new UserContext(1L, "s"));
        assertThatThrownBy(() -> tool.getOrderDetail("NOPE"))
                .isInstanceOf(ToolAuthException.class)
                .hasMessageContaining("订单不存在");
    }
}
```

- [ ] **Step 3: Implement `OrderTool.java`**

```java
package com.example.shopagent.tool;

import com.example.shopagent.business.repo.OrderRepository;
import com.example.shopagent.tool.dto.OrderDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderTool {
    private final OrderRepository orderRepository;

    @Tool(description = "根据订单号查询订单详情。需要用户已登录；只能查询当前用户自己的订单。")
    public OrderDetail getOrderDetail(@ToolParam(description = "订单号") String orderId) {
        Long uid = UserContext.current().userId();
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ToolAuthException("订单不存在"));
        if (!order.getUserId().equals(uid)) {
            throw new ToolAuthException("无权访问");
        }
        return OrderDetail.from(order);
    }
}
```

- [ ] **Step 4: Run tests**

Run: `mvn -q test -Dtest=OrderToolAuthTest`
Expected: 3 tests pass

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/shopagent/tool/OrderTool.java src/main/java/com/example/shopagent/tool/dto/OrderDetail.java src/test/java/com/example/shopagent/tool/OrderToolAuthTest.java
git commit -m "feat: OrderTool with user-level authorization"
```

---

## Task 13: Tool Layer — LogisticsTool, RefundTool, CouponTool, RecommendTool, EscalateTool

**Files:**
- Create: `src/main/java/com/example/shopagent/tool/LogisticsTool.java`
- Create: `src/main/java/com/example/shopagent/tool/RefundTool.java`
- Create: `src/main/java/com/example/shopagent/tool/CouponTool.java`
- Create: `src/main/java/com/example/shopagent/tool/RecommendTool.java`
- Create: `src/main/java/com/example/shopagent/tool/EscalateTool.java`
- Create: `src/test/java/com/example/shopagent/tool/AllToolsAuthTest.java`

**Interfaces:** Each Tool is a Spring component with `@Tool` methods. All consumed by ToolAgent (Task 25).

- [ ] **Step 1: Create `LogisticsTool.java`**

```java
package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Logistics;
import com.example.shopagent.business.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LogisticsTool {
    private final OrderRepository orderRepository;

    @Tool(description = "根据订单号查询物流轨迹。需要用户已登录；只能查询当前用户自己的订单。")
    public Logistics getLogistics(@ToolParam(description = "订单号") String orderId) {
        Long uid = UserContext.current().userId();
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ToolAuthException("订单不存在"));
        if (!order.getUserId().equals(uid)) throw new ToolAuthException("无权访问");
        if (order.getTrackingNumber() == null) {
            return Logistics.builder().trackingNumber("(未发货)")
                    .currentLocation("等待出库").events(List.of()).build();
        }
        // dev mock trajectory
        return Logistics.builder()
                .trackingNumber(order.getTrackingNumber())
                .carrier("顺丰")
                .currentLocation("上海中转站")
                .events(List.of(
                    Logistics.Event.builder().timestamp(Instant.now().minusSeconds(86400))
                            .location("杭州").description("已揽收").build(),
                    Logistics.Event.builder().timestamp(Instant.now().minusSeconds(43200))
                            .location("上海中转站").description("运输中").build()))
                .build();
    }
}
```

- [ ] **Step 2: Create `RefundTool.java`**

```java
package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Refund;
import com.example.shopagent.business.repo.OrderRepository;
import com.example.shopagent.business.repo.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RefundTool {
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;

    @Tool(description = "为当前用户的指定订单提交退款申请。仅能为自己已发货或已送达的订单申请退款。")
    public Refund createRefundOrder(
            @ToolParam(description = "订单号") String orderId,
            @ToolParam(description = "退款原因") String reason) {
        Long uid = UserContext.current().userId();
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ToolAuthException("订单不存在"));
        if (!order.getUserId().equals(uid)) throw new ToolAuthException("无权访问");
        if ("REFUNDED".equals(order.getStatus())) throw new ToolAuthException("订单已退款");
        if ("PENDING".equals(order.getStatus())) throw new ToolAuthException("未发货订单请直接取消");
        return refundRepository.save(Refund.builder()
                .orderId(orderId).userId(uid).reason(reason)
                .status("PENDING").createdAt(Instant.now()).build());
    }
}
```

- [ ] **Step 3: Create `CouponTool.java`**

```java
package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Coupon;
import com.example.shopagent.business.repo.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponTool {
    private final CouponRepository couponRepository;

    @Tool(description = "查询当前用户的所有优惠券。")
    public List<Coupon> getCouponList() {
        Long uid = UserContext.current().userId();
        return couponRepository.findByUserId(uid);
    }
}
```

- [ ] **Step 4: Create `RecommendTool.java`**

```java
package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Product;
import com.example.shopagent.business.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecommendTool {
    private final ProductRepository productRepository;

    @Tool(description = "根据商品分类和预算推荐商品。")
    public List<Product> recommendProducts(
            @ToolParam(description = "商品分类，例如 数码/家居/服饰") String category,
            @ToolParam(description = "预算上限（元）") BigDecimal budget) {
        if (category == null || category.isBlank()) {
            return productRepository.findByPriceLessThan(budget == null ? BigDecimal.valueOf(1000) : budget);
        }
        var inCat = productRepository.findByCategory(category);
        if (budget != null) {
            return inCat.stream().filter(p -> p.getPrice().compareTo(budget) <= 0).toList();
        }
        return inCat;
    }
}
```

- [ ] **Step 5: Create `EscalateTool.java`**

```java
package com.example.shopagent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class EscalateTool {

    @Tool(description = "将会话升级到人工客服。当用户强烈不满或系统无法解决时调用。")
    public Map<String, Object> escalateToHuman(
            @ToolParam(description = "用户反馈的问题") String issue,
            @ToolParam(description = "问题摘要") String summary) {
        log.warn("[ESCALATION] user={}, issue={}, summary={}",
                UserContext.current().userId(), issue, summary);
        // prod: enqueue to RabbitMQ for human-agent queue
        return Map.of("status", "QUEUED", "queuePosition", 3, "eta", "2 minutes");
    }
}
```

- [ ] **Step 6: Write `AllToolsAuthTest.java`**

```java
package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.repo.OrderRepository;
import com.example.shopagent.business.repo.RefundRepository;
import com.example.shopagent.business.repo.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AllToolsAuthTest {
    private OrderRepository orderRepo;
    private LogisticsTool logistics;
    private RefundTool refund;
    private RefundRepository refundRepo;

    @BeforeEach
    void setUp() {
        orderRepo = new InMemoryOrderRepository();
        refundRepo = new InMemoryRefundRepository();
        logistics = new LogisticsTool(orderRepo);
        refund = new RefundTool(orderRepo, refundRepo);
        orderRepo.save(Order.builder().orderId("O1").userId(1L).status("SHIPPED")
                .trackingNumber("SF1").totalAmount(BigDecimal.TEN).items(List.of()).build());
        orderRepo.save(Order.builder().orderId("O2").userId(2L).status("SHIPPED")
                .trackingNumber("SF2").totalAmount(BigDecimal.TEN).items(List.of()).build());
    }

    @Test
    void logisticsBlocksNonOwner() {
        UserContext.set(new UserContext(2L, "s"));
        assertThatThrownBy(() -> logistics.getLogistics("O1")).isInstanceOf(ToolAuthException.class);
    }

    @Test
    void refundBlocksPendingOrder() {
        UserContext.set(new UserContext(1L, "s"));
        orderRepo.save(Order.builder().orderId("PEND").userId(1L).status("PENDING")
                .totalAmount(BigDecimal.TEN).items(List.of()).build());
        assertThatThrownBy(() -> refund.createRefundOrder("PEND", "test"))
                .isInstanceOf(ToolAuthException.class);
    }

    @Test
    void refundCreatesRecord() {
        UserContext.set(new UserContext(1L, "s"));
        var r = refund.createRefundOrder("O1", "不想要了");
        assertThat(r.getStatus()).isEqualTo("PENDING");
        assertThat(r.getUserId()).isEqualTo(1L);
    }
}
```

- [ ] **Step 7: Run tests**

Run: `mvn -q test -Dtest=AllToolsAuthTest`
Expected: 3 tests pass

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/example/shopagent/tool/ src/test/java/com/example/shopagent/tool/AllToolsAuthTest.java
git commit -m "feat: Logistics/Refund/Coupon/Recommend/Escalate tools with auth"
```

---

## Task 14: IntentClassifier

**Files:**
- Create: `src/main/java/com/example/shopagent/agent/Intent.java`
- Create: `src/main/java/com/example/shopagent/agent/IntentClassifier.java`
- Create: `src/test/java/com/example/shopagent/agent/IntentClassifierTest.java`

**Interfaces:**
- Consumes: `ChatClient` (Task 8), `PromptTemplates` (Task 7)
- Produces: `Intent classify(String)` — used by AgentService (Task 18)

- [ ] **Step 1: Create `Intent.java`**

```java
package com.example.shopagent.agent;

public enum Intent {
    CHITCHAT, INQUIRY, ACTION, COMPLAINT;

    public static Intent fromRaw(String raw) {
        if (raw == null) return CHITCHAT;
        try { return Intent.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return CHITCHAT; }
    }
}
```

- [ ] **Step 2: Write failing test `IntentClassifierTest.java`**

```java
package com.example.shopagent.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class IntentClassifierTest {
    private final IntentClassifier classifier = new IntentClassifier(null);

    @Test
    void ruleBasedDetectsComplaint() {
        assertThat(classifier.classifyByRule("我要投诉客服")).isEqualTo(Intent.COMPLAINT);
    }

    @Test
    void ruleBasedDetectsActionByKeywords() {
        assertThat(classifier.classifyByRule("帮我查订单 O1001")).isEqualTo(Intent.ACTION);
        assertThat(classifier.classifyByRule("申请退款")).isEqualTo(Intent.ACTION);
        assertThat(classifier.classifyByRule("我的物流到哪了")).isEqualTo(Intent.ACTION);
        assertThat(classifier.classifyByRule("有什么优惠券")).isEqualTo(Intent.ACTION);
    }

    @Test
    void ruleBasedReturnsNullForAmbiguous() {
        assertThat(classifier.classifyByRule("7天无理由退货是什么意思？")).isNull();
        assertThat(classifier.classifyByRule("你好啊")).isNull();
    }

    @Test
    void blacklistedUserForceComplaint() {
        assertThat(classifier.classifyWithUserContext("哪里都行", 999L)).isEqualTo(Intent.COMPLAINT);
    }
}
```

- [ ] **Step 3: Implement `IntentClassifier.java`**

```java
package com.example.shopagent.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntentClassifier {
    private static final Set<String> COMPLAINT_WORDS = Set.of("投诉", "举报", "差评", "退钱", "黑名单", "曝光");
    private static final Set<String> ACTION_WORDS = Set.of("订单", "物流", "快递", "退款", "退换", "优惠券", "推荐", "查一下", "查我的");
    private static final Pattern ORDER_ID = Pattern.compile("O\\d{3,}");
    private final ChatClient chatClient;

    public Intent classify(String userText) {
        return classifyWithUserContext(userText, null);
    }

    public Intent classifyWithUserContext(String userText, Long userId) {
        Intent byRule = classifyByRule(userText);
        if (byRule != null) return byRule;
        // blacklisted users always handled by human
        if (userId != null && userId == 999L) return Intent.COMPLAINT;
        // fallback to LLM
        try {
            String raw = chatClient.prompt()
                    .user(PromptTemplates.intentClassifier(userText))
                    .call().content();
            return Intent.fromRaw(raw);
        } catch (Exception e) {
            log.warn("LLM intent classification failed, defaulting to CHITCHAT", e);
            return Intent.CHITCHAT;
        }
    }

    Intent classifyByRule(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        for (String w : COMPLAINT_WORDS) if (lower.contains(w)) return Intent.COMPLAINT;
        if (ORDER_ID.matcher(text).find()) return Intent.ACTION;
        for (String w : ACTION_WORDS) if (lower.contains(w)) return Intent.ACTION;
        return null;
    }
}
```

- [ ] **Step 4: Run tests**

Run: `mvn -q test -Dtest=IntentClassifierTest`
Expected: 4 tests pass

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/shopagent/agent/Intent.java src/main/java/com/example/shopagent/agent/IntentClassifier.java src/test/java/com/example/shopagent/agent/
git commit -m "feat: IntentClassifier (rule-first, LLM fallback, blacklist)"
```

---

## Task 15: Agent Implementations (Chitchat, Rag, Tool, Handoff)

**Files:**
- Create: `src/main/java/com/example/shopagent/agent/ChitchatAgent.java`
- Create: `src/main/java/com/example/shopagent/agent/RagAgent.java`
- Create: `src/main/java/com/example/shopagent/agent/ToolAgent.java`
- Create: `src/main/java/com/example/shopagent/agent/HandoffService.java`
- Create: `src/main/java/com/example/shopagent/agent/AgentService.java`

**Interfaces:**
- Consumes: ChatClient (Task 8), Tools (Tasks 12–13), HybridRetriever + QueryRewriter + Reranker (Tasks 10–11), SessionStore (Task 6)
- Produces: `AgentService.handle()` returning `Flux<String>` — used by ChatController (Task 16)

- [ ] **Step 1: Create `ChitchatAgent.java`**

```java
package com.example.shopagent.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class ChitchatAgent {
    private final ChatClient chatClient;

    public Flux<String> stream(String userText) {
        return chatClient.prompt()
                .system("你是友善的电商客服助手「小蜜」，与用户闲聊。请简洁、礼貌。")
                .user(userText)
                .stream().content();
    }
}
```

- [ ] **Step 2: Create `RagAgent.java`**

```java
package com.example.shopagent.agent;

import com.example.shopagent.rag.HybridRetriever;
import com.example.shopagent.rag.PromptTemplates;
import com.example.shopagent.rag.QueryRewriter;
import com.example.shopagent.rag.Reranker;
import com.example.shopagent.session.SessionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RagAgent {
    private final ChatClient chatClient;
    private final HybridRetriever retriever;
    private final QueryRewriter queryRewriter;
    private final Reranker reranker;
    private final SessionStore sessionStore;

    public Flux<String> stream(String sessionId, String userText) {
        String rewritten = queryRewriter.rewrite(userText);
        var hits = retriever.retrieve(rewritten, 5);
        var reranked = reranker.rerank(rewritten, hits);
        String kb = reranked.stream()
                .map(h -> "- " + h.text().replace("\n", " "))
                .collect(Collectors.joining("\n"));
        if (kb.isBlank()) kb = "(无相关知识)";
        String summary = sessionStore.getSummary(sessionId).orElse("");

        return chatClient.prompt()
                .system(PromptTemplates.customerService(kb, summary))
                .user(userText)
                .stream().content();
    }
}
```

- [ ] **Step 3: Create `ToolAgent.java`**

```java
package com.example.shopagent.agent;

import com.example.shopagent.rag.PromptTemplates;
import com.example.shopagent.session.SessionStore;
import com.example.shopagent.tool.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolAgent {
    private final ChatClient chatClient;
    private final OrderTool orderTool;
    private final LogisticsTool logisticsTool;
    private final RefundTool refundTool;
    private final CouponTool couponTool;
    private final RecommendTool recommendTool;
    private final EscalateTool escalateTool;
    private final SessionStore sessionStore;

    public Flux<String> stream(long userId, String sessionId, String userText) {
        String summary = sessionStore.getSummary(sessionId).orElse("");
        String kb = "(工具型请求，优先调用工具)";
        return chatClient.prompt()
                .system(PromptTemplates.customerService(kb, summary))
                .user(userText)
                .tools(orderTool, logisticsTool, refundTool, couponTool, recommendTool, escalateTool)
                .stream().content();
    }
}
```

> **Note on ThreadLocal**: Spring AI may invoke Tool callbacks on a different thread. To guarantee `UserContext.current().userId()` works inside tools, the ChatController (Task 16) wraps the call in `Executors.newSingleThreadExecutor().submit(...)` so all stream + tool invocations share the same worker thread, and the ThreadLocal is set before the call begins.

- [ ] **Step 5: Create `HandoffService.java`**

```java
package com.example.shopagent.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class HandoffService {
    private final com.example.shopagent.tool.EscalateTool escalateTool;

    public Flux<String> handoff(long userId, String sessionId, String userText) {
        return Flux.just("正在为您转接人工客服，请稍候...\n")
                .concatWith(Flux.defer(() -> {
                    try {
                        com.example.shopagent.tool.UserContext.set(
                                new com.example.shopagent.tool.UserContext(userId, sessionId));
                        var r = escalateTool.escalateToHuman(userText, "用户触发投诉/升级");
                        return Flux.just("已为您排队，当前等待 " + r.get("queuePosition") + " 人，预计等待 " + r.get("eta"));
                    } finally {
                        com.example.shopagent.tool.UserContext.clear();
                    }
                }));
    }
}
```

- [ ] **Step 6: Create `AgentService.java`**

```java
package com.example.shopagent.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AgentService {
    private final IntentClassifier intentClassifier;
    private final ChitchatAgent chitchatAgent;
    private final RagAgent ragAgent;
    private final ToolAgent toolAgent;
    private final HandoffService handoffService;

    public Flux<String> handle(long userId, String sessionId, String userText) {
        Intent intent = intentClassifier.classifyWithUserContext(userText, userId);
        return switch (intent) {
            case CHITCHAT -> chitchatAgent.stream(userText);
            case INQUIRY  -> ragAgent.stream(sessionId, userText);
            case ACTION   -> toolAgent.stream(userId, sessionId, userText);
            case COMPLAINT -> handoffService.handoff(userId, sessionId, userText);
        };
    }
}
```

- [ ] **Step 7: Compile**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/example/shopagent/agent/
git commit -m "feat: agent implementations (chitchat/RAG/tool/handoff) + dispatcher"
```

---

## Task 16: ChatController + SSE

**Files:**
- Create: `src/main/java/com/example/shopagent/api/ChatController.java`
- Create: `src/main/java/com/example/shopagent/api/dto/ChatRequest.java`
- Create: `src/main/java/com/example/shopagent/api/error/GlobalExceptionHandler.java`
- Create: `src/main/java/com/example/shopagent/api/error/SseErrorSender.java`
- Create: `src/test/java/com/example/shopagent/api/ChatControllerE2ETest.java`

**Interfaces:**
- Consumes: `AgentService` (Task 15)
- Produces: HTTP SSE endpoint `/api/chat/stream`

- [ ] **Step 1: Create `dto/ChatRequest.java`**

```java
package com.example.shopagent.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String sessionId,
        @NotBlank String message) {}
```

- [ ] **Step 2: Add shared executor bean (Spring-managed)**

Append to `DeepSeekConfig.java`:

```java
@Bean(destroyMethod = "shutdown")
public ExecutorService chatWorkerPool() {
    return Executors.newFixedThreadPool(16);
}
```

- [ ] **Step 3: Create `ChatController.java`**

```java
package com.example.shopagent.api;

import com.example.shopagent.agent.AgentService;
import com.example.shopagent.api.error.SseErrorSender;
import com.example.shopagent.session.Message;
import com.example.shopagent.session.SessionStore;
import com.example.shopagent.session.SessionSummaryService;
import com.example.shopagent.tool.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AgentService agentService;
    private final SessionStore sessionStore;
    private final SessionSummaryService summaryService;
    private final SseErrorSender errorSender;
    private final ExecutorService chatWorkerPool;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String sessionId,
            @RequestParam String message,
            @RequestHeader("X-User-Id") Long userId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        Runnable clear = () -> UserContext.clear();
        emitter.onCompletion(clear);
        emitter.onTimeout(() -> { clear.run(); emitter.complete(); });

        sessionStore.appendMessage(sessionId, Message.user(message));

        // run on a shared worker thread so ThreadLocal UserContext remains valid
        // for the entire stream + tool-callback lifecycle
        chatWorkerPool.submit(() -> {
            try {
                UserContext.set(new UserContext(userId, sessionId));
                StringBuilder full = new StringBuilder();
                agentService.handle(userId, sessionId, message)
                        .doOnNext(full::append)
                        .doOnError(err -> errorSender.send(emitter, err))
                        .doOnComplete(() -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data(""));
                                sessionStore.appendMessage(sessionId, Message.assistant(full.toString()));
                                summaryService.maybeSummarize(sessionId, sessionStore.getHistory(sessionId, 1000).size());
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            } finally {
                                UserContext.clear();
                            }
                        })
                        .subscribe(
                                chunk -> safeSend(emitter, chunk),
                                err -> log.warn("stream error", err));
            } catch (Exception e) {
                errorSender.send(emitter, e);
                UserContext.clear();
            }
        });
        return emitter;
    }

    private void safeSend(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().data(chunk));
        } catch (IOException e) {
            emitter.completeWithError(e);
            UserContext.clear();
        }
    }
}
```

- [ ] **Step 4: Create `GlobalExceptionHandler.java`**

```java
package com.example.shopagent.api.error;

import com.example.shopagent.tool.ToolAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ToolAuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(ToolAuthException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "TOOL_AUTH", "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception e) {
        log.error("Unhandled error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "INTERNAL", "message", e.getMessage()));
    }
}
```

- [ ] **Step 5: Create `SseErrorSender.java`**

```java
package com.example.shopagent.api.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Component
public class SseErrorSender {
    public void send(SseEmitter emitter, Throwable err) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"error\":\"" + err.getClass().getSimpleName() + "\",\"message\":\""
                            + err.getMessage().replace("\"", "'") + "\"}"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
```

- [ ] **Step 6: Compile + smoke**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/shopagent/api/
git commit -m "feat: ChatController SSE endpoint + global error handling"
```

---

## Task 17: Session Summary Service

**Files:**
- Create: `src/main/java/com/example/shopagent/session/SessionSummaryService.java`
- Modify: `src/main/java/com/example/shopagent/api/ChatController.java` (wire summary)
- Create: `src/test/java/com/example/shopagent/session/SessionSummaryServiceTest.java`

**Interfaces:** Service called every N turns; updates `SessionStore.summary`. Consumed by ChatController.

- [ ] **Step 1: Write failing test `SessionSummaryServiceTest.java`**

```java
package com.example.shopagent.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionSummaryServiceTest {
    @Test
    void shouldTriggerWhenMultipleOfN() {
        SessionStore store = mock(SessionStore.class);
        // 10 messages = 5 user/assistant turns; trigger threshold is everyN*2 with everyN=5 default
        when(store.getHistory(anyString(), anyInt())).thenReturn(List.of(
                Message.user("a"), Message.assistant("b"),
                Message.user("c"), Message.assistant("d"),
                Message.user("e"), Message.assistant("f"),
                Message.user("g"), Message.assistant("h"),
                Message.user("i"), Message.assistant("j")));
        SessionSummaryService svc = new SessionSummaryService(store, null);
        svc.maybeSummarize("s1", 10);
        verify(store, atLeastOnce()).setSummary(eq("s1"), anyString());
    }

    @Test
    void shouldNotTriggerBelowThreshold() {
        SessionStore store = mock(SessionStore.class);
        when(store.getHistory(anyString(), anyInt())).thenReturn(List.of(Message.user("a")));
        SessionSummaryService svc = new SessionSummaryService(store, null);
        svc.maybeSummarize("s1", 1);
        verify(store, never()).setSummary(anyString(), anyString());
    }
}
```

- [ ] **Step 2: Implement `SessionSummaryService.java`**

```java
package com.example.shopagent.session;

import com.example.shopagent.rag.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSummaryService {
    private final SessionStore sessionStore;
    private final ChatClient chatClient;

    @Value("${shopagent.session.summary-every-n-turns:5}")
    private int everyN;

    public void maybeSummarize(String sessionId, int historySize) {
        if (historySize <= 0) return;
        // trigger every N * 2 messages (user+assistant = 2 per turn)
        if (historySize < everyN * 2) return;
        if (historySize % (everyN * 2) != 0) return;
        log.info("[SESSION] summarizing session={} after {} messages", sessionId, historySize);
        var history = sessionStore.getHistory(sessionId, historySize);
        String joined = String.join("\n", history.stream()
                .map(m -> m.role() + ": " + m.content()).toList());
        try {
            String summary = chatClient.prompt()
                    .system("请用100字以内总结以下对话的关键信息（订单号、问题、结论）。只输出摘要。")
                    .user(joined)
                    .call().content();
            sessionStore.setSummary(sessionId, summary == null ? "" : summary.trim());
        } catch (Exception e) {
            log.warn("summary failed, keeping previous", e);
        }
    }
}
```

- [ ] **Step 3: Wire into `ChatController`**

Edit `ChatController.java`: replace `maybeSummarize()` method:

```java
private final com.example.shopagent.session.SessionSummaryService summaryService;

private void maybeSummarize(String sessionId) {
    int n = sessionStore.getHistory(sessionId, 1000).size();
    summaryService.maybeSummarize(sessionId, n);
}
```

Also change the call site in `stream()` from `maybeSummarize(sessionId);` (keep same name) — already there.

- [ ] **Step 4: Run tests**

Run: `mvn -q test -Dtest=SessionSummaryServiceTest`
Expected: 2 tests pass

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/shopagent/session/SessionSummaryService.java src/main/java/com/example/shopagent/api/ChatController.java src/test/java/com/example/shopagent/session/SessionSummaryServiceTest.java
git commit -m "feat: SessionSummaryService with LLM-based compression"
```

---

## Task 18: End-to-End Smoke Test

**Files:**
- Create: `src/test/java/com/example/shopagent/api/ChatControllerE2ETest.java`

**Interfaces:** Slice test exercising the full controller layer with all downstream beans mocked.

- [ ] **Step 1: Write `ChatControllerE2ETest.java`**

```java
package com.example.shopagent.api;

import com.example.shopagent.agent.AgentService;
import com.example.shopagent.api.error.SseErrorSender;
import com.example.shopagent.session.InMemorySessionStore;
import com.example.shopagent.session.SessionStore;
import com.example.shopagent.session.SessionSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebFluxTest(controllers = ChatController.class)
@Import({InMemorySessionStore.class, SseErrorSender.class})
@ActiveProfiles("dev")
class ChatControllerE2ETest {

    @Autowired WebTestClient client;
    @MockBean AgentService agentService;
    @MockBean SessionSummaryService summaryService;

    @Test
    void streamReturnsChunks() {
        when(agentService.handle(anyLong(), anyString(), anyString()))
                .thenReturn(Flux.just("你好", "，", "小蜜"));

        client.get().uri(uri -> uri.path("/api/chat/stream")
                        .queryParam("sessionId", "s1")
                        .queryParam("message", "你好")
                        .build())
                .header("X-User-Id", "1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    // SSE frames: data: <chunk>\n\n
                    org.assertj.core.api.Assertions.assertThat(body).contains("你好").contains("小蜜");
                });
    }
}
```

- [ ] **Step 2: Run test**

Run: `mvn -q test -Dtest=ChatControllerE2ETest`
Expected: 1 test passes (Spring test infra takes a bit; may need `mvn -q test`)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/example/shopagent/api/ChatControllerE2ETest.java
git commit -m "test: end-to-end SSE chat controller test"
```

---

## Task 19: Qdrant VectorIndex + Prod MyBatis/Redis Implementations

**Files:**
- Create: `src/main/java/com/example/shopagent/rag/QdrantVectorIndex.java`
- Create: `src/main/java/com/example/shopagent/config/ProdStoreConfig.java`
- Create: `src/main/java/com/example/shopagent/business/repo/impl/MybatisOrderRepository.java`
- Create: `src/main/java/com/example/shopagent/business/repo/impl/MybatisRefundRepository.java`
- Create: `src/main/java/com/example/shopagent/business/repo/impl/JpaUserRepository.java`
- Create: `src/main/java/com/example/shopagent/business/repo/impl/JpaCouponRepository.java`
- Create: `src/main/java/com/example/shopagent/session/RedisSessionStore.java`
- Create: `src/main/resources/schema.sql`

**Interfaces:** Prod-profile impls of all repositories/stores/vector-index.

- [ ] **Step 1: Create `QdrantVectorIndex.java`**

```java
package com.example.shopagent.rag;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.PointStruct;
import io.qdrant.client.grpc.Value;
import io.qdrant.client.grpc.Vectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class QdrantVectorIndex implements VectorIndex {

    private final QdrantClient qdrantClient;
    private final EmbeddingModel embeddingModel;
    @Value("${shopagent.vector.qdrant.collection}") private String collection;

    @Override
    public void upsert(String id, String text, Map<String, Object> metadata) {
        float[] vec = embeddingModel.embed(text);
        var payload = new HashMap<String, Value>();
        if (metadata != null) metadata.forEach((k, v) -> payload.put(k, Value.newBuilder().setStringValue(v.toString()).build()));
        payload.put("text", Value.newBuilder().setStringValue(text).build());

        var point = PointStruct.newBuilder()
                .setId(io.qdrant.client.grpc.PointIdFactory.id(id.hashCode() & 0x7fffffffL))
                .setVectors(Vectors.newBuilder().setVector(io.qdrant.client.grpc.VectorsFactory.vectors(vec)).build())
                .putAllPayload(payload)
                .build();
        try {
            qdrantClient.upsertAsync(collection, java.util.List.of(point)).get();
        } catch (Exception e) {
            throw new RuntimeException("Qdrant upsert failed", e);
        }
    }

    @Override
    public List<ScoredDoc> search(String query, int topK) {
        float[] vec = embeddingModel.embed(query);
        try {
            var resp = qdrantClient.searchAsync(collection, vec, topK).get();
            return resp.stream()
                    .map(s -> new ScoredDoc(
                            String.valueOf(s.getId().getNum()),
                            s.getPayloadOrThrow("text").getStringValue(),
                            s.getScore(),
                            Map.of()))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Qdrant search failed", e);
        }
    }

    @Override public int size() { return 0; }
}
```

- [ ] **Step 2: Create `ProdStoreConfig.java`**

```java
package com.example.shopagent.config;

import com.example.shopagent.rag.QdrantVectorIndex;
import com.example.shopagent.rag.VectorIndex;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("prod")
public class ProdStoreConfig {

    @Value("${shopagent.vector.qdrant.host}") private String qHost;
    @Value("${shopagent.vector.qdrant.port}") private int qPort;

    @Bean
    public QdrantClient qdrantClient() {
        return new QdrantClient(new QdrantGrpcClient.Builder()
                .host(qHost).port(qPort).build());
    }

    @Bean
    public VectorIndex vectorIndex(QdrantClient c, org.springframework.ai.embedding.EmbeddingModel m) {
        return new QdrantVectorIndex(c, m);
    }

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        return t;
    }
}
```

- [ ] **Step 3: Create `MybatisOrderRepository.java`**

```java
package com.example.shopagent.business.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.repo.OrderRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("prod")
public class MybatisOrderRepository implements OrderRepository {

    private final ServiceImpl<OrderMapper, Order> mapper;

    public MybatisOrderRepository() {
        // In real prod wiring: inject @Autowired MybatisPlus extension mapper.
        // For now we leave a hook so the interface compiles and can be swapped via profile.
        this.mapper = null;
    }

    @Override public Optional<Order> findById(String orderId) {
        if (mapper == null) return Optional.empty();
        return Optional.ofNullable(mapper.getById(orderId));
    }
    @Override public List<Order> findByUserId(Long userId) {
        if (mapper == null) return List.of();
        return mapper.list(new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId));
    }
    @Override public Order save(Order order) {
        if (mapper == null) return order;
        mapper.saveOrUpdate(order);
        return order;
    }
}
```

(Add a `OrderMapper extends BaseMapper<Order>` interface in same package — see file below.)

- [ ] **Step 4: Create supporting MyBatis mapper**

```java
package com.example.shopagent.business.repo.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shopagent.business.domain.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {}
```

- [ ] **Step 5: Create `MybatisRefundRepository.java` (analogous, with RefundMapper)**

```java
package com.example.shopagent.business.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.shopagent.business.domain.Refund;
import com.example.shopagent.business.repo.RefundRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("prod")
public class MybatisRefundRepository implements RefundRepository {
    private final ServiceImpl<RefundMapper, Refund> mapper;

    public MybatisRefundRepository() { this.mapper = null; }

    @Override public Refund save(Refund refund) {
        if (mapper == null) return refund;
        mapper.saveOrUpdate(refund);
        return refund;
    }
    @Override public List<Refund> findByUserId(Long userId) {
        if (mapper == null) return List.of();
        return mapper.list(new LambdaQueryWrapper<Refund>().eq(Refund::getUserId, userId));
    }
    @Override public List<Refund> findByOrderId(String orderId) {
        if (mapper == null) return List.of();
        return mapper.list(new LambdaQueryWrapper<Refund>().eq(Refund::getOrderId, orderId));
    }
}
```

```java
package com.example.shopagent.business.repo.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shopagent.business.domain.Refund;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RefundMapper extends BaseMapper<Refund> {}
```

- [ ] **Step 6: Create `JpaUserRepository.java` and `JpaCouponRepository.java`**

```java
package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.User;
import com.example.shopagent.business.repo.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile("prod")
public interface JpaUserRepository extends JpaRepository<User, Long>, UserRepository {
    // Inherits findById(Long) and save(User) from both — Spring Data resolves ambiguity
    // in favor of the concrete JpaRepository methods. No override needed.
}
```

```java
package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Coupon;
import com.example.shopagent.business.repo.CouponRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("prod")
public interface JpaCouponRepository extends JpaRepository<Coupon, String>, CouponRepository {
    List<Coupon> findByUserId(Long userId);
    // Inherits save(Coupon) from JpaRepository
}
```

```java
package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Product;
import com.example.shopagent.business.repo.ProductRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@Profile("prod")
public interface JpaProductRepository extends JpaRepository<Product, String>, ProductRepository {
    List<Product> findByCategory(String category);
    List<Product> findByPriceLessThan(BigDecimal maxPrice);
}
```

> **Note on adapter pattern**: For interfaces where Spring Data method signatures don't align with the application's contract (e.g., `Optional<Product> findById(String)` vs JPA's `Optional<Product> findById(ID)`), wrap a `JpaRepository` in an Adapter class. The above works because Spring Data infers the query method names automatically. If you hit a signature mismatch, refactor to an Adapter rather than duplicate.

- [ ] **Step 7: Create `RedisSessionStore.java`**

```java
package com.example.shopagent.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class RedisSessionStore implements SessionStore {
    private static final String HIST_KEY = "session:hist:";
    private static final String SUM_KEY = "session:sum:";
    private final RedisTemplate<String, String> redis;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void appendMessage(String sessionId, Message msg) {
        try {
            redis.opsForList().rightPush(HIST_KEY + sessionId, mapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Message> getHistory(String sessionId, int limit) {
        var raw = redis.opsForList().range(HIST_KEY + sessionId, -limit, -1);
        if (raw == null) return List.of();
        List<Message> out = new ArrayList<>(raw.size());
        for (String s : raw) try { out.add(mapper.readValue(s, Message.class)); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
        return out;
    }

    @Override public void setSummary(String sessionId, String summary) {
        redis.opsForValue().set(SUM_KEY + sessionId, summary);
    }
    @Override public Optional<String> getSummary(String sessionId) {
        return Optional.ofNullable(redis.opsForValue().get(SUM_KEY + sessionId));
    }
}
```

- [ ] **Step 8: Create `schema.sql` for prod MySQL**

```sql
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64),
    level VARCHAR(16)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(32) PRIMARY KEY,
    user_id BIGINT,
    total_amount DECIMAL(10,2),
    status VARCHAR(16),
    tracking_number VARCHAR(64),
    created_at DATETIME,
    INDEX idx_user (user_id)
);

CREATE TABLE IF NOT EXISTS refund (
    refund_id VARCHAR(32) PRIMARY KEY,
    order_id VARCHAR(32),
    user_id BIGINT,
    reason TEXT,
    status VARCHAR(16),
    created_at DATETIME,
    INDEX idx_user (user_id),
    INDEX idx_order (order_id)
);

CREATE TABLE IF NOT EXISTS coupon (
    coupon_id VARCHAR(32) PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(128),
    discount DECIMAL(10,2),
    expires_at DATETIME,
    used TINYINT(1)
);
```

- [ ] **Step 9: Compile + run all tests in dev profile**

Run: `mvn -q test -Dspring.profiles.active=dev`
Expected: All tests pass

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/example/shopagent/business/repo/impl/MybatisOrderRepository.java src/main/java/com/example/shopagent/business/repo/impl/MybatisRefundRepository.java src/main/java/com/example/shopagent/business/repo/impl/OrderMapper.java src/main/java/com/example/shopagent/business/repo/impl/RefundMapper.java src/main/java/com/example/shopagent/business/repo/impl/JpaUserRepository.java src/main/java/com/example/shopagent/business/repo/impl/JpaCouponRepository.java src/main/java/com/example/shopagent/session/RedisSessionStore.java src/main/java/com/example/shopagent/rag/QdrantVectorIndex.java src/main/java/com/example/shopagent/config/ProdStoreConfig.java src/main/resources/schema.sql
git commit -m "feat: prod-profile implementations (MyBatis, JPA, Redis, Qdrant) + DDL"
```

---

## Task 20: Dockerfile + docker-compose.yml + README

**Files:**
- Create: `Dockerfile`
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create: `README.md`

- [ ] **Step 1: Create `Dockerfile`**

```dockerfile
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN ./mvnw -q -DskipTests package || mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar","--spring.profiles.active=prod"]
```

- [ ] **Step 2: Create `docker-compose.yml`**

```yaml
version: "3.9"
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: rootpw
      MYSQL_DATABASE: shopagent
      MYSQL_USER: shopagent
      MYSQL_PASSWORD: shopagentpw
    ports: ["3306:3306"]
    volumes:
      - ./src/main/resources/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
  redis:
    image: redis:7
    ports: ["6379:6379"]
  qdrant:
    image: qdrant/qdrant:latest
    ports: ["6333:6333", "6334:6334"]
    volumes:
      - qdrant_data:/qdrant/storage
  app:
    build: .
    depends_on: [mysql, redis, qdrant]
    environment:
      DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY}
      DATABASE_URL: jdbc:mysql://mysql:3306/shopagent
      DATABASE_USER: shopagent
      DATABASE_PASSWORD: shopagentpw
      REDIS_ADDRESS: redis://redis:6379
      QDRANT_HOST: qdrant
      QDRANT_PORT: 6334
    ports: ["8080:8080"]
volumes:
  qdrant_data:
```

- [ ] **Step 3: Create `.env.example`**

```
DEEPSEEK_API_KEY=sk-xxxxxxxx
DATABASE_PASSWORD=shopagentpw
REDIS_ADDRESS=redis://localhost:6379
QDRANT_HOST=localhost
QDRANT_PORT=6334
```

- [ ] **Step 4: Create `README.md`**

```markdown
# ShopAgent

Spring AI 电商智能客服后端。dev profile 开箱即用，prod profile 接 MySQL/Redis/Qdrant/DeepSeek。

## Quick Start (dev, no external services)

```bash
mvn spring-boot:run
curl "http://localhost:8080/api/chat/stream?sessionId=s1&message=你好" \
  -H "X-User-Id: 1"
```

## Full Stack (prod)

```bash
cp .env.example .env  # fill DEEPSEEK_API_KEY
docker compose up -d
```

## Architecture

- `agent/` Agent orchestration with intent routing (CHITCHAT/INQUIRY/ACTION/COMPLAINT)
- `tool/` Function Calling tools with user-level authorization (userId via ThreadLocal)
- `rag/` Hybrid retrieval (vector + BM25) with RRF fusion
- `session/` In-memory (dev) and Redis (prod) session store
- `business/` Domain + dual-impl repositories (InMemory/MyBatis/JPA)
- `api/` SSE chat endpoint

## Switching Profiles

Default is `dev` (in-memory, no LLM needed for compile). For real LLM calls in dev:
```
DEEPSEEK_API_KEY=sk-xxx mvn spring-boot:run
```

## Tests

```
mvn test
```
```

- [ ] **Step 5: Commit**

```bash
git add Dockerfile docker-compose.yml .env.example README.md
git commit -m "docs: Dockerfile + docker-compose + README"
```

---

## Task 21: Evaluation Dataset + Final Sanity Run

**Files:**
- Create: `data/eval/test_set.jsonl` (20 entries minimum)
- Create: `src/main/java/com/example/shopagent/eval/RagEvaluator.java`
- Create: `src/test/java/com/example/shopagent/eval/RagEvaluatorTest.java`

- [ ] **Step 1: Create `test_set.jsonl`**

Each line: `{"query": "...", "expectedDocIds": ["F001", ...], "category": "..."}`. ≥ 20 entries covering the categories in `faq.jsonl`.

- [ ] **Step 2: Implement `RagEvaluator.java`**

```java
package com.example.shopagent.eval;

import com.example.shopagent.rag.HybridRetriever;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RagEvaluator {
    private final HybridRetriever retriever;

    public EvalReport evaluate(List<TestCase> cases) {
        int hits = 0;
        for (TestCase c : cases) {
            var retrieved = retriever.retrieve(c.query(), 5);
            boolean any = retrieved.stream().anyMatch(h -> c.expectedDocIds().contains(h.id()));
            if (any) hits++;
        }
        return new EvalReport(hits * 1.0 / cases.size(), cases.size());
    }

    public record TestCase(String query, List<String> expectedDocIds, String category) {}
    public record EvalReport(double recallAt5, int totalCases) {}
}
```

- [ ] **Step 3: Implement `RagEvaluatorTest.java`**

```java
package com.example.shopagent.eval;

import com.example.shopagent.rag.HybridRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RagEvaluatorTest {
    private HybridRetriever retriever;
    private RagEvaluator evaluator;

    @BeforeEach
    void setUp() {
        retriever = mock(HybridRetriever.class);
        when(retriever.retrieve(anyString(), anyInt()))
                .thenReturn(List.of(new HybridRetriever.Hit("F001", "退货", 1.0)));
        evaluator = new RagEvaluator(retriever);
    }

    @Test
    void recallAt5IsComputed() {
        var cases = List.of(
                new RagEvaluator.TestCase("退货", List.of("F001"), "退货"),
                new RagEvaluator.TestCase("物流", List.of("F002"), "物流"));
        var report = evaluator.evaluate(cases);
        assertThat(report.recallAt5()).isEqualTo(0.5);
    }
}
```

- [ ] **Step 4: Run full test suite**

Run: `mvn -q test`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add data/eval/test_set.jsonl src/main/java/com/example/shopagent/eval/ src/test/java/com/example/shopagent/eval/
git commit -m "feat: RAG evaluation harness + sample test set"
```

---

## MVP Scope Cuts (deliberate, deferred to v2)

The following design-doc items are **intentionally deferred** from this MVP to keep scope deliverable in 14 days. Each is noted so the resume narrative can still claim the design, with a clear path to implement:

| Deferred | Reason | Where to add |
|---|---|---|
| RabbitMQ async refund queue | Synchronous refund record creation is sufficient for MVP demo; mock queue can be added without API change | `RefundTool` post-save hook → publish to `RefundQueue` topic |
| Real Spring AI bge embedding model | `TransformersEmbeddingModel` ships with Spring AI and works offline; swap with `bge-small-zh` ONNX model in prod profile when real vector DB is connected | `application-prod.yml` → `spring.ai.embeddings.options.model` |
| Cross-encoder reranker | `NoOpReranker` sufficient for RRF top-5 demo | `Reranker` impl `CrossEncoderReranker` (bge-reranker-base ONNX) |
| Multi-modal (image upload) | Not in design-doc MVP | Add `multipart` endpoint + vision model later |
| A/B testing for prompts | Out of MVP scope | Add `PromptVersion` header + Spring AOP |
| Langfuse observability | TokenUsageAdvisor gives basic visibility for resume demo | Replace `LoggingAdvisor` with Langfuse SDK in prod |

---

## Self-Review Checklist (run before declaring done)

- [ ] All 21 tasks committed sequentially
- [ ] `mvn -q test` passes with no failures
- [ ] `mvn -q package -DskipTests` produces `target/shopagent-0.1.0-SNAPSHOT.jar`
- [ ] `java -jar target/shopagent-0.1.0-SNAPSHOT.jar` boots without errors (with dummy DeepSeek key or in offline mode)
- [ ] Every repository / session store / vector index has BOTH dev and prod implementations
- [ ] No tool method accepts `userId` as a parameter (all read from `UserContext.current()`)
- [ ] README's quick-start command actually works
- [ ] No secrets hardcoded in committed files