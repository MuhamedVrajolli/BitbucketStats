import { ExternalLink, Clock, MessageSquare, FileCode, Plus, Minus } from 'lucide-react'

function formatTimeOpen(hours) {
  if (hours === null || hours === undefined) return '-'
  if (hours < 24) return `${Math.round(hours)}h`
  const days = Math.floor(hours / 24)
  const remainingHours = Math.round(hours % 24)
  if (remainingHours === 0) return `${days}d`
  return `${days}d ${remainingHours}h`
}

export default function PRDetailsList({ pullRequests, loading }) {
  if (loading) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 p-4 card-shadow">
        <div className="animate-pulse space-y-3">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-16 bg-gray-100 rounded-lg"></div>
          ))}
        </div>
      </div>
    )
  }

  if (!pullRequests || pullRequests.length === 0) {
    return null
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 card-shadow overflow-hidden">
      <div className="p-4 border-b border-gray-100">
        <h3 className="font-semibold text-gray-900">Pull Request Details</h3>
      </div>
      <div className="divide-y divide-gray-100">
        {pullRequests.map(pr => (
          <div key={pr.id} className="p-4 hover:bg-gray-50 transition-colors">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium text-gray-500">#{pr.id}</span>
                  <a
                    href={pr.link}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-800 font-medium truncate flex items-center gap-1"
                  >
                    {pr.title}
                    <ExternalLink className="h-3 w-3 flex-shrink-0" />
                  </a>
                </div>
                {pr.repo && (
                  <span className="text-xs text-gray-500 mt-1 block">{pr.repo}</span>
                )}
              </div>

              <div className="flex items-center gap-4 text-sm text-gray-600">
                <div className="flex items-center gap-1" title="Time Open">
                  <Clock className="h-4 w-4" />
                  <span>{formatTimeOpen(pr.time_open_hours)}</span>
                </div>
                <div className="flex items-center gap-1" title="Comments">
                  <MessageSquare className="h-4 w-4" />
                  <span>{pr.comment_count ?? '-'}</span>
                </div>
                {pr.diff_details && (
                  <>
                    <div className="flex items-center gap-1" title="Files Changed">
                      <FileCode className="h-4 w-4" />
                      <span>{pr.diff_details.files_changed}</span>
                    </div>
                    <div className="flex items-center gap-1 text-green-600" title="Lines Added">
                      <Plus className="h-4 w-4" />
                      <span>{pr.diff_details.lines_added}</span>
                    </div>
                    <div className="flex items-center gap-1 text-red-600" title="Lines Removed">
                      <Minus className="h-4 w-4" />
                      <span>{pr.diff_details.lines_removed}</span>
                    </div>
                  </>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
