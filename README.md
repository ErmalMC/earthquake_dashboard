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
- Display records in frontend table with local UI filters
- Optional delete endpoint and UI action

## Reviewer flow

Use this path to quickly verify the full assignment workflow:

1. **Sync**: click `Sync From USGS` in the frontend to fetch, filter, and replace local DB data.
2. **Filter**: adjust `Min magnitude`, `Location`, `Start time`, and `End time` to narrow the visible records.
3. **Optional delete**: delete a row (with confirmation) to test `DELETE /api/earthquakes/{usgsId}`.

## Notes

- Backend currently defines server-side filtering at ingest time.
- Frontend applies interactive time/magnitude filters on already stored data.

