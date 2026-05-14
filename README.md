# Earthquake Dashboard

Full-stack assignment project for fetching, storing, and visualizing recent USGS earthquake data.

## Project structure

- `backend/` Spring Boot + MongoDB API
- `frontend/` React (Vite) dashboard UI

## Quick start

1. Configure and start backend (see `backend/README.md`).
2. Start frontend (see `frontend/README.md`).
3. Open the frontend URL and sync data from USGS using the UI button.

## Docker quick start

This project now includes:

- `backend/Dockerfile` for the Spring Boot API
- `frontend/Dockerfile` for the React/Vite UI
- official `mongo:7` image for MongoDB

Example run flow:

```bash
docker network create earthquake-net
docker run --name earthquake-mongo --network earthquake-net -p 27017:27017 -d mongo:7

docker build -t earthquake-backend ./backend
docker run --name earthquake-backend --network earthquake-net -p 8080:8080 -e MONGODB_URI=mongodb://earthquake-mongo:27017/earthquake_dashboard -d earthquake-backend

docker build -t earthquake-frontend ./frontend
docker run --name earthquake-frontend --network earthquake-net -p 5173:80 -d earthquake-frontend
```

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

