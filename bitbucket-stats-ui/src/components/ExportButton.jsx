import { Download } from 'lucide-react'

function formatTimeOpen(hours) {
  if (hours === null || hours === undefined) return '-'
  if (hours < 24) return `${Math.round(hours)}h`
  const days = Math.floor(hours / 24)
  const remainingHours = Math.round(hours % 24)
  if (remainingHours === 0) return `${days}d`
  return `${days}d ${remainingHours}h`
}

export default function ExportButton({ prData, reviewData, comparePrData, compareReviewData, compareMode }) {
  const handleExport = () => {
    const rows = []

    // Header
    if (compareMode && comparePrData) {
      rows.push(['Metric', 'Current Period', 'Previous Period', 'Change %'])
    } else {
      rows.push(['Metric', 'Value'])
    }

    // PR Stats
    rows.push(['--- MY PULL REQUESTS ---', '', '', ''])
    if (prData) {
      rows.push(['Period', prData.period || '-', comparePrData?.period || '', ''])

      const prMetrics = [
        ['Total PRs', prData.total_pull_requests, comparePrData?.total_pull_requests],
        ['Avg Time Open', formatTimeOpen(prData.avg_time_open_hours), formatTimeOpen(comparePrData?.avg_time_open_hours)],
        ['Avg Comments', prData.avg_comment_count?.toFixed(1), comparePrData?.avg_comment_count?.toFixed(1)],
        ['Avg Files Changed', prData.avg_files_changed?.toFixed(1), comparePrData?.avg_files_changed?.toFixed(1)],
        ['Avg Lines Added', prData.avg_lines_added?.toFixed(1), comparePrData?.avg_lines_added?.toFixed(1)],
        ['Avg Lines Removed', prData.avg_lines_removed?.toFixed(1), comparePrData?.avg_lines_removed?.toFixed(1)],
      ]

      prMetrics.forEach(([label, current, previous]) => {
        if (compareMode && comparePrData) {
          const change = previous && current ? (((current - previous) / previous) * 100).toFixed(1) + '%' : '-'
          rows.push([label, current ?? '-', previous ?? '-', change])
        } else {
          rows.push([label, current ?? '-'])
        }
      })
    }

    // Review Stats
    rows.push(['--- REVIEW ACTIVITY ---', '', '', ''])
    if (reviewData) {
      rows.push(['Period', reviewData.period || '-', compareReviewData?.period || '', ''])

      const reviewMetrics = [
        ['PRs Reviewed', reviewData.total_pull_requests_reviewed, compareReviewData?.total_pull_requests_reviewed],
        ['PRs Approved', reviewData.total_pull_requests_approved, compareReviewData?.total_pull_requests_approved],
        ['Approval Rate %', reviewData.approved_percentage?.toFixed(1), compareReviewData?.approved_percentage?.toFixed(1)],
        ['Total Comments', reviewData.total_comments, compareReviewData?.total_comments],
        ['Comment Rate %', reviewData.commented_percentage?.toFixed(1), compareReviewData?.commented_percentage?.toFixed(1)],
      ]

      reviewMetrics.forEach(([label, current, previous]) => {
        if (compareMode && compareReviewData) {
          const change = previous && current ? (((current - previous) / previous) * 100).toFixed(1) + '%' : '-'
          rows.push([label, current ?? '-', previous ?? '-', change])
        } else {
          rows.push([label, current ?? '-'])
        }
      })
    }

    // PR Details
    if (prData?.pull_request_details?.length > 0) {
      rows.push(['--- PR DETAILS ---', '', '', ''])
      rows.push(['ID', 'Title', 'Repo', 'Time Open', 'Comments', 'Files', 'Lines Added', 'Lines Removed', 'Link'])

      prData.pull_request_details.forEach(pr => {
        rows.push([
          pr.id,
          pr.title,
          pr.repo,
          formatTimeOpen(pr.time_open_hours),
          pr.comment_count ?? '-',
          pr.diff_details?.files_changed ?? '-',
          pr.diff_details?.lines_added ?? '-',
          pr.diff_details?.lines_removed ?? '-',
          pr.link
        ])
      })
    }

    // Convert to CSV
    const csv = rows.map(row =>
      row.map(cell => {
        const str = String(cell ?? '')
        // Escape quotes and wrap in quotes if contains comma or quote
        if (str.includes(',') || str.includes('"') || str.includes('\n')) {
          return `"${str.replace(/"/g, '""')}"`
        }
        return str
      }).join(',')
    ).join('\n')

    // Download
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `bitbucket-stats-${new Date().toISOString().split('T')[0]}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }

  if (!prData && !reviewData) return null

  return (
    <button
      onClick={handleExport}
      className="flex items-center gap-2 px-3 py-1.5 text-sm font-medium bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
    >
      <Download className="h-4 w-4" />
      Export CSV
    </button>
  )
}
