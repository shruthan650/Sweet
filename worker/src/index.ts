interface Env {
  DB: D1Database;
  SWEET_API_SECRET: string;
}

async function readJsonBody(request: Request): Promise<unknown> {
  const contentLength = Number(request.headers.get("Content-Length") ?? 0);
  if (Number.isFinite(contentLength) && contentLength > MAX_BODY_BYTES) {
    throw new RequestError(413, "Request body is too large");
  }
  if (!request.body) throw new RequestError(400, "Request body is required");
  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let totalBytes = 0;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    totalBytes += value.byteLength;
    if (totalBytes > MAX_BODY_BYTES) {
      await reader.cancel();
      throw new RequestError(413, "Request body is too large");
    }
    chunks.push(value);
  }
  const bytes = new Uint8Array(totalBytes);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }
  const body = new TextDecoder().decode(bytes);
  try {
    return JSON.parse(body) as unknown;
  } catch {
    throw new RequestError(400, "Malformed JSON request body");
  }
}

type JsonObject = Record<string, unknown>;

const ID_PATTERN = /^[A-Za-z0-9][A-Za-z0-9_-]{0,99}$/;
const ROAD_TYPES = new Set(["STREET", "AVENUE", "HIGHWAY", "ALLEY"]);
const DIRECTIONS = new Set(["HORIZONTAL", "VERTICAL", "DIAGONAL", "UNKNOWN"]);
const LIGHT_STATES = new Set(["RED", "GREEN", "YELLOW"]);
const VEHICLE_STATES = new Set(["IDLE", "MOVING", "PARKED", "ARRIVED"]);
const BUILDING_TYPES = new Set(["RESIDENTIAL", "COMMERCIAL", "INDUSTRIAL", "PUBLIC"]);
const MAX_ENTITIES = 250;
const MAX_BATCH_STATEMENTS = 1000;
const MAX_BODY_BYTES = 1_000_000;

class RequestError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
  }
}

function minimalIntersection(id: string, intersections: Map<string, JsonObject>): JsonObject | null {
  const intersection = intersections.get(id);
  if (!intersection) return null;
  return {
    id: intersection.id,
    name: intersection.name,
    x: intersection.x,
    y: intersection.y,
    incomingRoads: [],
    outgoingRoads: [],
    trafficLight: null
  };
}

function json(data: unknown, status = 200): Response {
  return Response.json(data, {
    status,
    headers: { "Cache-Control": "no-store" }
  });
}

