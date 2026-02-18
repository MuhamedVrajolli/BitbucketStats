// Direct backend URL - ensure backend is running on this port
const API_BASE = 'http://localhost:8081'

function buildQueryString(params) {
  const searchParams = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') return

    if (Array.isArray(value)) {
      value.forEach(v => searchParams.append(key, v))
    } else {
      searchParams.append(key, value)
    }
  })

  return searchParams.toString()
}

function buildHeaders(credentials) {
  const headers = {
    'Content-Type': 'application/json',
  }

  if (credentials) {
    const basicAuth = btoa(`${credentials.username}:${credentials.appPassword}`)
    headers['Authorization'] = `Basic ${basicAuth}`
    headers['username'] = credentials.username
    headers['appPassword'] = credentials.appPassword
  }

  return headers
}

export async function fetchMyPRStats(credentials, params) {
  const queryString = buildQueryString({
    workspace: params.workspace,
    repo: params.repos,
    sinceDate: params.sinceDate,
    untilDate: params.untilDate,
    state: params.state,
    includeDiffDetails: true,
    includePullRequestDetails: params.includePullRequestDetails || false,
    nickname: params.nickname,
  })

  const response = await fetch(`${API_BASE}/pull-requests/stats?${queryString}`, {
    method: 'GET',
    headers: buildHeaders(credentials),
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }))
    throw new Error(error.message || 'Failed to fetch PR stats')
  }

  return response.json()
}

export async function fetchReviewStats(credentials, params) {
  const queryString = buildQueryString({
    workspace: params.workspace,
    repo: params.repos,
    sinceDate: params.sinceDate,
    untilDate: params.untilDate,
    state: params.state,
    includeCommentDetails: params.includeCommentDetails || false,
  })

  const response = await fetch(`${API_BASE}/pull-requests/reviews/stats?${queryString}`, {
    method: 'GET',
    headers: buildHeaders(credentials),
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }))
    throw new Error(error.message || 'Failed to fetch review stats')
  }

  return response.json()
}
