# Earthquake Dashboard Frontend

React frontend for visualizing earthquake data exposed by the Spring Boot backend.

## Backend contract used

The app calls these endpoints:

- `GET /api/earthquakes` to load stored data
- `POST /api/earthquakes/refresh` to fetch latest USGS data and replace DB data
- `DELETE /api/earthquakes/{usgsId}` to remove a single row

Filters are split by responsibility:

- Server-side (`GET /api/earthquakes` query params): `minMagnitude`, `startTime`, `endTime`
- Client-side (instant table filter): `location`

- `location` performs case-insensitive matching against `place` and `title`.

## Environment variables

- `VITE_API_BASE_URL` (default: `/api`)

Use default value when Vite proxy is configured (recommended), or set full backend URL when running without proxy.

## Run locally

```bash
npm install
npm run dev
```

## Build and lint

```bash
npm run lint
npm run build
```
