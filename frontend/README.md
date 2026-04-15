# Earthquake Dashboard Frontend

React frontend for visualizing earthquake data exposed by the Spring Boot backend.

## API contract

The frontend uses the following backend endpoints:

- `GET /api/earthquakes` to load stored data
- `POST /api/earthquakes/refresh` to fetch latest USGS data and replace DB data
- `DELETE /api/earthquakes/{usgsId}` to remove a single row

Filters are split by responsibility:

- Server-side (`GET /api/earthquakes` query params): `minMagnitude`, `startTime`, `endTime`
- Client-side (instant table filter): `location` (case-insensitive match against `place` and `title`)

## Environment variables

- `VITE_API_BASE_URL` (default: `/api`)

Use the default when Vite proxy is enabled, or set a full backend URL when running without proxy.

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
