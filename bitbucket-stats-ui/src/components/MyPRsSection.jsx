import {
  GitPullRequest,
  Clock,
  MessageSquare,
  FileCode,
  Plus,
  Minus
} from 'lucide-react'
import SummaryCard from './SummaryCard'
import ComparisonCard from './ComparisonCard'

function formatTimeOpen(hours) {
  if (hours === null || hours === undefined) return '-'
  if (hours < 24) return `${Math.round(hours)}h`
  const days = Math.floor(hours / 24)
  const remainingHours = Math.round(hours % 24)
  if (remainingHours === 0) return `${days}d`
  return `${days}d ${remainingHours}h`
}

function formatNumber(num) {
  if (num === null || num === undefined) return '-'
  return num.toLocaleString(undefined, { maximumFractionDigits: 1 })
}

export default function MyPRsSection({ data, compareData, loading, error, compareMode }) {
  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-red-700">
        <p className="font-medium">Error loading PR stats</p>
        <p className="text-sm mt-1">{error.message}</p>
      </div>
    )
  }

  const stats = data || {}
  const prevStats = compareData || {}

  // Use ComparisonCard when in compare mode, otherwise use SummaryCard
  const Card = compareMode ? ComparisonCard : SummaryCard

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <GitPullRequest className="h-5 w-5 text-purple-600" />
        <h2 className="text-lg font-semibold text-gray-900">My Pull Requests</h2>
        {data?.period && (
          <span className="text-sm text-gray-500">({data.period})</span>
        )}
        {compareMode && compareData?.period && (
          <span className="text-sm text-purple-500">vs ({compareData.period})</span>
        )}
      </div>

      <div className={`grid gap-4 sm:grid-cols-2 lg:grid-cols-3 ${compareMode ? 'xl:grid-cols-3' : 'xl:grid-cols-6'}`}>
        {compareMode ? (
          <>
            <ComparisonCard
              currentValue={stats.total_pull_requests}
              previousValue={prevStats.total_pull_requests}
              label="Total PRs"
              icon={<GitPullRequest />}
              color="purple"
              loading={loading}
              higherIsBetter={true}
              formatValue={(v) => v ?? '-'}
            />
            <ComparisonCard
              currentValue={stats.avg_time_open_hours}
              previousValue={prevStats.avg_time_open_hours}
              label="Avg Time Open"
              icon={<Clock />}
              color="blue"
              loading={loading}
              higherIsBetter={false}
              formatValue={formatTimeOpen}
            />
            <ComparisonCard
              currentValue={stats.avg_comment_count}
              previousValue={prevStats.avg_comment_count}
              label="Avg Comments"
              icon={<MessageSquare />}
              color="amber"
              loading={loading}
              higherIsBetter={true}
              formatValue={formatNumber}
            />
            <ComparisonCard
              currentValue={stats.avg_files_changed}
              previousValue={prevStats.avg_files_changed}
              label="Avg Files Changed"
              icon={<FileCode />}
              color="gray"
              loading={loading}
              higherIsBetter={false}
              formatValue={formatNumber}
            />
            <ComparisonCard
              currentValue={stats.avg_lines_added}
              previousValue={prevStats.avg_lines_added}
              label="Avg Lines Added"
              icon={<Plus />}
              color="green"
              loading={loading}
              higherIsBetter={true}
              formatValue={formatNumber}
            />
            <ComparisonCard
              currentValue={stats.avg_lines_removed}
              previousValue={prevStats.avg_lines_removed}
              label="Avg Lines Removed"
              icon={<Minus />}
              color="red"
              loading={loading}
              higherIsBetter={true}
              formatValue={formatNumber}
            />
          </>
        ) : (
          <>
            <SummaryCard
              value={loading ? '-' : (stats.total_pull_requests ?? '-')}
              label="Total PRs"
              icon={<GitPullRequest />}
              color="purple"
              loading={loading}
            />
            <SummaryCard
              value={loading ? '-' : formatTimeOpen(stats.avg_time_open_hours)}
              label="Avg Time Open"
              icon={<Clock />}
              color="blue"
              loading={loading}
            />
            <SummaryCard
              value={loading ? '-' : formatNumber(stats.avg_comment_count)}
              label="Avg Comments"
              icon={<MessageSquare />}
              color="amber"
              loading={loading}
            />
            <SummaryCard
              value={loading ? '-' : formatNumber(stats.avg_files_changed)}
              label="Avg Files Changed"
              icon={<FileCode />}
              color="gray"
              loading={loading}
            />
            <SummaryCard
              value={loading ? '-' : formatNumber(stats.avg_lines_added)}
              label="Avg Lines Added"
              icon={<Plus />}
              color="green"
              loading={loading}
            />
            <SummaryCard
              value={loading ? '-' : formatNumber(stats.avg_lines_removed)}
              label="Avg Lines Removed"
              icon={<Minus />}
              color="red"
              loading={loading}
            />
          </>
        )}
      </div>
    </div>
  )
}
