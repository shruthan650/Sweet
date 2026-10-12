CREATE TABLE cities (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    width INTEGER NOT NULL CHECK (width BETWEEN 1 AND 1000),
    height INTEGER NOT NULL CHECK (height BETWEEN 1 AND 1000),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE intersections (
    city_id TEXT NOT NULL,
    id TEXT NOT NULL,
    name TEXT NOT NULL,
    x INTEGER NOT NULL CHECK (x >= 0),
    y INTEGER NOT NULL CHECK (y >= 0),
    traffic_light_id TEXT,
    traffic_light_state TEXT CHECK (traffic_light_state IN ('RED', 'GREEN', 'YELLOW')),
    traffic_light_cycle_seconds INTEGER CHECK (traffic_light_cycle_seconds > 0),
    PRIMARY KEY (city_id, id),
    UNIQUE (city_id, traffic_light_id),
    FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE
);

CREATE TABLE roads (
    city_id TEXT NOT NULL,
    id TEXT NOT NULL,
    name TEXT NOT NULL,
    start_intersection_id TEXT NOT NULL,
    end_intersection_id TEXT NOT NULL,
    road_type TEXT NOT NULL CHECK (road_type IN ('STREET', 'AVENUE', 'HIGHWAY', 'ALLEY')),
    direction TEXT NOT NULL CHECK (direction IN ('HORIZONTAL', 'VERTICAL', 'DIAGONAL', 'UNKNOWN')),
    bidirectional INTEGER NOT NULL CHECK (bidirectional IN (0, 1)),
    length INTEGER NOT NULL CHECK (length > 0),
    PRIMARY KEY (city_id, id),
    UNIQUE (city_id, start_intersection_id, end_intersection_id),
    FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE,
    FOREIGN KEY (city_id, start_intersection_id) REFERENCES intersections(city_id, id),
    FOREIGN KEY (city_id, end_intersection_id) REFERENCES intersections(city_id, id)
);

CREATE TABLE intersection_road_links (
    city_id TEXT NOT NULL,
    intersection_id TEXT NOT NULL,
    road_id TEXT NOT NULL,
    direction TEXT NOT NULL CHECK (direction IN ('INCOMING', 'OUTGOING')),
    sequence INTEGER NOT NULL CHECK (sequence >= 0),
    PRIMARY KEY (city_id, intersection_id, direction, sequence),
    FOREIGN KEY (city_id, intersection_id) REFERENCES intersections(city_id, id) ON DELETE CASCADE,
    FOREIGN KEY (city_id, road_id) REFERENCES roads(city_id, id) ON DELETE CASCADE
);

CREATE TABLE buildings (
    city_id TEXT NOT NULL,
    id TEXT NOT NULL,
    name TEXT NOT NULL,
    x INTEGER NOT NULL CHECK (x >= 0),
    y INTEGER NOT NULL CHECK (y >= 0),
    width INTEGER NOT NULL CHECK (width > 0),
    height INTEGER NOT NULL CHECK (height > 0),
    building_type TEXT NOT NULL CHECK (building_type IN ('RESIDENTIAL', 'COMMERCIAL', 'INDUSTRIAL', 'PUBLIC')),
    PRIMARY KEY (city_id, id),
    FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE
);

CREATE TABLE vehicles (
    city_id TEXT NOT NULL,
    id TEXT NOT NULL,
    name TEXT NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    speed_kph INTEGER NOT NULL CHECK (speed_kph > 0),
    status TEXT NOT NULL CHECK (status IN ('IDLE', 'MOVING', 'PARKED', 'ARRIVED')),
    current_intersection_id TEXT,
    destination_intersection_id TEXT,
    PRIMARY KEY (city_id, id),
    FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE,
    FOREIGN KEY (city_id, current_intersection_id) REFERENCES intersections(city_id, id),
    FOREIGN KEY (city_id, destination_intersection_id) REFERENCES intersections(city_id, id)
);

CREATE TABLE vehicle_routes (
    city_id TEXT NOT NULL,
    vehicle_id TEXT NOT NULL,
    sequence INTEGER NOT NULL CHECK (sequence >= 0),
    intersection_id TEXT NOT NULL,
    PRIMARY KEY (city_id, vehicle_id, sequence),
    FOREIGN KEY (city_id, vehicle_id) REFERENCES vehicles(city_id, id) ON DELETE CASCADE,
    FOREIGN KEY (city_id, intersection_id) REFERENCES intersections(city_id, id)
);

CREATE TABLE simulation_events (
    city_id TEXT NOT NULL,
    id TEXT NOT NULL,
    tick INTEGER NOT NULL CHECK (tick >= 0),
    event_type TEXT NOT NULL,
    message TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    PRIMARY KEY (city_id, id),
    FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE
);