function isObject(value: unknown): value is JsonObject {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function requiredText(value: unknown, label: string, maxLength = 120): string {
  if (typeof value !== "string" || value.trim().length === 0 || value.trim().length > maxLength) {
    throw new RequestError(400, `${label} is required and must be at most ${maxLength} characters`);
  }
  return value.trim();
}

function validId(value: unknown, label: string): string {
  const id = requiredText(value, label, 100);
  if (!ID_PATTERN.test(id)) throw new RequestError(400, `${label} has an invalid format`);
  return id;
}

function integer(value: unknown, label: string, min: number, max: number): number {
  if (!Number.isInteger(value) || (value as number) < min || (value as number) > max) {
    throw new RequestError(400, `${label} must be an integer between ${min} and ${max}`);
  }
  return value as number;
}

function enumValue(value: unknown, label: string, allowed: Set<string>): string {
  if (typeof value !== "string" || !allowed.has(value)) {
    throw new RequestError(400, `${label} is invalid`);
  }
  return value;
}

function booleanValue(value: unknown, label: string): boolean {
  if (typeof value !== "boolean") throw new RequestError(400, `${label} must be a boolean`);
  return value;
}

function array(value: unknown, label: string): unknown[] {
  if (value === undefined || value === null) return [];
  if (!Array.isArray(value) || value.length > MAX_ENTITIES) {
    throw new RequestError(400, `${label} must be an array with at most ${MAX_ENTITIES} entries`);
  }
  return value;
}

function relationId(value: unknown, label: string): string {
  if (typeof value === "string") return validId(value, label);
  if (isObject(value)) return validId(value.id, label);
  throw new RequestError(400, `${label} is required`);
}

function optionalRelationId(value: unknown, label: string): string | null {
  return value === null || value === undefined ? null : relationId(value, label);
}

function validateCity(value: unknown, pathId?: string): JsonObject {
  if (!isObject(value)) throw new RequestError(400, "City payload must be a JSON object");
  const id = validId(value.id, "City id");
  if (pathId && id !== pathId) throw new RequestError(400, "City id must match the route id");
  const name = requiredText(value.name, "City name");
  const width = integer(value.width, "City width", 1, 1000);
  const height = integer(value.height, "City height", 1, 1000);
  const intersections = array(value.intersections, "Intersections").map((entry, index) => {
    if (!isObject(entry)) throw new RequestError(400, `Intersection ${index} must be an object`);
    const intersectionId = validId(entry.id, `Intersection ${index} id`);
    const x = integer(entry.x, `Intersection ${intersectionId} x`, 0, width - 1);
    const y = integer(entry.y, `Intersection ${intersectionId} y`, 0, height - 1);
    const trafficLight = entry.trafficLight;
    let light: JsonObject | null = null;
    if (trafficLight !== undefined && trafficLight !== null) {
      if (!isObject(trafficLight)) throw new RequestError(400, `Intersection ${intersectionId} traffic light is invalid`);
      light = {
        id: validId(trafficLight.id, "Traffic light id"),
        state: enumValue(trafficLight.state, "Traffic light state", LIGHT_STATES),
        cycleDurationSeconds: integer(trafficLight.cycleDurationSeconds, "Traffic light cycle duration", 1, 86400)
      };
    }
    return {
      id: intersectionId,
      name: requiredText(entry.name, `Intersection ${intersectionId} name`),
      x,
      y,
      trafficLight: light,
      incomingRoads: array(entry.incomingRoads, "Incoming roads").map((id) => validId(isObject(id) ? id.id : id, "Road id")),
      outgoingRoads: array(entry.outgoingRoads, "Outgoing roads").map((id) => validId(isObject(id) ? id.id : id, "Road id"))
    };
  });
  const intersectionIds = new Set<string>();
  const trafficLightIds = new Set<string>();
  for (const intersection of intersections) {
    if (intersectionIds.has(intersection.id as string)) throw new RequestError(400, "Duplicate intersection id");
    intersectionIds.add(intersection.id as string);
    const light = intersection.trafficLight as JsonObject | null;
    if (light) {
      const lightId = light.id as string;
      if (trafficLightIds.has(lightId)) throw new RequestError(400, "Duplicate traffic light id");
      trafficLightIds.add(lightId);
    }
  }
  const roads = array(value.roads, "Roads").map((entry, index) => {
    if (!isObject(entry)) throw new RequestError(400, `Road ${index} must be an object`);
    const start = relationId(entry.start, "Road start intersection");
    const end = relationId(entry.end, "Road end intersection");
    if (start === end || !intersectionIds.has(start) || !intersectionIds.has(end)) {
      throw new RequestError(400, "Road endpoints must reference two distinct city intersections");
    }
    return {
      id: validId(entry.id, `Road ${index} id`),
      name: requiredText(entry.name, "Road name"),
      start,
      end,
      type: enumValue(entry.type, "Road type", ROAD_TYPES),
      direction: enumValue(entry.direction ?? "UNKNOWN", "Road direction", DIRECTIONS),
      bidirectional: booleanValue(entry.bidirectional, "Road bidirectional"),
      length: integer(entry.length, "Road length", 1, 1_000_000)
    };
  });
  const roadIds = new Set<string>();
  const segments = new Set<string>();
  for (const road of roads) {
    if (roadIds.has(road.id)) throw new RequestError(400, "Duplicate road id");
    roadIds.add(road.id);
    const segment = [road.start, road.end].sort().join(":");
    if (segments.has(segment)) throw new RequestError(400, "Duplicate road segment");
    segments.add(segment);
  }
  const buildings = array(value.buildings, "Buildings").map((entry, index) => {
    if (!isObject(entry)) throw new RequestError(400, `Building ${index} must be an object`);
    const x = integer(entry.x, "Building x", 0, width - 1);
    const y = integer(entry.y, "Building y", 0, height - 1);
    const buildingWidth = integer(entry.width, "Building width", 1, width);
    const buildingHeight = integer(entry.height, "Building height", 1, height);
    if (x + buildingWidth > width || y + buildingHeight > height) {
      throw new RequestError(400, "Building must fit within the city grid");
    }
    return {
      id: validId(entry.id, `Building ${index} id`),
      name: requiredText(entry.name, "Building name"),
      x, y, width: buildingWidth, height: buildingHeight,
      type: enumValue(entry.type, "Building type", BUILDING_TYPES)
    };
  });
  for (let i = 0; i < buildings.length; i++) {
    for (let j = i + 1; j < buildings.length; j++) {
      const a = buildings[i];
      const b = buildings[j];
      if ((a.x as number) < (b.x as number) + (b.width as number)
        && (a.x as number) + (a.width as number) > (b.x as number)
        && (a.y as number) < (b.y as number) + (b.height as number)
        && (a.y as number) + (a.height as number) > (b.y as number)) {
        throw new RequestError(400, "Building placements cannot overlap");
      }
    }
  }
  const vehicles = array(value.vehicles, "Vehicles").map((entry, index) => {
    if (!isObject(entry)) throw new RequestError(400, `Vehicle ${index} must be an object`);
    const x = integer(entry.x, "Vehicle x", 0, width - 1);
    const y = integer(entry.y, "Vehicle y", 0, height - 1);
    const currentIntersection = optionalRelationId(entry.currentIntersection, "Current intersection");
    const destination = optionalRelationId(entry.destination, "Destination intersection");
    const route = array(entry.route, "Vehicle route").map((id) => relationId(id, "Route intersection"));
    if ((currentIntersection && !intersectionIds.has(currentIntersection))
      || (destination && !intersectionIds.has(destination))
      || route.some((id) => !intersectionIds.has(id))) {
      throw new RequestError(400, "Vehicle route and intersections must reference city intersections");
    }
    return {
      id: validId(entry.id, `Vehicle ${index} id`),
      name: requiredText(entry.name, "Vehicle name"),
      x, y,
      speedKph: integer(entry.speedKph, "Vehicle speed", 1, 300),
      status: enumValue(entry.status, "Vehicle status", VEHICLE_STATES),
      currentIntersection,
      destination,
      route
    };
  });
  const vehicleIds = new Set<string>();
  for (const vehicle of vehicles) {
    if (vehicleIds.has(vehicle.id)) throw new RequestError(400, "Duplicate vehicle id");
    vehicleIds.add(vehicle.id);
  }
  const buildingIds = new Set<string>();
  for (const building of buildings) {
    if (buildingIds.has(building.id)) throw new RequestError(400, "Duplicate building id");
    buildingIds.add(building.id);
  }
  const statementCount = 1 + intersections.length + roads.length
    + intersections.reduce((n, intersection) => n
      + (intersection.incomingRoads as unknown[]).length
      + (intersection.outgoingRoads as unknown[]).length, 0)
    + buildings.length + vehicles.length
    + vehicles.reduce((n, vehicle) => n + (vehicle.route as unknown[]).length, 0);
  if (statementCount > MAX_BATCH_STATEMENTS) {
    throw new RequestError(400, "City is too large to save in one atomic operation");
  }
  for (const intersection of intersections) {
    if ([...(intersection.incomingRoads as string[]), ...(intersection.outgoingRoads as string[])]
      .some((roadId) => !roadIds.has(roadId))) {
      throw new RequestError(400, "Intersection road links must reference city roads");
    }
  }
  return { id, name, width, height, roads, intersections, vehicles, buildings };
}

async function authenticated(request: Request, env: Env): Promise<boolean> {
  if (!env.SWEET_API_SECRET || env.SWEET_API_SECRET.length < 32) return false;
  const header = request.headers.get("Authorization");
  if (!header?.startsWith("Bearer ")) return false;
  const encoder = new TextEncoder();
  const expected = new Uint8Array(await crypto.subtle.digest("SHA-256", encoder.encode(env.SWEET_API_SECRET)));
  const supplied = new Uint8Array(await crypto.subtle.digest("SHA-256", encoder.encode(header.slice(7))));
  let difference = 0;
  for (let i = 0; i < expected.length; i++) difference |= expected[i] ^ supplied[i];
  return difference === 0;
}

async function readCity(db: D1Database, id: string): Promise<JsonObject | null> {
  const city = await db.prepare("SELECT id, name, width, height FROM cities WHERE id = ?")
    .bind(id).first<JsonObject>();
  if (!city) return null;
  const [intersectionRows, roadRows, buildingRows, vehicleRows] = await Promise.all([
    db.prepare("SELECT * FROM intersections WHERE city_id = ? ORDER BY id").bind(id).all<JsonObject>(),
    db.prepare("SELECT * FROM roads WHERE city_id = ? ORDER BY id").bind(id).all<JsonObject>(),
    db.prepare("SELECT * FROM buildings WHERE city_id = ? ORDER BY id").bind(id).all<JsonObject>(),
    db.prepare("SELECT * FROM vehicles WHERE city_id = ? ORDER BY id").bind(id).all<JsonObject>()
  ]);
  const [linkRows, routeRows] = await Promise.all([
    db.prepare("SELECT intersection_id, road_id, direction FROM intersection_road_links WHERE city_id = ? ORDER BY sequence")
      .bind(id).all<JsonObject>(),
    db.prepare("SELECT vehicle_id, intersection_id FROM vehicle_routes WHERE city_id = ? ORDER BY vehicle_id, sequence")
      .bind(id).all<JsonObject>()
  ]);
  const intersectionById = new Map<string, JsonObject>();
  const intersections = intersectionRows.results.map((row) => {
    const intersection: JsonObject = {
      id: row.id, name: row.name, x: row.x, y: row.y,
      incomingRoads: [], outgoingRoads: [], trafficLight: null
    };
    if (row.traffic_light_id) {
      intersection.trafficLight = {
        id: row.traffic_light_id,
        state: row.traffic_light_state,
        cycleDurationSeconds: row.traffic_light_cycle_seconds,
        intersection: { id: row.id, name: row.name, x: row.x, y: row.y }
      };
    }
    intersectionById.set(row.id as string, intersection);
    return intersection;
  });
  const roadsById = new Map<string, JsonObject>();
  const roads = roadRows.results.map((row) => {
    const road: JsonObject = {
      id: row.id, name: row.name, type: row.road_type, direction: row.direction,
      bidirectional: row.bidirectional === 1, length: row.length,
      start: minimalIntersection(row.start_intersection_id as string, intersectionById),
      end: minimalIntersection(row.end_intersection_id as string, intersectionById)
    };
    roadsById.set(row.id as string, road);
    return road;
  });
  for (const row of linkRows.results) {
    const intersection = intersectionById.get(row.intersection_id as string);
    const road = roadsById.get(row.road_id as string);
    if (intersection && road) {
      const property = row.direction === "INCOMING" ? "incomingRoads" : "outgoingRoads";
      (intersection[property] as JsonObject[]).push(road);
    }
  }
  const routes = new Map<string, string[]>();
  for (const row of routeRows.results) {
    const vehicleId = row.vehicle_id as string;
    const route = routes.get(vehicleId) ?? [];
    route.push(row.intersection_id as string);
    routes.set(vehicleId, route);
  }
  const vehicles = vehicleRows.results.map((row) => ({
    id: row.id, name: row.name, x: row.x, y: row.y, speedKph: row.speed_kph,
    status: row.status,
    currentIntersection: row.current_intersection_id
      ? intersectionById.get(row.current_intersection_id as string) : null,
    destination: row.destination_intersection_id
      ? intersectionById.get(row.destination_intersection_id as string) : null,
    route: routes.get(row.id as string)?.map((intersectionId) => intersectionById.get(intersectionId)) ?? []
  }));
  const buildings = buildingRows.results.map((row) => ({
    id: row.id, name: row.name, x: row.x, y: row.y, width: row.width, height: row.height,
    type: row.building_type
  }));
  return { ...city, roads, intersections, vehicles, buildings };
}

async function saveCity(db: D1Database, city: JsonObject, insert: boolean): Promise<void> {
  const statements: D1PreparedStatement[] = [];
  const id = city.id as string;
  if (insert) {
    statements.push(db.prepare("INSERT INTO cities (id, name, width, height) VALUES (?, ?, ?, ?)")
      .bind(id, city.name, city.width, city.height));
  } else {
    statements.push(db.prepare("UPDATE cities SET name = ?, width = ?, height = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
      .bind(city.name, city.width, city.height, id));
    statements.push(db.prepare("DELETE FROM vehicles WHERE city_id = ?").bind(id));
    statements.push(db.prepare("DELETE FROM roads WHERE city_id = ?").bind(id));
    statements.push(db.prepare("DELETE FROM buildings WHERE city_id = ?").bind(id));
    statements.push(db.prepare("DELETE FROM intersections WHERE city_id = ?").bind(id));
  }
  for (const intersection of city.intersections as JsonObject[]) {
    const light = intersection.trafficLight as JsonObject | null;
    statements.push(db.prepare(`INSERT INTO intersections
      (city_id, id, name, x, y, traffic_light_id, traffic_light_state, traffic_light_cycle_seconds)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)`)
      .bind(id, intersection.id, intersection.name, intersection.x, intersection.y,
        light?.id ?? null, light?.state ?? null, light?.cycleDurationSeconds ?? null));
  }
  for (const road of city.roads as JsonObject[]) {
    statements.push(db.prepare(`INSERT INTO roads
      (city_id, id, name, start_intersection_id, end_intersection_id, road_type, direction, bidirectional, length)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`)
      .bind(id, road.id, road.name, road.start, road.end, road.type, road.direction,
        road.bidirectional ? 1 : 0, road.length));
  }
  for (const intersection of city.intersections as JsonObject[]) {
    for (const [direction, key] of [["INCOMING", "incomingRoads"], ["OUTGOING", "outgoingRoads"]] as const) {
      (intersection[key] as string[]).forEach((roadId, sequence) => {
        statements.push(db.prepare(`INSERT INTO intersection_road_links
          (city_id, intersection_id, road_id, direction, sequence) VALUES (?, ?, ?, ?, ?)`)
          .bind(id, intersection.id, roadId, direction, sequence));
      });
    }
  }
  for (const building of city.buildings as JsonObject[]) {
    statements.push(db.prepare(`INSERT INTO buildings
      (city_id, id, name, x, y, width, height, building_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`)
      .bind(id, building.id, building.name, building.x, building.y, building.width, building.height, building.type));
  }
  for (const vehicle of city.vehicles as JsonObject[]) {
    statements.push(db.prepare(`INSERT INTO vehicles
      (city_id, id, name, x, y, speed_kph, status, current_intersection_id, destination_intersection_id)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`)
      .bind(id, vehicle.id, vehicle.name, vehicle.x, vehicle.y, vehicle.speedKph,
        vehicle.status, vehicle.currentIntersection, vehicle.destination));
    (vehicle.route as string[]).forEach((intersectionId, sequence) => {
      statements.push(db.prepare("INSERT INTO vehicle_routes (city_id, vehicle_id, sequence, intersection_id) VALUES (?, ?, ?, ?)")
        .bind(id, vehicle.id, sequence, intersectionId));
    });
  }
  if (statements.length > MAX_BATCH_STATEMENTS) throw new RequestError(400, "City is too large to save atomically");
  await db.batch(statements);
}

async function handle(request: Request, env: Env): Promise<Response> {
  if (!(await authenticated(request, env))) return json({ error: "Unauthorized" }, 401);
  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, "") || "/";
  if (path === "/health" && request.method === "GET") return json({ status: "ok" });
  if (path === "/v1/cities" && request.method === "GET") {
    const result = await env.DB.prepare("SELECT id FROM cities ORDER BY id").all<{ id: string }>();
    const cities = await Promise.all(result.results.map((row) => readCity(env.DB, row.id)));
    return json(cities.filter((city): city is JsonObject => city !== null));
  }
  if (path === "/v1/cities" && request.method === "POST") {
    const city = validateCity(await readJsonBody(request));
    const exists = await env.DB.prepare("SELECT id FROM cities WHERE id = ?").bind(city.id).first();
    if (exists) throw new RequestError(409, "City already exists");
    await saveCity(env.DB, city, true);
    return json(await readCity(env.DB, city.id as string), 201);
  }
  const cityPath = path.match(/^\/v1\/cities\/([^/]+)$/);
  if (cityPath) {
    let id: string;
    try {
      id = validId(decodeURIComponent(cityPath[1]), "City id");
    } catch (error) {
      if (error instanceof RequestError) throw error;
      throw new RequestError(400, "City id has an invalid format");
    }
    if (request.method === "GET") {
      const city = await readCity(env.DB, id);
      return city ? json(city) : json({ error: "City not found" }, 404);
    }
    if (request.method === "PUT") {
      const city = validateCity(await readJsonBody(request), id);
      const exists = await env.DB.prepare("SELECT id FROM cities WHERE id = ?").bind(id).first();
      if (!exists) throw new RequestError(404, "City not found");
      await saveCity(env.DB, city, false);
      return json(await readCity(env.DB, id));
    }
    if (request.method === "DELETE") {
      await env.DB.prepare("DELETE FROM cities WHERE id = ?").bind(id).run();
      return new Response(null, { status: 204 });
    }
  }
  return json({ error: "Not found" }, 404);
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      return await handle(request, env);
    } catch (error) {
      if (error instanceof RequestError) return json({ error: error.message }, error.status);
      console.error("D1 request failed", error);
      return json({ error: "Persistent storage request failed" }, 500);
    }
  }
};
