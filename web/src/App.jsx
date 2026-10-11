import { useMemo, useState } from 'react';

const initialCities = [
  {
    id: 'city-1',
    name: 'Downtown',
    width: 20,
    height: 20,
    status: 'Running',
    tick: 42,
    roads: 5,
    vehicles: 12,
    intersections: 6
  },
  {
    id: 'city-2',
    name: 'Harbor',
    width: 16,
    height: 16,
    status: 'Paused',
    tick: 15,
    roads: 4,
    vehicles: 7,
    intersections: 5
  }
];

function App() {
  const [cities, setCities] = useState(initialCities);
  const [selectedCityId, setSelectedCityId] = useState(initialCities[0].id);

  const selectedCity = useMemo(
    () => cities.find((city) => city.id === selectedCityId) ?? cities[0],
    [cities, selectedCityId]
  );

  const toggleCityState = (cityId) => {
    setCities((currentCities) =>
      currentCities.map((city) => {
        if (city.id !== cityId) return city;
        const nextStatus = city.status === 'Running' ? 'Paused' : 'Running';
        return {
          ...city,
          status: nextStatus,
          tick: nextStatus === 'Running' ? city.tick + 1 : city.tick
        };
      })
    );
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <span className="brand-mark">S</span>
          <div>
            <h1>Sweet</h1>
            <p>City Simulator</p>
          </div>
        </div>

        <nav className="city-list" aria-label="City list">
          {cities.map((city) => (
            <button
              key={city.id}
              type="button"
              className={selectedCityId === city.id ? 'city-item active' : 'city-item'}
              onClick={() => setSelectedCityId(city.id)}
            >
              <span>{city.name}</span>
              <small>{city.status}</small>
            </button>
          ))}
        </nav>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div>
            <p className="eyebrow">Simulation</p>
            <h2>{selectedCity.name}</h2>
          </div>
          <button type="button" className="primary-button" onClick={() => toggleCityState(selectedCity.id)}>
            {selectedCity.status === 'Running' ? 'Pause' : 'Start'}
          </button>
        </header>

        <section className="stats-grid">
          <div className="stat-card">
            <label>Tick</label>
            <strong>{selectedCity.tick}</strong>
          </div>
          <div className="stat-card">
            <label>Vehicles</label>
            <strong>{selectedCity.vehicles}</strong>
          </div>
          <div className="stat-card">
            <label>Intersections</label>
            <strong>{selectedCity.intersections}</strong>
          </div>
          <div className="stat-card">
            <label>Roads</label>
            <strong>{selectedCity.roads}</strong>
          </div>
        </section>

        <section className="panel-grid">
          <div className="panel">
            <h3>City Overview</h3>
            <ul>
              <li>Grid size: {selectedCity.width} x {selectedCity.height}</li>
              <li>Status: {selectedCity.status}</li>
              <li>Network health: Stable</li>
            </ul>
          </div>

          <div className="panel">
            <h3>Traffic Signals</h3>
            <ul>
              <li>Main Street: Green</li>
              <li>Harbor Avenue: Yellow</li>
              <li>Market Road: Red</li>
            </ul>
          </div>
        </section>
      </main>
    </div>
  );
}

export default App;
