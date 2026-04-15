function formatDate(value) {
  if (!value) {
	return '-'
  }

  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) {
	return '-'
  }

  return date.toLocaleString()
}

function formatNumber(value, fractionDigits = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
	return '-'
  }

  return Number(value).toFixed(fractionDigits)
}

export default function EarthquakeTable({
  earthquakes,
  isLoading,
  isSyncing,
  deletingId,
  onDelete,
}) {
  if (isLoading) {
	return <p>Loading earthquake data...</p>
  }

  if (!earthquakes.length) {
	return <p>No earthquakes found for the selected filters.</p>
  }

  return (
	<div className="table-wrapper">
	  <table className="earthquake-table">
		<thead>
		  <tr>
			<th>Magnitude</th>
			<th>Mag Type</th>
			<th>Place</th>
			<th>Title</th>
			<th>Time</th>
			<th>Details</th>
			<th>Action</th>
		  </tr>
		</thead>
		<tbody>
		  {earthquakes.map((earthquake, index) => {
			const currentId = earthquake.usgsId || earthquake.id || `row-${index}`
			const hasUsgsId = Boolean(earthquake.usgsId)
			const canDelete = hasUsgsId && !isSyncing && deletingId !== earthquake.usgsId
			const isDeleting = deletingId === earthquake.usgsId
			const deleteTitle = !hasUsgsId
			  ? 'Cannot delete: missing USGS ID'
			  : isSyncing
				? 'Cannot delete while sync is running'
				: isDeleting
				  ? 'Deleting earthquake...'
				  : 'Delete this earthquake from local database'

			return (
			  <tr key={currentId}>
				<td>{formatNumber(earthquake.magnitude, 1)}</td>
				<td>{earthquake.magType || '-'}</td>
				<td>{earthquake.place || '-'}</td>
				<td>{earthquake.title || '-'}</td>
				<td>{formatDate(earthquake.eventTime)}</td>
				<td>
				  <details className="row-details">
					<summary>View</summary>
					<div className="row-details-body">
					  <p><strong>USGS ID:</strong> {earthquake.usgsId || '-'}</p>
					  <p><strong>Latitude:</strong> {formatNumber(earthquake.latitude, 3)}</p>
					  <p><strong>Longitude:</strong> {formatNumber(earthquake.longitude, 3)}</p>
					  <p><strong>Depth (km):</strong> {formatNumber(earthquake.depthKm, 2)}</p>
					</div>
				  </details>
				</td>
				<td>
				  <button
					type="button"
					className="danger-button"
					disabled={!canDelete}
					title={deleteTitle}
					onClick={() => onDelete(earthquake.usgsId)}
				  >
					{isDeleting ? 'Deleting...' : 'Delete'}
				  </button>
				</td>
			  </tr>
			)
		  })}
		</tbody>
	  </table>
	</div>
  )
}


