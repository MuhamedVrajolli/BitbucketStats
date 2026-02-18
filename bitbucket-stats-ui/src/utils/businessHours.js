/**
 * Calculate business hours between two dates, excluding weekends
 * @param {string} startDateStr - ISO date string
 * @param {string} endDateStr - ISO date string
 * @returns {number} Hours excluding weekends
 */
export function calculateBusinessHours(startDateStr, endDateStr) {
  if (!startDateStr || !endDateStr) return null

  const start = new Date(startDateStr)
  const end = new Date(endDateStr)

  if (isNaN(start.getTime()) || isNaN(end.getTime())) return null

  let totalHours = 0
  const current = new Date(start)

  while (current < end) {
    const dayOfWeek = current.getDay()
    // 0 = Sunday, 6 = Saturday
    const isWeekend = dayOfWeek === 0 || dayOfWeek === 6

    if (!isWeekend) {
      // Add hours for this day (up to 24 or remaining time)
      const nextDay = new Date(current)
      nextDay.setDate(nextDay.getDate() + 1)
      nextDay.setHours(0, 0, 0, 0)

      const endOfDay = nextDay < end ? nextDay : end
      const hoursThisDay = (endOfDay - current) / (1000 * 60 * 60)
      totalHours += hoursThisDay
    }

    // Move to next day
    current.setDate(current.getDate() + 1)
    current.setHours(0, 0, 0, 0)
  }

  return Math.round(totalHours)
}

/**
 * Calculate business hours for a PR
 * @param {Object} pr - Pull request object with created_on and closed_on
 * @returns {number} Business hours
 */
export function getPRBusinessHours(pr) {
  if (!pr.created_on || !pr.closed_on) {
    // Fall back to original time_open_hours
    return pr.time_open_hours
  }
  return calculateBusinessHours(pr.created_on, pr.closed_on)
}
