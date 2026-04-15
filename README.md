# Earthquake Dashboard

Full-stack assignment project for fetching, storing, and visualizing recent USGS earthquake data.

## Project structure

- `backend/` Spring Boot + PostgreSQL API
- `frontend/` React (Vite) dashboard UI

## Quick start

1. Configure and start backend (see `backend/README.md`).
2. Start frontend (see `frontend/README.md`).
3. Open the frontend URL and sync data from USGS using the UI button.

## Assignment coverage snapshot

- Fetch USGS GeoJSON data from last-hour feed
- Parse relevant fields (magnitude, place, title, time, location)
- Filter at ingestion by configured magnitude/time threshold
- Replace stored data on refresh to avoid duplicates
- Display records in frontend table with server and client filters
- Optional delete endpoint and UI action

## Verification steps

Use the following sequence to verify the full workflow:

1. **Sync**: click `Sync From USGS` to fetch data from USGS, apply ingest filters, and replace local records.
2. **Filter**: apply `Min magnitude`, `Start time`, and `End time` (server-side), then refine with `Location` (client-side).
3. **Delete (optional)**: remove a record with confirmation to validate `DELETE /api/earthquakes/{usgsId}`.

## Notes

- Backend applies filtering both at ingest time and on `GET /api/earthquakes` query filters (`minMagnitude`, `startTime`, `endTime`).
- Frontend applies an additional interactive `location` filter against `place` and `title`.

