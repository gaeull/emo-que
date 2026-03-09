# Emoque – AI-Based Personalized Emoticon Service

AI-powered web app that blends survey input, ChatGPT conversation history, and curated emotion prompts to generate export-ready emoticon packs plus a one-line bio.

## Project Structure

```
Test_Emo-que/
├── backend/          # Spring Boot, RabbitMQ, Redis integration, OpenAI/DALL·E stubs
└── frontend/         # React + Vite single-page app for survey → loading → result flow
```

## Backend (Spring Boot)

- `SurveyController`: captures profile input and stores it in-memory (swap with DB later).
- `ChatController`: placeholder for importing ChatGPT threads through OpenAI API.
- `GenerationController`: queues emotion generation tasks, exposes polling endpoint.
- `GenerationService`: orchestrates prompts, talks to stubbed OpenAI + image clients, records status in Redis, and emits work into RabbitMQ.
- `TaskWorker`: Rabbit consumer that processes queued tasks and persists results.
- `OpenAiClient` / `ImageGenerationClient`: centralized spots to integrate real APIs.

### Run Backend

```bash
cd backend
./gradlew bootRun
```

> Requires local RabbitMQ (`localhost:5672`) and Redis (`localhost:6379`). Update `application.yml` for external services and secrets.

#### Run without Gradle (recommended in restricted environments)

If `./gradlew bootRun` fails due to daemon socket restrictions, run the app via the bootable jar:

1) Build the jar

```
bash backend/scripts/build_jar.sh
```

2) Run the jar

```
bash backend/scripts/run_jar.sh
```

Optional:

```
# run worker message consumer only when RabbitMQ is available
SPRING_PROFILES_ACTIVE=worker bash backend/scripts/run_jar.sh

# tune JVM
JAVA_OPTS="-Xms256m -Xmx512m" bash backend/scripts/run_jar.sh
```

## Frontend (React + Vite)

- Hero → Survey → Emotion selection → Loading → Result UI sequence.
- Axios client proxies to `/api/*` (configured in `vite.config.ts`).
- Polls generation status every 3 seconds until completion or failure.

### Run Frontend

```bash
# http://localhost:5173/
cd frontend
npm install
npm run dev
```

## Environment & Secrets

| Service  | Default | Notes |
|----------|---------|-------|
| OpenAI   | `openai.apiKey` in `backend/src/main/resources/application.yml` | Inject real key via env var or secret manager. |
| MySQL    | `jdbc:mysql://localhost:3306/emoque` | Update username/password in `application.yml`; schema auto-creates via JPA. |
| RabbitMQ | `localhost:5672` | Queue name: `emoque.tasks`. |
| Redis    | `localhost:6379` | Stores transient task status. |
