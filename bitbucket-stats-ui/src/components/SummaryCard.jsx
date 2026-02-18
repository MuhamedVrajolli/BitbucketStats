import { cloneElement } from 'react'

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

export default function SummaryCard({ value, label, icon, color = 'blue', loading = false }) {
  const colorClass = colorClasses[color] || colorClasses.blue
  const iconBgClass = iconBgClasses[color] || iconBgClasses.blue

  if (loading) {
    return (
      <div className={`rounded-xl border p-6 ${colorClass} card-shadow animate-pulse`}>
        <div className="flex items-start justify-between">
          <div className="space-y-3">
            <div className="h-8 w-20 bg-current opacity-20 rounded"></div>
            <div className="h-4 w-24 bg-current opacity-10 rounded"></div>
          </div>
          <div className={`p-3 rounded-lg ${iconBgClass} opacity-50`}>
            <div className="h-6 w-6"></div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className={`rounded-xl border p-6 ${colorClass} card-shadow card-shadow-hover transition-all duration-200`}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-3xl font-bold tracking-tight">{value}</p>
          <p className="mt-1 text-sm font-medium opacity-80">{label}</p>
        </div>
        {icon && (
          <div className={`p-3 rounded-lg ${iconBgClass}`}>
            {cloneElement(icon, { className: 'h-6 w-6' })}
          </div>
        )}
      </div>
    </div>
  )
}
