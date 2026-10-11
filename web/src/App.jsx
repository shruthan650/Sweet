import { useCallback, useEffect, useMemo, useState } from 'react';
import CityCanvas from './CityCanvas';
import { cityApi } from './api';

const TOOLS = [
  { id: 'select', label: 'Select', icon: '↖' },
  { id: 'intersection', label: 'Intersection', icon: '⊙' },
  { id: 'road', label: 'Road', icon: '╱' },
  { id: 'building', label: 'Building', icon: '▦' },
  { id: 'vehicle', label: 'Vehicle', icon: '▰' },
  { id: 'erase', label: 'Erase', icon: '⌫' }
];

function makeId(prefix) {
  return `${prefix}-${crypto.randomUUID().slice(0, 8)}`;
}

function blankCity() {
  return {
    id: makeId('city'),
    name: 'New City',
    width: 24,
    height: 16,
    roads: [],
    intersections: [],
    vehicles: [],
    buildings: []
  };
}

function entityBySelection(city, selection) {
  if (!selection) return null;
  return city[`${selection.kind}s`]?.find((entity) => entity.id === selection.id) ?? null;
}

function createRoad(city, start, end) {
  const direction = start.x === end.x
    ? 'VERTICAL'
    : start.y === end.y
      ? 'HORIZONTAL'
      : 'DIAGONAL';
  return {
    id: makeId('road'),
    name: `Road ${city.roads.length + 1}`,
    start,
    end,
    type: 'STREET',
    direction,
    bidirectional: true,
    length: Math.max(Math.abs(start.x - end.x), Math.abs(start.y - end.y))
  };
}

