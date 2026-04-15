export default function Navbar({
  isBusy,
  lastUpdated,
  onSync,
  totalCount,
  visibleCount,
}) {
  return (
	<header className="navbar panel">
	  <div>
		<h1>Earthquake Dashboard</h1>
		<p className="subtitle">USGS last-hour data stored by Spring Boot + PostgreSQL</p>
	  </div>

	  <div className="navbar-meta">
		<p>
		  Showing <strong>{visibleCount}</strong> of <strong>{totalCount}</strong> records
		</p>
		<p>
		  Last loaded: <strong>{lastUpdated}</strong>
		</p>
	  </div>

	  <button type="button" className="primary-button" onClick={onSync} disabled={isBusy}>
		{isBusy ? 'Working...' : 'Sync From USGS'}
	  </button>
	</header>
  )
}

