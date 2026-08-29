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