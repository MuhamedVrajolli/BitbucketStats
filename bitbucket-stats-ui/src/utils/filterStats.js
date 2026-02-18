import { getPRBusinessHours } from './businessHours'

/**
 * Filter PR data to exclude stale PRs and recalculate averages
 * @param {Object} data - PR stats data from API
 * @param {number|null} maxDaysOpen - Max days open to include (null = no filter)
 * @param {boolean} excludeWeekends - Whether to exclude weekends from time calculation
 */
export function filterPRStats(data, maxDaysOpen, excludeWeekends = false) {
  if (!data) return data

  const details = data.pull_request_details || []

  // If no filters applied, just add business hours if needed
  if (!maxDaysOpen && !excludeWeekends) return data

  // Calculate hours for each PR (business hours if excludeWeekends)
  const prsWithHours = details.map(pr => {
    const hours = excludeWeekends ? getPRBusinessHours(pr) : pr.time_open_hours
    return { ...pr, _calculatedHours: hours }
  })

  // Filter by max days open (using calculated hours)
  const maxHours = maxDaysOpen ? maxDaysOpen * 24 : Infinity
  const filteredDetails = prsWithHours.filter(pr => {
    const hours = pr._calculatedHours
    return hours !== null && hours !== undefined && hours <= maxHours
  })

  // If no PRs remain after filtering, return with note
  if (filteredDetails.length === 0) {
    return {
      ...data,
      _filtered: true,
      _originalCount: details.length,
      _filteredCount: 0,
      _excludeWeekends: excludeWeekends,
    }
  }

  // Recalculate averages using the calculated hours
  const totalPRs = filteredDetails.length
  const sumTimeOpen = filteredDetails.reduce((sum, pr) => sum + (pr._calculatedHours || 0), 0)
  const sumComments = filteredDetails.reduce((sum, pr) => sum + (pr.comment_count || 0), 0)

  let sumFilesChanged = 0
  let sumLinesAdded = 0
  let sumLinesRemoved = 0
  let hasDiffDetails = false

  filteredDetails.forEach(pr => {
    if (pr.diff_details) {
      hasDiffDetails = true
      sumFilesChanged += pr.diff_details.files_changed || 0
      sumLinesAdded += pr.diff_details.lines_added || 0
      sumLinesRemoved += pr.diff_details.lines_removed || 0
    }
  })

  // Update time_open_hours to use calculated hours for display
  const updatedDetails = filteredDetails.map(pr => ({
    ...pr,
    time_open_hours: pr._calculatedHours,
  }))

  return {
    ...data,
    total_pull_requests: totalPRs,
    avg_time_open_hours: totalPRs > 0 ? sumTimeOpen / totalPRs : null,
    avg_comment_count: totalPRs > 0 ? sumComments / totalPRs : null,
    avg_files_changed: hasDiffDetails && totalPRs > 0 ? sumFilesChanged / totalPRs : null,
    avg_lines_added: hasDiffDetails && totalPRs > 0 ? sumLinesAdded / totalPRs : null,
    avg_lines_removed: hasDiffDetails && totalPRs > 0 ? sumLinesRemoved / totalPRs : null,
    pull_request_details: updatedDetails,
    _filtered: true,
    _originalCount: details.length,
    _filteredCount: totalPRs,
    _excludedCount: details.length - totalPRs,
    _excludeWeekends: excludeWeekends,
  }
}
