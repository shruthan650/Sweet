# Sweet

Sweet is a Java 25 / Spring Boot city-simulation project that models roads, intersections, buildings, vehicles, traffic lights, and simulation events for a 2D urban environment.

## Current state

This repository currently hosts the backend application for the simulator. The domain model, simulation primitives, and road-network validation are implemented in the `com.shruthan.sweet` package, with unit tests covering validation and state transitions.

### Milestone tracking

- Phase 0: environment and repository setup complete
- Phase 1: domain model complete
- Phase 2: city grid and road-network validation complete
- Phase 3: simulation engine next

## Requirements

- Java 25
- Maven wrapper (`./mvnw`)
- MongoDB running locally on `localhost:27017` for Spring Data integration tests and local development

## Run tests

```powershell
./mvnw test
```

## Run the app

```powershell
./mvnw spring-boot:run
```

## Project structure

- `src/main/java/com/shruthan/sweet` — backend and simulation logic
- `src/test/java` — automated tests
- `docs/` — architecture and design notes
- `desktop/` and `web/` — future frontend applications
