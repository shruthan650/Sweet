async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers
    }
  });

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      message = body.error ?? message;
    } catch {
      // Keep the HTTP status message when the response isn't JSON.
    }
    throw new Error(message);
  }

  return response.status === 204 ? null : response.json();
}

export const cityApi = {
  list: () => request('/api/cities'),
  create: (city) => request('/api/cities', { method: 'POST', body: JSON.stringify(city) }),
  update: (city) => request(`/api/cities/${encodeURIComponent(city.id)}`, {
    method: 'PUT',
    body: JSON.stringify(city)
  }),
  start: (cityId) => request(`/api/simulation/cities/${encodeURIComponent(cityId)}/start`, { method: 'POST' }),
  pause: (cityId) => request(`/api/simulation/cities/${encodeURIComponent(cityId)}/pause`, { method: 'POST' }),
  tick: (cityId) => request(`/api/simulation/cities/${encodeURIComponent(cityId)}/tick`, { method: 'POST' }),
  reset: (cityId) => request(`/api/simulation/cities/${encodeURIComponent(cityId)}/reset`, { method: 'POST' })
};
