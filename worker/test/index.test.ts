import { describe, expect, it } from "vitest";
import worker from "../src/index";

const secret = "worker-test-secret-0123456789-abcdef";

interface PreparedCall {
  sql: string;
  values: unknown[];
}

function fakeDatabase(failBatch = false) {
  const calls: PreparedCall[] = [];
  const tables = new Map<string, unknown[][]>();
  let city: unknown[] | undefined;
  const db = {
    prepare(sql: string) {
      const call: PreparedCall = { sql, values: [] };
      calls.push(call);
      return {
        sql,
        get values() {
          return call.values;
        },
        bind(...values: unknown[]) {
          call.values = values;
          return this;
        },
        async first() {
          if (sql.includes("FROM cities")) {
            return city
              ? { id: city[0], name: city[1], width: city[2], height: city[3] }
              : null;
          }
          return null;
        },
        async all() {
          if (sql.includes("FROM cities")) {
            return { results: city ? [{ id: city[0] }] : [] };
          }
          if (sql.includes("FROM intersections")) return { results: tables.get("intersections") ?? [] };
          if (sql.includes("FROM roads")) return { results: tables.get("roads") ?? [] };
          if (sql.includes("FROM buildings")) return { results: tables.get("buildings") ?? [] };
          if (sql.includes("FROM vehicles")) return { results: tables.get("vehicles") ?? [] };
          if (sql.includes("FROM intersection_road_links")) return { results: tables.get("links") ?? [] };
          if (sql.includes("FROM vehicle_routes")) return { results: tables.get("routes") ?? [] };
          return { results: [] };
        },
        async run() {
          return { success: true, meta: { changes: 1 } };
        }
      };
    },
    async batch(statements: Array<{ sql: string; values?: unknown[] }>) {
      if (failBatch) throw new Error("D1 unavailable");
      for (const statement of statements) {
        const values = statement.values ?? [];
        if (statement.sql.startsWith("INSERT INTO cities")) {
          city = values;
        } else if (statement.sql.startsWith("INSERT INTO intersections")) {
          tables.set("intersections", [
            ...(tables.get("intersections") ?? []),
            [{
              city_id: values[0], id: values[1], name: values[2], x: values[3], y: values[4],
              traffic_light_id: values[5], traffic_light_state: values[6],
              traffic_light_cycle_seconds: values[7]
            }]
          ].flat());
        } else if (statement.sql.startsWith("INSERT INTO roads")) {
          tables.set("roads", [
            ...(tables.get("roads") ?? []),
            [{
              city_id: values[0], id: values[1], name: values[2],
              start_intersection_id: values[3], end_intersection_id: values[4],
              road_type: values[5], direction: values[6], bidirectional: values[7], length: values[8]
            }]
          ].flat());
        } else if (statement.sql.startsWith("INSERT INTO buildings")) {
          tables.set("buildings", [
            ...(tables.get("buildings") ?? []),
            [{
              city_id: values[0], id: values[1], name: values[2], x: values[3], y: values[4],
              width: values[5], height: values[6], building_type: values[7]
            }]
          ].flat());
        } else if (statement.sql.startsWith("INSERT INTO vehicles")) {
          tables.set("vehicles", [
            ...(tables.get("vehicles") ?? []),
            [{
              city_id: values[0], id: values[1], name: values[2], x: values[3], y: values[4],
              speed_kph: values[5], status: values[6], current_intersection_id: values[7],
              destination_intersection_id: values[8]
            }]
          ].flat());
        } else if (statement.sql.startsWith("INSERT INTO intersection_road_links")) {
          tables.set("links", [
            ...(tables.get("links") ?? []),
            [{
              city_id: values[0], intersection_id: values[1], road_id: values[2],
              direction: values[3], sequence: values[4]
            }]
          ].flat());
        } else if (statement.sql.startsWith("INSERT INTO vehicle_routes")) {
          tables.set("routes", [
            ...(tables.get("routes") ?? []),
            [{ vehicle_id: values[1], sequence: values[2], intersection_id: values[3] }]
          ].flat());
        }
      }
      return [];
    }
  };
  return { db: db as unknown as D1Database, calls };
}

function env(db: D1Database): { DB: D1Database; SWEET_API_SECRET: string } {
  return { DB: db, SWEET_API_SECRET: secret };
}

function request(path: string, method = "GET", body?: unknown, authorization = secret): Request {
  return new Request(`https://sweet-worker.test${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${authorization}`,
      ...(body === undefined ? {} : { "Content-Type": "application/json" })
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) })
  });
}

function cityPayload() {
  return {
    id: "downtown-1",
    name: "Downtown '); DROP TABLE cities; --",
    width: 20,
    height: 20,
    intersections: [
      {
        id: "i1", name: "Main", x: 2, y: 2,
        trafficLight: { id: "light-1", state: "RED", cycleDurationSeconds: 20 },
        incomingRoads: [], outgoingRoads: ["r1"]
      },
      { id: "i2", name: "Cross", x: 8, y: 2, trafficLight: null, incomingRoads: [], outgoingRoads: [] }
    ],
    roads: [{
      id: "r1", name: "Main Street", start: "i1", end: "i2",
      type: "STREET", direction: "HORIZONTAL", bidirectional: true, length: 6
    }],
    buildings: [{
      id: "b1", name: "Library", x: 10, y: 10, width: 3, height: 3, type: "PUBLIC"
    }],
    vehicles: [{
      id: "v1", name: "Taxi", x: 2, y: 2, speedKph: 30, status: "MOVING",
      currentIntersection: "i1", destination: "i2", route: ["i2"]
    }]
  };
}

describe("Cloudflare D1 Worker API", () => {
  it("requires the shared secret before touching D1", async () => {
    const { db, calls } = fakeDatabase();
    const response = await worker.fetch(request("/v1/cities", "GET", undefined, "wrong-secret"), env(db));

    expect(response.status).toBe(401);
    expect(calls).toHaveLength(0);
  });

  it("validates invalid geometry and refuses to write", async () => {
    const { db, calls } = fakeDatabase();
    const invalid = { ...cityPayload(), buildings: [{ ...cityPayload().buildings[0], x: 19 }] };
    const response = await worker.fetch(request("/v1/cities", "POST", invalid), env(db));

    expect(response.status).toBe(400);
    expect(calls.some((call) => call.sql.startsWith("INSERT"))).toBe(false);
  });

  it("saves and reloads a complete city using bound SQL parameters", async () => {
    const { db, calls } = fakeDatabase();
    const response = await worker.fetch(request("/v1/cities", "POST", cityPayload()), env(db));
    const saved = await response.json() as ReturnType<typeof cityPayload>;

    expect(response.status).toBe(201);
    expect(saved.name).toBe(cityPayload().name);
    expect(saved.roads).toHaveLength(1);
    expect(saved.intersections[0].trafficLight?.state).toBe("RED");
    expect(saved.intersections[0].outgoingRoads).toHaveLength(1);
    expect(saved.vehicles[0].route).toHaveLength(1);
    expect(saved.buildings).toHaveLength(1);
    expect(calls.some((call) => call.sql.includes(cityPayload().name))).toBe(false);
    expect(calls.some((call) => call.sql.includes("VALUES (?, ?, ?, ?)") && call.values.includes(cityPayload().name))).toBe(true);
  });

  it("surfaces D1 write failures as storage errors", async () => {
    const { db } = fakeDatabase(true);
    const response = await worker.fetch(request("/v1/cities", "POST", cityPayload()), env(db));

    expect(response.status).toBe(500);
    expect(await response.json()).toEqual({ error: "Persistent storage request failed" });
  });
});
