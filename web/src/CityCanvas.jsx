const CELL = 34;
const PADDING = CELL / 2;

function pointFor(intersection) {
  return {
    x: PADDING + intersection.x * CELL,
    y: PADDING + intersection.y * CELL
  };
}

function findIntersection(city, id) {
  return city.intersections.find((intersection) => intersection.id === id);
}

export default function CityCanvas({ city, tool, selectedId, onCellClick, onSelect }) {
  const width = Math.max(city.width * CELL, 1);
  const height = Math.max(city.height * CELL, 1);

  return (
    <div className="canvas-scroll">
      <svg
        className={`city-canvas tool-${tool}`}
        viewBox={`0 0 ${width} ${height}`}
        width={width}
        height={height}
        role="img"
        aria-label={`${city.name} city grid, ${city.width} columns by ${city.height} rows`}
        onClick={(event) => {
          const bounds = event.currentTarget.getBoundingClientRect();
          const x = Math.floor(((event.clientX - bounds.left) / bounds.width) * city.width);
          const y = Math.floor(((event.clientY - bounds.top) / bounds.height) * city.height);
          if (x >= 0 && x < city.width && y >= 0 && y < city.height) onCellClick(x, y);
        }}
      >
        <defs>
          <pattern id="grid" width={CELL} height={CELL} patternUnits="userSpaceOnUse">
            <path d={`M ${CELL} 0 L 0 0 0 ${CELL}`} fill="none" stroke="#29384a" strokeWidth="1" />
          </pattern>
        </defs>
        <rect width={width} height={height} fill="#101b29" />
        <rect width={width} height={height} fill="url(#grid)" />

        {city.roads.map((road) => {
          const start = findIntersection(city, road.start?.id);
          const end = findIntersection(city, road.end?.id);
          if (!start || !end) return null;
          const a = pointFor(start);
          const b = pointFor(end);
          return (
            <g key={road.id} className="map-road" onClick={(event) => {
              event.stopPropagation();
              onSelect({ kind: 'road', id: road.id });
            }}>
              <line x1={a.x} y1={a.y} x2={b.x} y2={b.y} className="road-shadow" />
              <line x1={a.x} y1={a.y} x2={b.x} y2={b.y} className="road-line" />
              <line x1={a.x} y1={a.y} x2={b.x} y2={b.y} className="road-center" />
              <title>{road.name} · {road.type}</title>
            </g>
          );
        })}

        {city.buildings.map((building) => (
          <g key={building.id} onClick={(event) => {
            event.stopPropagation();
            onSelect({ kind: 'building', id: building.id });
          }}>
            <rect
              x={building.x * CELL + 4}
              y={building.y * CELL + 4}
              width={Math.max(building.width * CELL - 8, 8)}
              height={Math.max(building.height * CELL - 8, 8)}
              rx="5"
              className={`building building-${building.type.toLowerCase()} ${selectedId === building.id ? 'selected' : ''}`}
            />
            {building.width >= 2 && building.height >= 2 && (
              <text x={(building.x + building.width / 2) * CELL} y={(building.y + building.height / 2) * CELL}
                className="building-label">{building.name}</text>
            )}
            <title>{building.name} · {building.type}</title>
          </g>
        ))}

        {city.intersections.map((intersection) => {
          const point = pointFor(intersection);
          return (
            <g key={intersection.id} onClick={(event) => {
              event.stopPropagation();
              onSelect({ kind: 'intersection', id: intersection.id });
            }}>
              <circle cx={point.x} cy={point.y} r="11" className="intersection-halo" />
              <circle cx={point.x} cy={point.y} r="6" className="intersection-node" />
              {intersection.trafficLight && (
                <circle cx={point.x + 10} cy={point.y - 10} r="4.5"
                  className={`traffic-light light-${intersection.trafficLight.state.toLowerCase()}`} />
              )}
              {selectedId === intersection.id && (
                <circle cx={point.x} cy={point.y} r="14" className="selection-ring" />
              )}
              <title>{intersection.name} ({intersection.x}, {intersection.y})</title>
            </g>
          );
        })}

        {city.vehicles.map((vehicle) => (
          <g key={vehicle.id} transform={`translate(${vehicle.x * CELL + CELL / 2} ${vehicle.y * CELL + CELL / 2})`}
            onClick={(event) => {
              event.stopPropagation();
              onSelect({ kind: 'vehicle', id: vehicle.id });
            }}>
            <rect x="-8" y="-6" width="16" height="12" rx="4" className="vehicle-marker" />
            <circle cx="-4" cy="6" r="2" className="vehicle-wheel" />
            <circle cx="4" cy="6" r="2" className="vehicle-wheel" />
            <title>{vehicle.name} · {vehicle.status}</title>
          </g>
        ))}
      </svg>
    </div>
  );
}
