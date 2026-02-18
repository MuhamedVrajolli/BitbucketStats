import {
  Eye,
  CheckCircle,
  Percent,
  MessageCircle,
  Activity
} from 'lucide-react'
import SummaryCard from './SummaryCard'
import ComparisonCard from './ComparisonCard'

function formatPercentage(value) {
  if (value === null || value === undefined) return '-'
  return `${value.toFixed(1)}%`
}

export default function ReviewSection({ data, compareData, loading, error, compareMode }) {
  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-red-700">
        <p className="font-medium">Error loading review stats</p>
        <p className="text-sm mt-1">{error.message}</p>
      </div>
    )
  }

  const stats = data || {}
  const prevStats = compareData || {}

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <Eye className="h-5 w-5 text-blue-600" />
        <h2 className="text-lg font-semibold text-gray-900">Review Activity</h2>
        {data?.period && (
          <span className="text-sm text-gray-500">({data.period})</span>
        )}
        {compareMode && compareData?.period && (
          <span className="text-sm text-purple-500">vs ({compareData.period})</span>
        )}
      </div>

      <div className={`grid gap-4 sm:grid-cols-2 lg:grid-cols-3 ${compareMode ? 'xl:grid-cols-3' : 'xl:grid-cols-5'}`}>
        {compareMode ? (
          <>
            <ComparisonCard
              currentValue={stats.total_pull_requests_reviewed}
              previousValue={prevStats.total_pull_requests_reviewed}
              label="PRs Reviewed"
              icon={<Eye />}
              color="blue"
              loading={loading}
              higherIsBetter={true}
              formatValue={(v) => v ?? '-'}
            />
            <ComparisonCard
              currentValue={stats.total_pull_requests_approved}
              previousValue={prevStats.total_pull_requests_approved}
              label="PRs Approved"
              icon={<CheckCircle />}
              color="green"
              loading={loading}
              higherIsBetter={true}
              formatValue={(v) => v ?? '-'}
            />
            <ComparisonCard
              currentValue={stats.approved_percentage}
              previousValue={prevStats.approved_percentage}
              label="Approval Rate"
              icon={<Percent />}
              color="green"
              loading={loading}
              higherIsBetter={true}
              formatValue={formatPercentage}
            />
            <ComparisonCard
              currentValue={stats.total_comments}
              previousValue={prevStats.total_comments}
              label="Total Comments"
              icon={<MessageCircle />}
              color="amber"
              loading={loading}
              higherIsBetter={true}
              formatValue={(v) => v ?? '-'}
            />
            <ComparisonCard
              currentValue={stats.commented_percentage}
              previousValue={prevStats.commented_percentage}
              label="Comment Rate"
              icon={<Activity />}
              color="purple"
              loading={loading}
              higherIsBetter={true}
              formatValue={formatPercentage}
            />
          </>
        ) : (
          <>
            <SummaryCard
              value={loading ? '-' : (stats.total_pull_requests_reviewed ?? '-')}
              label="PRs Reviewed"
              icon={<Eye />}
              color="blue"
              loading={loading}
            />
            <SummaryCard
              value={loading ? '-' : (stats.total_pull_requests_approved ?? '-')}
              label="PRs Approved"
              icon={<CheckCircle />}
              color="green"
              loading={loading}
            />
            <SummaryCard
              value={loading ? '-' : formatPercentage(stats.approved_percentage)}
              label="Approval Rate"
              icon={<Percent />}
              color="green"
              loading={loading}
            />
            <SummaryCard
              value={loading ? '-' : (stats.total_comments ?? '-')}
              label="Total Comments"
              icon={<MessageCircle />}
              color="amber"
              loading={loading}
            />
            <SummaryCard
              value={loading ? '-' : formatPercentage(stats.commented_percentage)}
              label="Comment Rate"
              icon={<Activity />}
              color="purple"
              loading={loading}
            />
          </>
        )}
      </div>
    </div>
  )
}
