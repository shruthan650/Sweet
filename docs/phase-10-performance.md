# Phase 10 performance baseline

Recorded with Java 25.0.2 on Windows 11 using:

```powershell
mvn -Dtest=SimulationEngineBenchmark test
```

The harness runs three samples per workload, with 200 warm-up ticks and 1,000 measured ticks per sample. It reports the median time per tick.

| Workload | Median tick time |
| --- | ---: |
| 100 intersections, 50 vehicles | 0.003 ms |
| 500 intersections, 250 vehicles | 0.002 ms |
| 1,000 intersections, 500 vehicles | 0.009 ms |
| 1,000 intersections, 20 vehicles, 2,000 route nodes each | 0.014 ms |

These are local characterization results, not performance guarantees. The benchmark calls `SimulationEngine` directly; it excludes REST, D1, JavaFX rendering, startup, and concurrent clients. The route workload uses synthetic routes to exercise route advancement. The measurements do not currently show a need to optimize the engine tick path; repeat them on deployment hardware and with representative user cities before making performance claims.

## Local D1 process-restart check

Wrangler 4.149.0 applied the Worker migration to a fresh database under the ignored `target/phase10-d1-state` directory. A synthetic city containing intersections, a traffic light, a road, a building, and a vehicle route was saved through the local Worker. After stopping and starting the Worker again with the same `--persist-to` directory, a GET returned all saved data and the expected relationships. The Worker ran with `--local`; no remote database was accessed. The isolated database files remain under `target/` and are not part of the source changes.
