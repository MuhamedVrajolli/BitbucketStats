import { Calendar } from 'lucide-react'

const getPresets = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = today.getMonth()

  // Helper to format date as YYYY-MM-DD
  const fmt = (d) => d.toISOString().split('T')[0]

  // First day of current month
  const thisMonthStart = new Date(year, month, 1)
  // Last day of current month
  const thisMonthEnd = new Date(year, month + 1, 0)
  // First day of last month
  const lastMonthStart = new Date(year, month - 1, 1)
  // Last day of last month
  const lastMonthEnd = new Date(year, month, 0)

  // Quarters
  const currentQuarter = Math.floor(month / 3)
  const thisQuarterStart = new Date(year, currentQuarter * 3, 1)
  const thisQuarterEnd = new Date(year, (currentQuarter + 1) * 3, 0)
  const lastQuarterStart = new Date(year, (currentQuarter - 1) * 3, 1)
  const lastQuarterEnd = new Date(year, currentQuarter * 3, 0)

  // Half years
  const isFirstHalf = month < 6
  const thisHalfStart = new Date(year, isFirstHalf ? 0 : 6, 1)
  const thisHalfEnd = new Date(year, isFirstHalf ? 6 : 12, 0)
  const lastHalfStart = new Date(isFirstHalf ? year - 1 : year, isFirstHalf ? 6 : 0, 1)
  const lastHalfEnd = new Date(isFirstHalf ? year : year, isFirstHalf ? 0 : 6, 0)

  // Last 30 days vs previous 30 days
  const last30End = today
  const last30Start = new Date(today)
  last30Start.setDate(last30Start.getDate() - 30)
  const prev30End = new Date(last30Start)
  prev30End.setDate(prev30End.getDate() - 1)
  const prev30Start = new Date(prev30End)
  prev30Start.setDate(prev30Start.getDate() - 30)

  // Year to date vs same period last year
  const ytdStart = new Date(year, 0, 1)
  const ytdEnd = today
  const lastYtdStart = new Date(year - 1, 0, 1)
  const lastYtdEnd = new Date(year - 1, month, today.getDate())

  return [
    {
      label: 'Last 30d vs Prev 30d',
      current: { since: fmt(last30Start), until: fmt(last30End) },
      previous: { since: fmt(prev30Start), until: fmt(prev30End) },
    },
    {
      label: 'This Month vs Last',
      current: { since: fmt(thisMonthStart), until: fmt(thisMonthEnd) },
      previous: { since: fmt(lastMonthStart), until: fmt(lastMonthEnd) },
    },
    {
      label: 'This Quarter vs Last',
      current: { since: fmt(thisQuarterStart), until: fmt(thisQuarterEnd) },
      previous: { since: fmt(lastQuarterStart), until: fmt(lastQuarterEnd) },
    },
    {
      label: 'This Half vs Last',
      current: { since: fmt(thisHalfStart), until: fmt(thisHalfEnd) },
      previous: { since: fmt(lastHalfStart), until: fmt(lastHalfEnd) },
    },
    {
      label: 'YTD vs Last Year',
      current: { since: fmt(ytdStart), until: fmt(ytdEnd) },
      previous: { since: fmt(lastYtdStart), until: fmt(lastYtdEnd) },
    },
  ]
}

export default function PeriodPresets({ onSelect }) {
  const presets = getPresets()

  return (
    <div className="flex flex-wrap gap-2">
      <span className="text-sm text-gray-500 flex items-center gap-1">
        <Calendar className="h-4 w-4" />
        Quick:
      </span>
      {presets.map((preset) => (
        <button
          key={preset.label}
          type="button"
          onClick={() => onSelect(preset)}
          className="px-2 py-1 text-xs font-medium bg-purple-100 text-purple-700 rounded hover:bg-purple-200 transition-colors"
        >
          {preset.label}
        </button>
      ))}
    </div>
  )
}
