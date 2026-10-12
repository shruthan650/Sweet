# Sweet

Sweet is a Java 25 / Spring Boot city-simulation project that models roads, intersections, buildings, vehicles, traffic lights, and simulation events for a 2D urban environment.

## Current state

The repository contains the Spring Boot backend, Cloudflare D1 Worker, React web client, and a JavaFX desktop client under active development.

### Milestone tracking

- Phase 0: environment and repository setup complete
- Phase 1: domain model complete
- Phase 2: city grid and road-network validation complete
- Phase 3: simulation engine complete
- Phase 4: pathfinding and traffic complete
- Phase 5: Spring Boot REST API complete
- Phase 6: JavaFX desktop application in progress

## Requirements

- Java 25
- Maven wrapper (`./mvnw`)
- Node.js 22 or newer for the Cloudflare Worker

## Run tests

```powershell
.\mvnw.cmd test
```

## Run the app

```powershell
.\mvnw.cmd spring-boot:run
```

## Run the desktop client

Start the Spring Boot API first. The desktop client uses `http://localhost:8080` by default; set `SWEET_API_BASE_URL` to a different HTTP(S) origin when needed.
Place vehicles on intersections, then select a vehicle and choose a connected destination; the backend computes and saves the route.

```powershell
.\mvnw.cmd -f desktop\pom.xml test
.\mvnw.cmd -f desktop\pom.xml javafx:run
```

## Project structure

- `src/main/java/com/shruthan/sweet` — backend and simulation logic
- `src/test/java` — automated tests
- `docs/` — architecture and design notes
- `desktop/` — JavaFX desktop client
- `web/` — React + Vite web client
- `worker/` — Cloudflare D1 Worker and SQL migrations
