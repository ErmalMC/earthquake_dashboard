import { useEffect, useMemo, useState } from 'react'
import './App.css'
import Navbar from './components/Navbar'
import EarthquakeTable from './components/EarthquakeTable'
import {
  deleteEarthquakeById,
  getEarthquakes,
  refreshEarthquakes,
} from './services/earthquakeService'

const DEFAULT_FILTERS = {
  minMagnitude: '2',
  location: '',
  startTime: '',
  endTime: '',
}

function App() {
  const [earthquakes, setEarthquakes] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSyncing, setIsSyncing] = useState(false)
  const [deletingId, setDeletingId] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [lastUpdated, setLastUpdated] = useState('')
  const [filters, setFilters] = useState({ ...DEFAULT_FILTERS })

  const serverFilters = useMemo(
    () => ({
      minMagnitude: filters.minMagnitude,
      startTime: filters.startTime,
      endTime: filters.endTime,
    }),
    [filters.endTime, filters.minMagnitude, filters.startTime]
  )

  const loadEarthquakes = async (currentServerFilters, signal) => {
    setIsLoading(true)
    setError('')

    try {
      const items = await getEarthquakes(currentServerFilters, signal)
      setEarthquakes(items)
      setLastUpdated(new Date().toISOString())
    } catch (err) {
      if (err.name !== 'AbortError') {
        setError(err.message || 'Failed to load earthquake data.')
      }
    } finally {
      if (!signal?.aborted) {
        setIsLoading(false)
      }
    }
  }

  useEffect(() => {
    const controller = new AbortController()
    loadEarthquakes(serverFilters, controller.signal)

    return () => {
      controller.abort()
    }
  }, [serverFilters])

  const onFilterChange = (event) => {
    const { name, value } = event.target
    setFilters((current) => ({ ...current, [name]: value }))
  }

  const onSync = async () => {
    setIsSyncing(true)
    setError('')
    setNotice('')

    try {
      await refreshEarthquakes()
      const items = await getEarthquakes(serverFilters)
      setEarthquakes(items)
      setLastUpdated(new Date().toISOString())
      setNotice('Data synced from USGS successfully.')
    } catch (err) {
      setError(err.message || 'Failed to sync earthquake data.')
    } finally {
      setIsSyncing(false)
    }
  }

  const onDelete = async (usgsId) => {
    if (isSyncing) {
      setError('Please wait for sync to finish before deleting records.')
      return
    }

    if (!usgsId) {
      setError('Cannot delete this record because the USGS ID is missing.')
      return
    }

    const confirmed = window.confirm(
      `Delete earthquake ${usgsId}? This only removes it from your local database.`
    )

    if (!confirmed) {
      return
    }

    setDeletingId(usgsId)
    setError('')
    setNotice('')

    try {
      await deleteEarthquakeById(usgsId)
      setEarthquakes((current) => current.filter((item) => item.usgsId !== usgsId))
      setNotice(`Deleted earthquake ${usgsId}.`)
    } catch (err) {
      setError(err.message || 'Failed to delete earthquake.')
    } finally {
      setDeletingId('')
    }
  }

  const filteredEarthquakes = useMemo(() => {
    const locationQuery = filters.location.trim().toLowerCase()

    return earthquakes.filter((item) => {
      if (locationQuery) {
        const place = (item.place ?? '').toLowerCase()
        const title = (item.title ?? '').toLowerCase()
        if (!place.includes(locationQuery) && !title.includes(locationQuery)) {
          return false
        }
      }

      return true
    })
  }, [earthquakes, filters.location])

  const formattedUpdated = lastUpdated
    ? new Date(lastUpdated).toLocaleString()
    : 'Not loaded yet'

  const onClearFilters = () => {
    setFilters({ ...DEFAULT_FILTERS })
  }

  return (
    <div className="app-shell">
      <Navbar
        lastUpdated={formattedUpdated}
        isBusy={isLoading || isSyncing}
        onSync={onSync}
        totalCount={earthquakes.length}
        visibleCount={filteredEarthquakes.length}
      />

      <section className="panel filters-panel">
        <div className="filters-header">
          <h2>Filters</h2>
          <button type="button" className="secondary-button" onClick={onClearFilters}>
            Clear Filters
          </button>
        </div>
        <div className="filters-grid">
          <label>
            <span>Min magnitude</span>
            <input
              type="number"
              min="0"
              step="0.1"
              name="minMagnitude"
              value={filters.minMagnitude}
              onChange={onFilterChange}
            />
          </label>

          <label>
            <span>Location (place/title)</span>
            <input
              type="text"
              name="location"
              value={filters.location}
              onChange={onFilterChange}
              placeholder="e.g. Alaska"
            />
          </label>

          <label>
            <span>Start time</span>
            <input
              type="datetime-local"
              name="startTime"
              value={filters.startTime}
              onChange={onFilterChange}
            />
          </label>

          <label>
            <span>End time</span>
            <input
              type="datetime-local"
              name="endTime"
              value={filters.endTime}
              onChange={onFilterChange}
            />
          </label>
        </div>
      </section>

      <section className="panel">
        {notice ? <p className="notice-message">{notice}</p> : null}
        {error ? <p className="error-message">{error}</p> : null}
        <EarthquakeTable
          earthquakes={filteredEarthquakes}
          isLoading={isLoading}
          isSyncing={isSyncing}
          deletingId={deletingId}
          onDelete={onDelete}
        />
      </section>
    </div>
  )
}

export default App
