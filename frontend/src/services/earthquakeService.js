const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '')

function toIso(value) {
  if (!value) {
    return ''
  }

  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) {
    const parsed = new Date(value)
    if (Number.isFinite(parsed.getTime())) {
      return parsed.toISOString()
    }
  }

  return value
}

function buildEarthquakeQuery(filters = {}) {
  const params = new URLSearchParams()

  if (filters.minMagnitude !== '' && filters.minMagnitude !== undefined && filters.minMagnitude !== null) {
    params.set('minMagnitude', String(filters.minMagnitude))
  }

  const startTime = toIso(filters.startTime)
  if (startTime) {
    params.set('startTime', startTime)
  }

  const endTime = toIso(filters.endTime)
  if (endTime) {
    params.set('endTime', endTime)
  }

  const query = params.toString()
  return query ? `/earthquakes?${query}` : '/earthquakes'
}

function normalizeEarthquake(item) {
  return {
	id: item?.id ?? null,
	usgsId: item?.usgsId ?? null,
	magnitude: item?.magnitude ?? null,
	magType: item?.magType ?? '',
	place: item?.place ?? 'Unknown location',
	title: item?.title ?? '',
	eventTime: item?.eventTime ?? null,
	latitude: item?.latitude ?? null,
	longitude: item?.longitude ?? null,
	depthKm: item?.depthKm ?? item?.depth ?? null,
  }
}

async function parseError(response) {
  let message = `Request failed with status ${response.status}`

  try {
	const payload = await response.json()
	if (payload?.message) {
	  message = payload.message
	}
  } catch {
	// Keep fallback if response body is not JSON.
  }

  throw new Error(message)
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options)

  if (!response.ok) {
	await parseError(response)
  }

  if (response.status === 204) {
	return null
  }

  return response.json()
}

function toItems(payload) {
  if (!Array.isArray(payload)) {
	return []
  }

  return payload.map(normalizeEarthquake)
}

export async function getEarthquakes(filters = {}, signal) {
  const path = buildEarthquakeQuery(filters)
  const payload = await request(path, { signal })
  return toItems(payload)
}

export async function refreshEarthquakes() {
  const payload = await request('/earthquakes/refresh', { method: 'POST' })
  return toItems(payload)
}

export async function deleteEarthquakeById(usgsId) {
  if (!usgsId || !usgsId.trim()) {
	throw new Error('Cannot delete earthquake without a USGS ID.')
  }

  await request(`/earthquakes/${encodeURIComponent(usgsId)}`, {
	method: 'DELETE',
  })
}

