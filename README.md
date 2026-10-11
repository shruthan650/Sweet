# Sweet

Sweet is a Java 25 / Spring Boot city-simulation project that models roads, intersections, buildings, vehicles, traffic lights, and simulation events for a 2D urban environment.

## Current state

This repository currently hosts the backend application for the simulator. The domain model, simulation primitives, and road-network validation are implemented in the `com.shruthan.sweet` package, with unit tests covering validation and state transitions.

### Milestone tracking

- Phase 0: environment and repository setup complete
- Phase 1: domain model complete
- Phase 2: city grid and road-network validation complete
- Phase 3: simulation engine complete
- Phase 4: pathfinding and simulation integrity complete
- Phase 5: API and persistence scaffolding complete
- Phase 6: web frontend scaffold complete

## Requirements

- Java 25
- Maven wrapper (`./mvnw`)
- Node.js 22+ and npm for the Cloudflare Worker
- A Cloudflare account with a D1 database for deployed persistence

## Run tests

```powershell
./mvnw test
```

## Run the app

```powershell
./mvnw spring-boot:run
```

## Cloudflare D1 persistence

Spring Boot stays on Render and preserves the `/api/cities` REST API. It calls the `worker/` Cloudflare Worker over HTTPS; only the Worker has a D1 binding. Do not put Cloudflare account/API tokens in Spring, Render's frontend, or browser code. The only shared credential is a dedicated random application secret, stored as `SWEET_API_SECRET` in Worker secrets and `SWEET_WORKER_SHARED_SECRET` in Render environment variables.

### Create and migrate D1

```powershell
cd worker
npm install
npx wrangler login
npx wrangler d1 create sweet-db
```

Copy the generated database ID into `worker/wrangler.toml`, replacing the all-zero placeholder. Then apply versioned migrations:

```powershell
npx wrangler d1 migrations apply sweet-db --remote
```

Schema changes go in a new, incrementally numbered file under `worker/migrations/`; do not edit a migration that has already been applied.

### Configure and deploy the Worker

Generate a unique random secret of at least 32 characters (for example, `openssl rand -hex 32`) and set it as a Cloudflare secret. Use the exact same value for the Render environment variable:

```powershell
npx wrangler secret put SWEET_API_SECRET
npm run deploy
```

Set these Render environment variables on the Spring Boot service:

```text
SWEET_WORKER_URL=https://<worker-name>.<account-subdomain>.workers.dev
SWEET_WORKER_SHARED_SECRET=<same dedicated random value>
```

The Java service requires HTTPS for non-local Worker URLs and refuses persistence calls if the secret is absent or shorter than 32 characters. The Worker checks bearer authorization, validates payloads, and uses prepared D1 statements with bound values for every query. Never use a Cloudflare API token as the shared application secret.

On Render, configure the backend with build command `./mvnw clean package` and start command `java -jar target/sweet-0.0.1-SNAPSHOT.jar`. Set both Worker environment variables in the Render service's Environment settings; do not add them to the frontend build. The web frontend must call the Spring API, never the Worker directly.

Input limits are deliberately bounded: city dimensions are 1–1000, each entity collection is limited to 250 items, the aggregate SQL batch to 1000 statements, and the request body to 1 MB. IDs use letters, numbers, underscores, and hyphens (maximum 100 characters).

### Local Worker development

Apply migrations to the local D1 emulator and start the Worker:

```powershell
cd worker
npx wrangler d1 migrations apply sweet-db --local
npm run dev
```

For local Worker secrets, create `worker/.dev.vars` (this file is git-ignored) containing `SWEET_API_SECRET=<same-random-secret>`. Set `SWEET_WORKER_URL=http://localhost:8787` and the matching `SWEET_WORKER_SHARED_SECRET` in the Spring Boot process environment. `.env.example` shows the expected variable names; never commit a populated environment file.

### Tests

```powershell
./mvnw test
cd worker
npm test
npm run typecheck
```

## Project structure

- `src/main/java/com/shruthan/sweet` — backend and simulation logic
- `src/test/java` — automated tests
- `docs/` — architecture and design notes
- `web/` — React frontend
- `desktop/` — future desktop frontend
