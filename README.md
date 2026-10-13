# Sweet

Sweet is a Java 25 / Spring Boot city-simulation project that models roads, intersections, buildings, vehicles, traffic lights, and simulation events for a 2D urban environment.

## Current state

The active application is the Spring Boot backend, Cloudflare D1 Worker, and JavaFX desktop client. The existing React/Vite files are retained in the repository but are outside the current development scope; no web frontend or Vercel deployment work is planned.

### Milestone tracking

- Phase 0: environment and repository setup complete
- Phase 1: domain model complete
- Phase 2: city grid and road-network validation complete
- Phase 3: simulation engine complete
- Phase 4: pathfinding and traffic complete
- Phase 5: Spring Boot REST API complete
- Phase 6: JavaFX desktop application complete
- Phase 7: Cloudflare D1 persistence complete; local Worker/D1 integration verified (remote deployment verification in Phase 11)
- Phase 8: JavaFX desktop integration with persistent simulation complete; city edits are explicitly saved through the backend, and simulation vehicle/traffic-light state is saved after each tick, step, and reset
- Phase 9: Real-time update evaluation complete; the single JavaFX client uses one-second REST ticks and refreshes city state, so WebSockets are deferred until server-driven updates or multiple concurrent clients are required
- Phase 10: Testing and performance — in progress; backend suite has 42 passing tests, desktop suite has 11 passing tests (including JavaFX canvas selection and grid-click interactions), and Worker suite has 4 passing tests plus a passing typecheck; HTTP error contracts, Worker city reload coverage across requests, a local D1 save/restart/reload check, and the local simulation benchmark are recorded
- Phase 11: Backend deployment and desktop distribution — pending

## Requirements

- Java 25
- Maven wrapper (`./mvnw`)
- Node.js 22 or newer for the Cloudflare Worker

## Run tests

```powershell
.\mvnw.cmd test
npm.cmd --prefix worker test
npm.cmd --prefix worker run typecheck
```

## Run the app

```powershell
.\mvnw.cmd spring-boot:run
```

## Run the desktop client

Start the Spring Boot API first. The desktop client uses `http://localhost:8080` by default; set `SWEET_API_BASE_URL` to a different HTTP(S) origin when needed. Create and delete cities from the sidebar; deletion requires confirmation.
Place vehicles on intersections, then select a vehicle and choose a connected destination; the backend computes and saves the route. Deleting roads or intersections clears affected vehicle routes.
Simulation ticks persist vehicle and traffic-light state; Start and Resume advance the simulation automatically once per second, while Step advances exactly one tick when paused. Reset restores the engine's starting vehicle and traffic-light state and persists it. City edits update an active simulation, and deleting a city removes its active simulation.

```powershell
.\mvnw.cmd -f desktop\pom.xml test
.\mvnw.cmd -f desktop\pom.xml javafx:run
```

## Cloudflare D1 persistence

The Spring Boot backend persists city configuration through the Cloudflare Worker; frontend clients must use the Spring Boot API and must not connect to D1 directly. The Worker stores cities, intersections (including traffic-light configuration), roads and their directional links, buildings, vehicles, and vehicle routes in normalized tables. Simulation ticks and event history are currently in-memory runtime state and are not retained in D1.

Configure Spring Boot with `SWEET_WORKER_BASE_URL` and `SWEET_WORKER_SHARED_SECRET`. Configure the Worker secret as `SWEET_API_SECRET` using a Wrangler secret for deployment or `worker/.dev.vars` locally; both sides must use the same secret. Never commit real secrets. The backend requires HTTPS for non-local Worker URLs and uses 5-second connection and 15-second response timeouts.

For local development, start the Worker in its own terminal:

```powershell
Set-Location worker
npm run dev
```

Apply or inspect local migrations from another terminal:

```powershell
Set-Location worker
npx wrangler d1 migrations apply sweet-db --local
npx wrangler d1 migrations list sweet-db --local
```

Before using a deployed database, replace the placeholder `database_id` in `worker/wrangler.toml` with the existing Cloudflare D1 database ID. Then apply remote migrations:

```powershell
Set-Location worker
npx wrangler d1 migrations apply sweet-db --remote
```

Set the actual Worker URL and a valid secret in the backend environment before starting the API. Do not run the remote migration command until the database ID is configured and the target database is confirmed.

## Project structure

- `src/main/java/com/shruthan/sweet` — backend and simulation logic
- `src/test/java` — automated tests
- `docs/` — architecture and design notes
- `desktop/` — JavaFX desktop client
- `web/` — existing React + Vite client, retained but outside the current development scope
- `worker/` — Cloudflare D1 Worker and SQL migrations
