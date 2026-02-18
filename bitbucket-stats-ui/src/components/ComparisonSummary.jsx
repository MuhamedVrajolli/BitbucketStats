import { TrendingUp, TrendingDown, Minus, AlertCircle } from 'lucide-react'

function calculateChange(current, previous) {
  if (!previous || previous === 0) return null
  if (current === null || current === undefined) return null
  return ((current - previous) / previous) * 100
}

function ChangeIndicator({ current, previous, label, higherIsBetter = true }) {
  const change = calculateChange(current, previous)
  if (change === null) return null

  const isPositive = change > 0
  const isNegative = change < 0
  const isGood = higherIsBetter ? isPositive : isNegative
  const isBad = higherIsBetter ? isNegative : isPositive

  return (
    <div className="flex items-center gap-2">
      <span className="text-sm text-gray-600">{label}:</span>
      <span className={`flex items-center gap-1 text-sm font-medium ${
        isGood ? 'text-green-600' : isBad ? 'text-red-600' : 'text-gray-500'
      }`}>
        {isPositive ? <TrendingUp className="h-4 w-4" /> :
         isNegative ? <TrendingDown className="h-4 w-4" /> :
         <Minus className="h-4 w-4" />}
        {change > 0 ? '+' : ''}{change.toFixed(1)}%
      </span>
    </div>
  )
}

export default function ComparisonSummary({ prData, comparePrData, reviewData, compareReviewData }) {
  if (!prData || !comparePrData) return null

  const insights = []

  // PR volume change
  const prChange = calculateChange(prData.total_pull_requests, comparePrData.total_pull_requests)
  if (prChange !== null) {
    if (prChange > 20) {
      insights.push({ type: 'positive', text: `PR output increased by ${prChange.toFixed(0)}%` })
    } else if (prChange < -20) {
      insights.push({ type: 'warning', text: `PR output decreased by ${Math.abs(prChange).toFixed(0)}%` })
    }
  }

  // Time to merge change
  const timeChange = calculateChange(prData.avg_time_open_hours, comparePrData.avg_time_open_hours)
  if (timeChange !== null) {
    if (timeChange < -20) {
      insights.push({ type: 'positive', text: `PRs merging ${Math.abs(timeChange).toFixed(0)}% faster` })
    } else if (timeChange > 20) {
      insights.push({ type: 'warning', text: `PRs taking ${timeChange.toFixed(0)}% longer to merge` })
    }
  }

  // Review activity
  if (reviewData && compareReviewData) {
    const reviewChange = calculateChange(reviewData.total_pull_requests_reviewed, compareReviewData.total_pull_requests_reviewed)
    if (reviewChange !== null && reviewChange > 20) {
      insights.push({ type: 'positive', text: `Review activity up ${reviewChange.toFixed(0)}%` })
    }
  }

  return (
    <div className="bg-gradient-to-r from-purple-50 to-blue-50 rounded-xl border border-purple-200 p-4">
      <h3 className="font-semibold text-gray-900 mb-3">Comparison Summary</h3>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 mb-4">
        <ChangeIndicator
          current={prData.total_pull_requests}
          previous={comparePrData.total_pull_requests}
          label="PRs Created"
          higherIsBetter={true}
        />
        <ChangeIndicator
          current={prData.avg_time_open_hours}
          previous={comparePrData.avg_time_open_hours}
          label="Avg Time Open"
          higherIsBetter={false}
        />
        {reviewData && compareReviewData && (
          <>
            <ChangeIndicator
              current={reviewData.total_pull_requests_reviewed}
              previous={compareReviewData.total_pull_requests_reviewed}
              label="PRs Reviewed"
              higherIsBetter={true}
            />
            <ChangeIndicator
              current={reviewData.approved_percentage}
              previous={compareReviewData.approved_percentage}
              label="Approval Rate"
              higherIsBetter={true}
            />
          </>
        )}
      </div>

      {insights.length > 0 && (
        <div className="border-t border-purple-200 pt-3 mt-3">
          <p className="text-sm font-medium text-gray-700 mb-2">Key Insights:</p>
          <div className="flex flex-wrap gap-2">
            {insights.map((insight, i) => (
              <span
                key={i}
                className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${
                  insight.type === 'positive'
                    ? 'bg-green-100 text-green-700'
                    : 'bg-amber-100 text-amber-700'
                }`}
              >
                {insight.type === 'positive' ? <TrendingUp className="h-3 w-3" /> : <AlertCircle className="h-3 w-3" />}
                {insight.text}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
