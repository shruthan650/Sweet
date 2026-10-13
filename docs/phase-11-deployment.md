# Phase 11 deployment and distribution

## Backend on Render

The root `render.yaml` defines a Docker web service. The Docker image defaults to the `production` Spring profile. Render injects `PORT`; Spring Boot binds to it through `server.port=${PORT:8080}`. The image exposes `/actuator/health` as its health check, and Actuator returns status only. Application logs go to standard output for the hosting platform to collect.

Set these service environment variables in Render:

| Variable | Value |
| --- | --- |
| `SWEET_WORKER_BASE_URL` | HTTPS origin of the deployed Cloudflare Worker, without a path or trailing slash |
| `SWEET_WORKER_SHARED_SECRET` | Secret shared with the Worker; use at least 32 characters |
| `SWEET_API_ACCESS_TOKEN` | Client bearer token; use at least 32 characters and keep it separate from the Worker secret |

The Blueprint marks secret values `sync: false`, so they are entered through Render rather than stored in source control. It selects the `production` profile, which requires `SWEET_API_ACCESS_TOKEN` at startup and protects every `/api/**` route. Keep the access token separate from the Worker secret. Never pass secrets as Docker build arguments or commit them to `.env` files.

## Cloudflare D1 and Worker

`worker/wrangler.toml` currently contains a placeholder D1 database ID. Before deploying, replace it with the ID of the existing D1 database and verify that the target is correct. Keep the ID in the deployment configuration, not application secrets.

From `worker/`, inspect migration status and the target before applying anything remotely:

```powershell
npm ci
npx wrangler d1 migrations list sweet-db --remote
npx wrangler d1 migrations apply sweet-db --remote
npx wrangler secret put SWEET_API_SECRET
npx wrangler deploy
```

Enter a newly generated secret at the `wrangler secret put` prompt; do not put it in the command line, this document, or source control. Use the same value for Render's `SWEET_WORKER_SHARED_SECRET`. The remote migration command changes the configured production D1 database, so only run it after confirming the database ID and reviewing the pending migrations.

After deploying, set `SWEET_WORKER_BASE_URL` and `SWEET_WORKER_SHARED_SECRET` in a secure local environment, then verify the Worker and backend-to-Worker access without printing credentials:

```powershell
Invoke-RestMethod "$env:SWEET_WORKER_BASE_URL/v1/cities" -Headers @{ Authorization = "Bearer $env:SWEET_WORKER_SHARED_SECRET" }
```

The Worker request returns the persisted city list. For the Spring Boot service, verify `/actuator/health`, then `GET /api/cities` with `Authorization: Bearer $env:SWEET_API_ACCESS_TOKEN`; the latter exercises the backend-to-Worker path. Do not enable Actuator component details publicly.

## Backend API

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/cities` | List cities |
| `POST` | `/api/cities` | Create a city |
| `GET` | `/api/cities/{id}` | Get a city |
| `PUT` | `/api/cities/{id}` | Replace a city |
| `DELETE` | `/api/cities/{id}` | Delete a city |
| `POST` | `/api/simulation/cities/{cityId}/start` | Start simulation |
| `POST` | `/api/simulation/cities/{cityId}/tick` | Advance a running simulation |
| `POST` | `/api/simulation/cities/{cityId}/step` | Advance one tick |
| `POST` | `/api/simulation/cities/{cityId}/pause` | Pause simulation |
| `POST` | `/api/simulation/cities/{cityId}/resume` | Resume simulation |
| `POST` | `/api/simulation/cities/{cityId}/reset` | Reset simulation |
| `PUT` | `/api/simulation/cities/{cityId}/vehicles/{vehicleId}/destination` | Assign a vehicle destination |

## JavaFX desktop distribution

Build a native app image on each target OS with JDK 25, Maven, and `jpackage` available on `PATH`:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File desktop/package.ps1
```

The script runs the desktop Maven package, stages runtime dependencies, and creates `desktop/target/dist/Sweet` for the host OS. App images are platform-specific; build on Windows for Windows, macOS for macOS, and Linux for Linux. The generated app image is an ignored build artifact and must not be committed.

Set `SWEET_API_BASE_URL` to the deployed backend origin and `SWEET_API_ACCESS_TOKEN` to the client token before launching the app. The URL defaults to `http://localhost:8080`; local development does not require a token. Do not bake the token into the package. For a clean-machine acceptance check, install/copy the app image on a machine with no JDK, set the backend URL and token, launch Sweet, load the city list, create and edit a test city, and run/step/reset a simulation. Confirm the Render health endpoint and city API remain available during the check.

## Release checklist

- Configure and verify the existing D1 database ID before any remote migration.
- Deploy the Worker with a Wrangler secret and verify its production URL.
- Deploy the Render Blueprint and configure the Worker URL, Worker secret, and API access token in Render.
- Confirm `/actuator/health` and authenticated `GET /api/cities` succeed on the Render URL.
- Build and smoke-test a JavaFX app image on each target operating system.
- Attach platform-specific app images to a release; do not commit generated files or credentials.