function App() {
  const [city, setCity] = useState(blankCity);
  const [cities, setCities] = useState([]);
  const [isPersisted, setIsPersisted] = useState(false);
  const [tool, setTool] = useState('intersection');
  const [selection, setSelection] = useState(null);
  const [roadStart, setRoadStart] = useState(null);
  const [buildingType, setBuildingType] = useState('RESIDENTIAL');
  const [apiMessage, setApiMessage] = useState('Connecting to the Sweet API…');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [simulation, setSimulation] = useState({ running: false, tick: 0 });

  useEffect(() => {
    let active = true;
    cityApi.list()
      .then((loadedCities) => {
        if (!active) return;
        setCities(loadedCities);
        if (loadedCities.length > 0) {
          setCity(loadedCities[0]);
          setIsPersisted(true);
          setApiMessage('Connected to the Sweet API');
        } else {
          setApiMessage('Connected · no saved cities yet');
        }
      })
      .catch((error) => {
        if (active) setApiMessage(`API unavailable · ${error.message}`);
      });
    return () => { active = false; };
  }, []);

  const selectedEntity = useMemo(() => entityBySelection(city, selection), [city, selection]);
  const activeCityCount = cities.length + (isPersisted || city.intersections.length === 0 ? 0 : 1);

  const updateCity = useCallback((update) => {
    setCity((current) => ({ ...current, ...update }));
  }, []);

  const handleCellClick = (x, y) => {
    setSelection(null);
    if (tool === 'select') return;
    if (tool === 'intersection') {
      if (city.intersections.some((item) => item.x === x && item.y === y)) {
        setNotice('There is already an intersection in that grid cell.');
        return;
      }
      const intersection = {
        id: makeId('intersection'),
        name: `Intersection ${city.intersections.length + 1}`,
        x,
        y,
        incomingRoads: [],
        outgoingRoads: [],
        trafficLight: null
      };
      updateCity({ intersections: [...city.intersections, intersection] });
      setNotice(`${intersection.name} placed at ${x}, ${y}.`);
      return;
    }
    if (tool === 'road') {
      const intersection = city.intersections.find((item) => item.x === x && item.y === y);
      if (!intersection) {
        setNotice('Roads connect intersections. Place intersections first, then click two of them.');
        return;
      }
      if (!roadStart) {
        setRoadStart(intersection);
        setNotice(`Road start: ${intersection.name}. Select its destination intersection.`);
        return;
      }
      if (roadStart.id === intersection.id) {
        setRoadStart(null);
        setNotice('Road drawing cancelled.');
        return;
      }
      if (city.roads.some((road) =>
        (road.start.id === roadStart.id && road.end.id === intersection.id)
        || (road.start.id === intersection.id && road.end.id === roadStart.id))) {
        setRoadStart(null);
        setNotice('A road already connects those intersections.');
        return;
      }
      updateCity({ roads: [...city.roads, createRoad(city, roadStart, intersection)] });
      setRoadStart(null);
      setNotice(`Road connected ${roadStart.name} to ${intersection.name}.`);
      return;
    }
    if (tool === 'building') {
      const building = {
        id: makeId('building'),
        name: `${buildingType[0]}${buildingType.slice(1).toLowerCase()} ${city.buildings.length + 1}`,
        x, y,
        width: Math.min(2, city.width - x),
        height: Math.min(2, city.height - y),
        type: buildingType
      };
      const overlaps = city.buildings.some((existing) =>
        building.x < existing.x + existing.width
        && building.x + building.width > existing.x
        && building.y < existing.y + existing.height
        && building.y + building.height > existing.y);
      if (overlaps) {
        setNotice('Buildings cannot overlap. Choose another grid area.');
        return;
      }
      updateCity({ buildings: [...city.buildings, building] });
      setNotice(`${building.name} placed at ${x}, ${y}.`);
      return;
    }
    if (tool === 'vehicle') {
      const currentIntersection = city.intersections.find((item) => item.x === x && item.y === y) ?? null;
      const vehicle = {
        id: makeId('vehicle'),
        name: `Vehicle ${city.vehicles.length + 1}`,
        x, y,
        speedKph: 30,
        status: 'IDLE',
        currentIntersection,
        destination: null,
        route: []
      };
      updateCity({ vehicles: [...city.vehicles, vehicle] });
      setNotice(`${vehicle.name} placed at ${x}, ${y}.`);
      return;
    }
    if (tool === 'erase') {
      const building = city.buildings.find((item) =>
        x >= item.x && x < item.x + item.width && y >= item.y && y < item.y + item.height);
      const vehicle = city.vehicles.find((item) => item.x === x && item.y === y);
      const intersection = city.intersections.find((item) => item.x === x && item.y === y);
      if (building) {
        updateCity({ buildings: city.buildings.filter((item) => item.id !== building.id) });
        setNotice(`${building.name} removed.`);
      } else if (vehicle) {
        updateCity({ vehicles: city.vehicles.filter((item) => item.id !== vehicle.id) });
        setNotice(`${vehicle.name} removed.`);
      } else if (intersection) {
        updateCity({
          intersections: city.intersections.filter((item) => item.id !== intersection.id),
          roads: city.roads.filter((road) => road.start.id !== intersection.id && road.end.id !== intersection.id),
          vehicles: city.vehicles.map((item) => ({
            ...item,
            currentIntersection: item.currentIntersection?.id === intersection.id ? null : item.currentIntersection,
            destination: item.destination?.id === intersection.id ? null : item.destination,
            route: item.route.filter((stop) => stop.id !== intersection.id)
          }))
        });
        setNotice(`${intersection.name} and its connected roads removed.`);
      } else {
        setNotice('Nothing to erase in that cell.');
      }
    }
  };

  const handleToolChange = (nextTool) => {
    setTool(nextTool);
    setRoadStart(null);
    setNotice(nextTool === 'road' ? 'Select two intersections to connect with a road.' : '');
  };

  const selectCity = (selected) => {
    setCity(selected);
    setIsPersisted(true);
    setSelection(null);
    setRoadStart(null);
    setSimulation({ running: false, tick: 0 });
    setNotice('');
  };

  const saveCity = async () => {
    setBusy(true);
    setNotice('');
    try {
      const saved = isPersisted ? await cityApi.update(city) : await cityApi.create(city);
      setCity(saved);
      setIsPersisted(true);
      setCities((current) => {
        const found = current.some((item) => item.id === saved.id);
        return found
          ? current.map((item) => item.id === saved.id ? saved : item)
          : [...current, saved];
      });
      setNotice('City saved successfully.');
      setApiMessage('Connected to the Sweet API');
    } catch (error) {
      setNotice(`Save failed · ${error.message}`);
    } finally {
      setBusy(false);
    }
  };

  const runSimulationAction = async (action) => {
    setBusy(true);
    setNotice('');
    try {
      let response;
      if (action === 'tick' && !simulation.running) {
        await cityApi.start(city.id);
        response = await cityApi.tick(city.id);
        await cityApi.pause(city.id);
        response.running = false;
      } else {
        response = await cityApi[action](city.id);
      }
      setSimulation({ running: response.running, tick: response.tick });
      if (action === 'tick') {
        const latest = await cityApi.list();
        setCities(latest);
        const refreshed = latest.find((item) => item.id === city.id);
        if (refreshed) setCity(refreshed);
      }
    } catch (error) {
      setNotice(`Simulation request failed · ${error.message}`);
    } finally {
      setBusy(false);
    }
  };

  const removeSelected = () => {
    removeSelection(selection);
  };

  const removeSelection = (target) => {
    if (!target) return;
    const { id, kind } = target;
    if (kind === 'intersection') {
      updateCity({
        intersections: city.intersections.filter((item) => item.id !== id),
        roads: city.roads.filter((road) => road.start.id !== id && road.end.id !== id),
        vehicles: city.vehicles.map((item) => ({
          ...item,
          currentIntersection: item.currentIntersection?.id === id ? null : item.currentIntersection,
          destination: item.destination?.id === id ? null : item.destination,
          route: item.route.filter((stop) => stop.id !== id)
        }))
      });
    } else {
      const list = `${kind}s`;
      updateCity({ [list]: city[list].filter((item) => item.id !== id) });
    }
    setSelection(null);
    setNotice('Selected item removed.');
  };

  const handleObjectClick = (target) => {
    const entity = city[`${target.kind}s`]?.find((item) => item.id === target.id);
    if (tool === 'road' && target.kind === 'intersection' && entity) {
      handleCellClick(entity.x, entity.y);
      return;
    }
    if (tool === 'erase') {
      removeSelection(target);
      return;
    }
    setSelection(target);
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <span className="brand-mark">S</span>
          <div>
            <h1>Sweet</h1>
            <p>City workshop</p>
          </div>
        </div>

        <div className="side-section-heading">
          <span>YOUR CITIES</span>
          <button className="icon-button" type="button" aria-label="Create a new city" title="New city"
            onClick={() => {
              setCity(blankCity());
              setIsPersisted(false);
              setSelection(null);
              setSimulation({ running: false, tick: 0 });
              setNotice('New city draft created. Edit the grid, then save it.');
            }}>+</button>
        </div>
        <nav className="city-list" aria-label="Saved cities">
          {cities.map((item) => (
            <button key={item.id} type="button"
              className={city.id === item.id ? 'city-item active' : 'city-item'}
              onClick={() => selectCity(item)}>
              <span>{item.name}</span>
              <small>{item.width} × {item.height}</small>
            </button>
          ))}
          {cities.length === 0 && <p className="empty-list">{apiMessage.startsWith('API unavailable') ? 'API is not connected' : 'No saved cities'}</p>}
        </nav>
        <div className="sidebar-footer">
          <span className={apiMessage.startsWith('Connected') ? 'connection-dot online' : 'connection-dot'} />
          <span>{apiMessage}</span>
        </div>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div className="page-heading">
            <p className="eyebrow">2D CITY MODELER</p>
            <div className="title-row">
              <input aria-label="City name" className="city-name-input" value={city.name}
                maxLength={120} onChange={(event) => updateCity({ name: event.target.value })}
                onBlur={(event) => {
                  if (!event.target.value.trim()) updateCity({ name: 'Untitled City' });
                }} />
              <span className={isPersisted ? 'saved-badge' : 'draft-badge'}>
                {isPersisted ? 'SAVED' : 'DRAFT'}
              </span>
            </div>
          </div>
          <div className="top-actions">
            <button className="secondary-button" type="button" disabled={busy || !isPersisted}
              onClick={() => runSimulationAction(simulation.running ? 'pause' : 'start')}>
              {simulation.running ? 'Pause' : 'Start simulation'}
            </button>
            <button className="primary-button" type="button" disabled={busy} onClick={saveCity}>
              {busy ? 'Working…' : 'Save city'}
            </button>
          </div>
        </header>

        <section className="stats-grid" aria-label="City summary">
          <div className="stat-card"><span>GRID</span><strong>{city.width} × {city.height}</strong></div>
          <div className="stat-card"><span>ROADS</span><strong>{city.roads.length.toString().padStart(2, '0')}</strong></div>
          <div className="stat-card"><span>INTERSECTIONS</span><strong>{city.intersections.length.toString().padStart(2, '0')}</strong></div>
          <div className="stat-card"><span>BUILDINGS</span><strong>{city.buildings.length.toString().padStart(2, '0')}</strong></div>
          <div className="stat-card"><span>VEHICLES</span><strong>{city.vehicles.length.toString().padStart(2, '0')}</strong></div>
        </section>

        <section className="workspace">
          <div className="editor-panel">
            <div className="canvas-toolbar">
              <div className="tool-list" role="toolbar" aria-label="City editing tools">
                {TOOLS.map((item) => (
                  <button key={item.id} type="button" title={item.label} aria-label={item.label}
                    aria-pressed={tool === item.id}
                    className={tool === item.id ? 'tool-button active' : 'tool-button'}
                    onClick={() => handleToolChange(item.id)}>
                    <span aria-hidden="true">{item.icon}</span>
                    <small>{item.label}</small>
                  </button>
                ))}
              </div>
              <div className="grid-size-controls">
                <label>Grid</label>
                <input aria-label="Grid width" type="number" min="5" max="40" value={city.width}
                  onChange={(event) => {
                    const width = Number(event.target.value);
                    if (width >= 5 && width <= 40) updateCity({ width });
                  }} />
                <span>×</span>
                <input aria-label="Grid height" type="number" min="5" max="40" value={city.height}
                  onChange={(event) => {
                    const height = Number(event.target.value);
                    if (height >= 5 && height <= 40) updateCity({ height });
                  }} />
              </div>
            </div>

            <div className="map-instructions">
              <span className="instruction-icon">{tool === 'road' && roadStart ? '2' : 'i'}</span>
              <span>{roadStart ? `Road start selected: ${roadStart.name}. Choose an end intersection.` : `Tool: ${TOOLS.find((item) => item.id === tool)?.label}. Click the grid to edit.`}</span>
              {tool === 'building' && (
                <label className="building-type-picker">
                  Type
                  <select value={buildingType} onChange={(event) => setBuildingType(event.target.value)}>
                    <option value="RESIDENTIAL">Residential</option>
                    <option value="COMMERCIAL">Commercial</option>
                    <option value="INDUSTRIAL">Industrial</option>
                    <option value="PUBLIC">Public</option>
                  </select>
                </label>
              )}
            </div>

            <CityCanvas city={city} tool={tool} selectedId={selection?.id}
              onCellClick={handleCellClick} onSelect={handleObjectClick} />

            <div className="canvas-legend">
              <span><i className="legend-road" /> Road</span>
              <span><i className="legend-intersection" /> Intersection</span>
              <span><i className="legend-building" /> Building</span>
              <span><i className="legend-vehicle" /> Vehicle</span>
            </div>
          </div>

          <aside className="inspector">
            <div className="inspector-section">
              <div className="inspector-title-row">
                <h3>City settings</h3>
                <span className="mini-badge">{activeCityCount} {activeCityCount === 1 ? 'CITY' : 'CITIES'}</span>
              </div>
              <label className="field-label" htmlFor="city-id">City ID</label>
              <input id="city-id" className="text-input" value={city.id} readOnly />
              <p className="field-hint">A unique identifier used by the API.</p>
            </div>

            <div className="inspector-section selected-section">
              <div className="inspector-title-row">
                <h3>{selectedEntity ? `${selection.kind[0].toUpperCase()}${selection.kind.slice(1)}` : 'Selection'}</h3>
                {selectedEntity && <button type="button" className="text-action" onClick={removeSelected}>Remove</button>}
              </div>
              {selectedEntity ? (
                <div className="selection-details">
                  <strong>{selectedEntity.name}</strong>
                  <span>ID · {selectedEntity.id}</span>
                  {selection.kind === 'intersection' && (
                    <>
                      <span>Grid position · {selectedEntity.x}, {selectedEntity.y}</span>
                      <button type="button" className="light-toggle" onClick={() => {
                        const trafficLight = selectedEntity.trafficLight
                          ? null
                          : { id: makeId('light'), state: 'RED', cycleDurationSeconds: 30, intersection: selectedEntity };
                        updateCity({ intersections: city.intersections.map((item) =>
                          item.id === selectedEntity.id ? { ...item, trafficLight } : item) });
                      }}>
                        {selectedEntity.trafficLight ? 'Remove traffic light' : 'Add traffic light'}
                      </button>
                    </>
                  )}
                  {selection.kind === 'road' && <span>{selectedEntity.type} · length {selectedEntity.length}</span>}
                  {selection.kind === 'building' && <span>{selectedEntity.type} · {selectedEntity.width} × {selectedEntity.height}</span>}
                  {selection.kind === 'vehicle' && <span>{selectedEntity.status} · {selectedEntity.speedKph} km/h</span>}
                </div>
              ) : (
                <p className="empty-selection">Select a map object to inspect its properties.</p>
              )}
            </div>

            <div className="inspector-section">
              <div className="inspector-title-row"><h3>Simulation</h3><span className={simulation.running ? 'run-indicator running' : 'run-indicator'} /></div>
              <div className="tick-display">
                <span>Current tick</span>
                <strong>{simulation.tick.toString().padStart(4, '0')}</strong>
              </div>
              <div className="simulation-actions">
                <button type="button" className="secondary-button" disabled={busy || !isPersisted}
                  onClick={() => runSimulationAction('tick')}>Step</button>
                <button type="button" className="quiet-button" disabled={busy || !isPersisted}
                  onClick={() => {
                    runSimulationAction('reset');
                  }}>Reset</button>
              </div>
              {!isPersisted && <p className="field-hint">Save this city before starting its simulation.</p>}
            </div>

            <div className="inspector-section object-list-section">
              <div className="inspector-title-row"><h3>Objects</h3><span className="mini-badge">{city.roads.length + city.intersections.length + city.buildings.length + city.vehicles.length}</span></div>
              <div className="object-list">
                {[
                  ...city.intersections.map((item) => ({ ...item, kind: 'intersection' })),
                  ...city.roads.map((item) => ({ ...item, kind: 'road' })),
                  ...city.buildings.map((item) => ({ ...item, kind: 'building' })),
                  ...city.vehicles.map((item) => ({ ...item, kind: 'vehicle' }))
                ].map((item) => (
                  <button type="button" key={`${item.kind}-${item.id}`}
                    className={selection?.id === item.id ? 'object-row selected' : 'object-row'}
                    onClick={() => setSelection({ kind: item.kind, id: item.id })}>
                    <span className={`object-icon object-${item.kind}`}>{item.kind === 'road' ? '╱' : item.kind === 'vehicle' ? '▰' : item.kind === 'building' ? '▦' : '⊙'}</span>
                    <span>{item.name}</span>
                    <small>{item.kind}</small>
                  </button>
                ))}
                {city.roads.length + city.intersections.length + city.buildings.length + city.vehicles.length === 0
                  && <p className="empty-selection">Your city is empty. Choose a tool and click the grid.</p>}
              </div>
            </div>
          </aside>
        </section>
        {notice && <div className={notice.startsWith('Save failed') || notice.startsWith('Simulation request failed') ? 'toast error' : 'toast'} role="status">{notice}</div>}
      </main>
    </div>
  );
}

export default App;
