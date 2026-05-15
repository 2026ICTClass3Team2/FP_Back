# FP_Back — DeadBug Backend

Spring Boot REST API for the DeadBug developer community platform. Owns the database, authentication, real-time chat, search, file uploads, and orchestrates calls to the LangChain LLM service.

## Tech stack

- **Spring Boot 4** on **Java 21**
- **Spring Security 6** with JWT + OAuth2 (Google, GitHub, Kakao)
- **JPA / Hibernate** + **QueryDSL 5** for complex queries
- **MySQL** (AWS RDS in production)
- **Redis** for caching and sessions
- **Elasticsearch** for full-text search
- **Spring WebFlux WebClient** for async calls to the LangChain service
- **Spring WebSocket** for real-time chat and live notifications
- **Thymeleaf** for HTML email templates
- **AWS S3 SDK v2** for file uploads
- **Micrometer + Prometheus** + Loki appender for observability

## Prerequisites

- JDK 21
- MySQL reachable at the URL configured in `application.yml`
- Redis at `localhost:6379`
- Elasticsearch at `localhost:9200`
- Required env vars (see below)

## Quick start

```bash
./gradlew bootRun                  # run on http://localhost:8090/api
./gradlew clean bootJar            # build production JAR
./gradlew compileJava              # compile only
./gradlew classes                  # regenerate QueryDSL Q-classes (after any @Entity change)
```

Tests are currently skipped (`-x test`); no test suite is configured.

## Environment variables

Required at runtime:

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | HMAC secret for signing JWTs |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google OAuth2 |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | GitHub OAuth2 |
| `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` | Kakao OAuth2 |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP credentials (Gmail) |
| `AWS_S3_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY`, `AWS_SECRET_KEY` | S3 uploads |
| `ELASTICSEARCH_HOST` | Defaults to `localhost` if unset |

In production these are injected by Jenkins at deploy time.

## Project structure

```
FP_Back/
  build.gradle               Gradle build, dependency list, QueryDSL config
  src/main/
    java/com/example/demo/
      domain/
        user/                Auth, JWT, OAuth2, roles, suspension
        content/             Posts, tags, bookmarks
        qna/                 QnA, AI difficulty scoring, EventQnaScheduler
        comment/             Comments on posts and QnA
        channel/             Learning channels
        chat/                WebSocket real-time messaging
        notification/        User notifications
        point/               Reward point system (PointTransactionRepository)
        shop/                Point shop (emotes, items)
        report/              Moderation: reports, blocks, hidden content
        admin/               Admin dashboard and moderation tools
        algorithm/           Interest profiling for feed ranking
        mypage/              My-page profile, posts, bookmarks
        chatbot/             Proxy to LangChain chatbot endpoints
        s3/                  S3 file upload management
      global/
        config/              Security, Redis, S3, QueryDSL, WebSocket, Async, WebClient
        elasticsearch/       ES configuration
        exception/           Global exception handler
    resources/
      application.yml        Datasource, OAuth2, mail, S3, Elasticsearch, llm.service-url
      templates/             Thymeleaf email templates (email-auth, email-update, email-password-reset)
```

## Endpoints

REST controllers under `domain/*/controller/` are mounted at `/api/<domain>/...`. Examples:

- `/api/user/...` — signup, login, email verification
- `/api/posts/...` — feed posts, bookmarks, tags
- `/api/qna/...` — questions, answers, difficulty scoring
- `/api/mypage/...` — profile, my posts, bookmarks, blocks, email change
- `/api/chatbot/...` — proxies to LangChain `/api/chatbot/{review,chat}`
- `/api/chat/...` — WebSocket endpoint and history
- `/api/admin/...` — admin/moderation
- `/api/s3/...` — presigned upload URLs

## LangChain integration

Calls the FastAPI LangChain service at `llm.service-url` (default `http://localhost:8001`) via `WebClient` (async/non-blocking):

| Caller (backend service) | LangChain endpoint |
|---|---|
| `LlmTagService` | `POST /api/tags/suggest` |
| `LlmQnaService` | `POST /api/qna/score` |
| `ChatbotService` | `POST /api/chatbot/review`, `POST /api/chatbot/chat` |
| `EventQnaScheduler` (daily cron `0 0 9 * * *`) | `POST /api/event/generate` |
| `EventQnaVerificationService` | `POST /api/event/verify` |

## QueryDSL

Q-classes are generated into `build/generated/querydsl` by the annotation processor. **Run `./gradlew classes` after any `@Entity` change** before relying on the regenerated Q-classes.

Complex queries (feed ranking, admin reports, search filters) use `JPAQueryFactory` rather than raw JPQL.
