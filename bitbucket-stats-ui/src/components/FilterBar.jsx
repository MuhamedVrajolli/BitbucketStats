import { useState } from 'react'
import { Calendar, GitBranch, Filter, Search, X, Plus, GitCompare, Clock, CalendarOff } from 'lucide-react'
import PeriodPresets from './PeriodPresets'

const getDefaultDates = () => {
  const today = new Date()
  const thirtyDaysAgo = new Date(today)
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)

  return {
    sinceDate: thirtyDaysAgo.toISOString().split('T')[0],
    untilDate: today.toISOString().split('T')[0],
  }
}

export default function FilterBar({ filters, setFilters, onFetch, loading, compareMode, setCompareMode }) {
  const [repoInput, setRepoInput] = useState('')
  const defaults = getDefaultDates()

  const handleChange = (e) => {
    const { name, value } = e.target
    setFilters(prev => ({ ...prev, [name]: value }))
  }

  const handleStateToggle = (state) => {
    setFilters(prev => {
      const currentStates = prev.state || ['MERGED']
      if (currentStates.includes(state)) {
        const newStates = currentStates.filter(s => s !== state)
        return { ...prev, state: newStates.length > 0 ? newStates : ['MERGED'] }
      }
      return { ...prev, state: [...currentStates, state] }
    })
  }

  const addRepo = () => {
    if (repoInput.trim() && !filters.repos?.includes(repoInput.trim())) {
      setFilters(prev => ({
        ...prev,
        repos: [...(prev.repos || []), repoInput.trim()]
      }))
      setRepoInput('')
    }
  }

  const removeRepo = (repo) => {
    setFilters(prev => ({
      ...prev,
      repos: prev.repos.filter(r => r !== repo)
    }))
  }

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      addRepo()
    }
  }

  const handlePresetSelect = (preset) => {
    setFilters(prev => ({
      ...prev,
      sinceDate: preset.current.since,
      untilDate: preset.current.until,
      compareSinceDate: preset.previous.since,
      compareUntilDate: preset.previous.until,
    }))
  }

  const states = ['MERGED', 'OPEN', 'DECLINED']
  const currentStates = filters.state || ['MERGED']

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-4 card-shadow space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-gray-700">
          <Filter className="h-5 w-5" />
          <h3 className="font-medium">Filters</h3>
        </div>
        <button
          type="button"
          onClick={() => setCompareMode(!compareMode)}
          className={`flex items-center gap-2 px-3 py-1.5 text-sm font-medium rounded-lg transition-colors ${
            compareMode
              ? 'bg-purple-600 text-white'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          <GitCompare className="h-4 w-4" />
          Compare Periods
        </button>
      </div>

      {/* Current Period */}
      <div className="space-y-2">
        <p className="text-sm font-medium text-gray-600">
          {compareMode ? 'Current Period' : 'Date Range'}
        </p>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Calendar className="inline h-4 w-4 mr-1" />
              Since Date
            </label>
            <input
              type="date"
              name="sinceDate"
              value={filters.sinceDate || defaults.sinceDate}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Calendar className="inline h-4 w-4 mr-1" />
              Until Date
            </label>
            <input
              type="date"
              name="untilDate"
              value={filters.untilDate || defaults.untilDate}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
            />
          </div>

          {/* Nickname Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Search className="inline h-4 w-4 mr-1" />
              Author Nickname
            </label>
            <input
              type="text"
              name="nickname"
              value={filters.nickname || ''}
              onChange={handleChange}
              placeholder="Filter by author"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
            />
          </div>

          {/* Max Days Open Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Clock className="inline h-4 w-4 mr-1" />
              Max Days Open
            </label>
            <input
              type="number"
              name="maxDaysOpen"
              value={filters.maxDaysOpen || ''}
              onChange={handleChange}
              placeholder="e.g., 10 (exclude stale)"
              min="1"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
            />
            <label className="flex items-center gap-2 mt-2 cursor-pointer">
              <input
                type="checkbox"
                name="excludeWeekends"
                checked={filters.excludeWeekends || false}
                onChange={(e) => setFilters(prev => ({ ...prev, excludeWeekends: e.target.checked }))}
                className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
              />
              <span className="text-xs text-gray-600 flex items-center gap-1">
                <CalendarOff className="h-3 w-3" />
                Exclude weekends
              </span>
            </label>
          </div>

          {/* State Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              PR State
            </label>
            <div className="flex gap-1 flex-wrap">
              {states.map(state => (
                <button
                  key={state}
                  type="button"
                  onClick={() => handleStateToggle(state)}
                  className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
                    currentStates.includes(state)
                      ? state === 'MERGED' ? 'bg-purple-600 text-white'
                      : state === 'OPEN' ? 'bg-blue-600 text-white'
                      : 'bg-red-600 text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  {state}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Comparison Period */}
      {compareMode && (
        <div className="space-y-3 pt-4 border-t border-gray-200">
          <PeriodPresets onSelect={handlePresetSelect} />
          <p className="text-sm font-medium text-purple-600">Previous Period (for comparison)</p>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <Calendar className="inline h-4 w-4 mr-1" />
                Since Date
              </label>
              <input
                type="date"
                name="compareSinceDate"
                value={filters.compareSinceDate || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-purple-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-purple-500 text-sm"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <Calendar className="inline h-4 w-4 mr-1" />
                Until Date
              </label>
              <input
                type="date"
                name="compareUntilDate"
                value={filters.compareUntilDate || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-purple-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-purple-500 text-sm"
              />
            </div>
          </div>
        </div>
      )}

      {/* Repositories */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          <GitBranch className="inline h-4 w-4 mr-1" />
          Repositories
        </label>
        <div className="flex gap-2">
          <input
            type="text"
            value={repoInput}
            onChange={(e) => setRepoInput(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Add repository slug (e.g., my-repo)"
            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
          />
          <button
            type="button"
            onClick={addRepo}
            className="px-3 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
          >
            <Plus className="h-5 w-5" />
          </button>
        </div>
        {filters.repos?.length > 0 && (
          <div className="flex flex-wrap gap-2 mt-2">
            {filters.repos.map(repo => (
              <span
                key={repo}
                className="inline-flex items-center gap-1 px-2 py-1 bg-blue-100 text-blue-700 rounded-full text-sm"
              >
                {repo}
                <button
                  type="button"
                  onClick={() => removeRepo(repo)}
                  className="hover:text-blue-900"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Fetch Button */}
      <div className="flex justify-end">
        <button
          onClick={onFetch}
          disabled={loading || !filters.repos?.length}
          className="px-6 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors text-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
        >
          {loading ? (
            <>
              <span className="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full"></span>
              Loading...
            </>
          ) : (
            <>
              <Search className="h-4 w-4" />
              {compareMode ? 'Compare Stats' : 'Fetch Stats'}
            </>
          )}
        </button>
      </div>
    </div>
  )
}
