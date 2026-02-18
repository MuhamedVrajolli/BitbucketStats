import { cloneElement } from 'react'
import { TrendingUp, TrendingDown, Minus } from 'lucide-react'

const colorClasses = {
  blue: 'bg-blue-50 text-blue-600 border-blue-100',
  green: 'bg-green-50 text-green-600 border-green-100',
  amber: 'bg-amber-50 text-amber-600 border-amber-100',
  purple: 'bg-purple-50 text-purple-600 border-purple-100',
  red: 'bg-red-50 text-red-600 border-red-100',
  gray: 'bg-gray-50 text-gray-600 border-gray-100',
}

const iconBgClasses = {
  blue: 'bg-blue-100',
  green: 'bg-green-100',
  amber: 'bg-amber-100',
  purple: 'bg-purple-100',
  red: 'bg-red-100',
  gray: 'bg-gray-100',
}

function calculateChange(current, previous) {
  if (previous === 0 || previous === null || previous === undefined) return null
  if (current === null || current === undefined) return null
  return ((current - previous) / previous) * 100
}

function formatChange(change) {
  if (change === null) return null
  const sign = change > 0 ? '+' : ''
  return `${sign}${change.toFixed(1)}%`
}

export default function ComparisonCard({
  currentValue,
  previousValue,
  label,
  icon,
  color = 'blue',
  loading = false,
  higherIsBetter = true,
  formatValue = (v) => v
}) {
  const colorClass = colorClasses[color] || colorClasses.blue
  const iconBgClass = iconBgClasses[color] || iconBgClasses.blue

  const change = calculateChange(
    typeof currentValue === 'string' ? parseFloat(currentValue) : currentValue,
    typeof previousValue === 'string' ? parseFloat(previousValue) : previousValue
  )

  const isPositive = change !== null && change > 0
  const isNegative = change !== null && change < 0
  const isGood = higherIsBetter ? isPositive : isNegative
  const isBad = higherIsBetter ? isNegative : isPositive

  if (loading) {
    return (
      <div className={`rounded-xl border p-4 ${colorClass} card-shadow animate-pulse`}>
        <div className="space-y-3">
          <div className="h-6 w-16 bg-current opacity-20 rounded"></div>
          <div className="h-4 w-20 bg-current opacity-10 rounded"></div>
        </div>
      </div>
    )
  }

  return (
    <div className={`rounded-xl border p-4 ${colorClass} card-shadow card-shadow-hover transition-all duration-200`}>
      <div className="flex items-start justify-between mb-2">
        <p className="text-xs font-medium opacity-70 uppercase tracking-wide">{label}</p>
        {icon && (
          <div className={`p-2 rounded-lg ${iconBgClass}`}>
            {cloneElement(icon, { className: 'h-4 w-4' })}
          </div>
        )}
      </div>

      <div className="space-y-2">
        {/* Current Period */}
        <div>
          <p className="text-2xl font-bold tracking-tight">{formatValue(currentValue)}</p>
          <p className="text-xs opacity-60">Current period</p>
        </div>

        {/* Previous Period */}
        <div className="pt-2 border-t border-current border-opacity-10">
          <p className="text-lg font-semibold opacity-80">{formatValue(previousValue)}</p>
          <p className="text-xs opacity-60">Previous period</p>
        </div>

        {/* Change Indicator */}
        {change !== null && (
          <div className={`flex items-center gap-1 text-sm font-medium ${
            isGood ? 'text-green-600' : isBad ? 'text-red-600' : 'text-gray-500'
          }`}>
            {isPositive ? (
              <TrendingUp className="h-4 w-4" />
            ) : isNegative ? (
              <TrendingDown className="h-4 w-4" />
            ) : (
              <Minus className="h-4 w-4" />
            )}
            <span>{formatChange(change)}</span>
          </div>
        )}
      </div>
    </div>
  )
}
