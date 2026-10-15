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
- Phase 7: Cloudflare D1 persistence complete; local and deployed Worker/D1 integration verified in Phase 11
- Phase 8: JavaFX desktop integration with persistent simulation complete; city edits are explicitly saved through the backend, and simulation vehicle/traffic-light state is saved after each tick, step, and reset
- Phase 9: Real-time update evaluation complete; the single JavaFX client uses one-second REST ticks and refreshes city state, so WebSockets are deferred until server-driven updates or multiple concurrent clients are required
- Phase 10: Testing and performance complete; backend suite has 50 passing tests, desktop suite has 12 passing tests (including JavaFX canvas selection and grid-click interactions), and Worker suite has 4 passing tests plus a passing typecheck; coverage includes BFS/Dijkstra, HTTP error contracts, Worker city reloads, local D1 process-restart persistence, SQL migration application, and a recorded local simulation benchmark
- Phase 11: Backend deployment and desktop distribution — in progress; Render and the Cloudflare Worker are deployed, authenticated API access is verified (health `UP`, four cities returned), and Windows/Linux desktop packages were published in release `v0.1.4`; clean-machine desktop launch checks remain

## Requirements

For the Spring Boot backend and JavaFX desktop app:

- Java 25
- Maven wrapper (`./mvnw`)

Node.js 22 or newer is only required when developing, testing, or deploying the Cloudflare Worker (`worker/`). It is not needed to run the Java desktop app with the hosted backend.

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

### Run from VS Code with the deployed backend

Create a local `.env` from `.env.example` and set `SWEET_API_ACCESS_TOKEN` to the same client token configured on Render. In VS Code, open the repository root and press **Ctrl+Shift+B**, then select **Run Sweet Desktop** if prompted. This task runs `desktop/run.ps1`, which loads `SWEET_API_BASE_URL` and `SWEET_API_ACCESS_TOKEN` from `.env` and launches the JavaFX client. If `SWEET_API_BASE_URL` is absent, the launcher defaults to `https://sweet-yqdu.onrender.com`. The `.env` file is ignored by Git; never commit or share its secrets.

You can also launch it from PowerShell at the repository root:

```powershell
$env:SWEET_API_BASE_URL = "https://sweet-yqdu.onrender.com"
$env:SWEET_API_ACCESS_TOKEN = ((Get-Content .env | Where-Object { $_ -match '^SWEET_API_ACCESS_TOKEN=' } | Select-Object -First 1) -split '=', 2)[1].Trim().Trim('"').Trim("'")
.\mvnw.cmd -f desktop\pom.xml javafx:run
```

This reads the token from your local, Git-ignored `.env` file; it does not print or add the token to the repository. You need the API access token configured for the hosted service. Do not paste a real token into a public issue, commit, or README. Alternatively, run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File desktop\run.ps1` to use the same local `.env` settings.

### Run with a local backend

Start the local Spring Boot API first. The desktop client defaults to `http://localhost:8080`; for another HTTP(S) origin, set `SWEET_API_BASE_URL`. Local API access does not require a bearer token unless production security is enabled.

Create and delete cities from the sidebar; deletion requires confirmation.
Place vehicles on intersections, then select a vehicle and choose a connected destination; the backend computes and saves the route. Deleting roads or intersections clears affected vehicle routes.
Simulation ticks persist vehicle and traffic-light state; Start and Resume advance the simulation automatically once per second, while Step advances exactly one tick when paused. Reset restores the engine's starting vehicle and traffic-light state and persists it. City edits update an active simulation, and deleting a city removes its active simulation.

```powershell
.\mvnw.cmd -f desktop\pom.xml test
.\mvnw.cmd -f desktop\pom.xml javafx:run
```

On first launch, the desktop creates a `Sweet Traffic Demo` if the backend has no saved cities. It contains a 4-by-4 intersection grid, 24 connected road segments, 12 buildings, 20 vehicles with multi-trip sample routes, and four traffic lights. Start or step the simulation to watch the vehicles animate between intersections. If the backend has an earlier starter or traffic demo, the app adds this updated demo alongside it without changing the existing city.

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

## Deployment and desktop distribution

See [Phase 11 deployment and distribution](docs/phase-11-deployment.md) for Render setup, Cloudflare production verification, API endpoints, desktop packaging, and clean-environment checks. The backend can be built with the root Dockerfile. Build a native JavaFX app image on the target operating system with `powershell.exe -NoProfile -ExecutionPolicy Bypass -File desktop/package.ps1`; it bundles a Java runtime and is the recommended download for users.

### Download and run the Windows JAR bundle

1. Open the project's [latest GitHub release](https://github.com/shruthan650/Sweet/releases/latest).
2. Under **Assets**, download `sweet-desktop-windows-x64-jar.zip`. This ZIP includes the app JAR, its required libraries, and a launcher; do not download the source-code ZIP files.
3. Extract the ZIP to a folder on your Windows x64 computer. Install Java 25 if it is not already installed.
4. Copy `env.example` to `.env` in the extracted folder. Set `SWEET_API_ACCESS_TOKEN` in `.env` to an access token issued for the hosted API. The app connects to the hosted API over the internet; the release does not include a token.
5. In that folder, run:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run.ps1
   ```

When the desktop opens, it loads the sample city if the hosted service has no saved cities. Press **Start** to watch traffic move. The `.env` file contains your private credential: never commit it or share it. If you do not have an API access token, contact the project maintainer; do not expect the public release to contain one.

To build the same JAR bundle yourself, use Java 25 and PowerShell from a local checkout:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File desktop/package-jar.ps1
```

This creates `desktop/target/dist/sweet-desktop-windows-x64-jar.zip`. The JAR bundle is for Windows x64 and requires Java 25 on the user's computer. Version-tagged releases also include Windows and Linux native app images, which bundle their Java runtime. Branch pushes and pull requests run backend/Worker checks and build the backend image.

## Project structure

- `src/main/java/com/shruthan/sweet` — backend and simulation logic
- `src/test/java` — automated tests
- `docs/` — architecture and design notes
- `desktop/` — JavaFX desktop client
- `worker/` — Cloudflare D1 Worker and SQL migrations
