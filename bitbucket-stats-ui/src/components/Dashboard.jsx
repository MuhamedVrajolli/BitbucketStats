import { useState, useCallback, useEffect, useMemo } from 'react'
import { BarChart3, AlertTriangle } from 'lucide-react'
import AuthForm from './AuthForm'
import FilterBar from './FilterBar'
import MyPRsSection from './MyPRsSection'
import ReviewSection from './ReviewSection'
import PRDetailsList from './PRDetailsList'
import ComparisonSummary from './ComparisonSummary'
import ExportButton from './ExportButton'
import { useMyPRStats, useReviewStats } from '../hooks/useStats'
import { filterPRStats } from '../utils/filterStats'

const FILTERS_STORAGE_KEY = 'bitbucket-stats-filters'

const getDefaultFilters = () => {
  const today = new Date()
  const thirtyDaysAgo = new Date(today)
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)

  return {
    sinceDate: thirtyDaysAgo.toISOString().split('T')[0],
    untilDate: today.toISOString().split('T')[0],
    state: ['MERGED'],
    repos: [],
    nickname: '',
    includePullRequestDetails: true,
    compareSinceDate: '',
    compareUntilDate: '',
    maxDaysOpen: '',
    excludeWeekends: false,
  }
}

const loadFilters = () => {
  const saved = localStorage.getItem(FILTERS_STORAGE_KEY)
  if (saved) {
    try {
      return { ...getDefaultFilters(), ...JSON.parse(saved) }
    } catch {
      return getDefaultFilters()
    }
  }
  return getDefaultFilters()
}

export default function Dashboard({ credentials, setCredentials }) {
  const [filters, setFilters] = useState(loadFilters)
  const [shouldFetch, setShouldFetch] = useState(false)
  const [compareMode, setCompareMode] = useState(() => {
    const saved = localStorage.getItem('bitbucket-stats-compare-mode')
    return saved === 'true'
  })

  // Save filters to localStorage whenever they change
  useEffect(() => {
    localStorage.setItem(FILTERS_STORAGE_KEY, JSON.stringify(filters))
  }, [filters])

  // Save compare mode to localStorage
  useEffect(() => {
    localStorage.setItem('bitbucket-stats-compare-mode', compareMode.toString())
  }, [compareMode])

  const queryParams = {
    workspace: credentials?.workspace,
    ...filters,
  }

  const compareQueryParams = {
    workspace: credentials?.workspace,
    ...filters,
    sinceDate: filters.compareSinceDate,
    untilDate: filters.compareUntilDate,
  }

  // Current period queries
  const {
    data: prData,
    isLoading: prLoading,
    error: prError,
    refetch: refetchPR
  } = useMyPRStats(credentials, queryParams, shouldFetch)

  const {
    data: reviewData,
    isLoading: reviewLoading,
    error: reviewError,
    refetch: refetchReview
  } = useReviewStats(credentials, queryParams, shouldFetch)

  // Comparison period queries
  const shouldFetchCompare = shouldFetch && compareMode && filters.compareSinceDate && filters.compareUntilDate

  const {
    data: comparePrData,
    isLoading: comparePrLoading,
    refetch: refetchComparePR
  } = useMyPRStats(credentials, compareQueryParams, shouldFetchCompare)

  const {
    data: compareReviewData,
    isLoading: compareReviewLoading,
    refetch: refetchCompareReview
  } = useReviewStats(credentials, compareQueryParams, shouldFetchCompare)

  const handleFetch = useCallback(() => {
    if (!credentials || !filters.repos?.length) return
    setShouldFetch(true)
    setTimeout(() => {
      refetchPR()
      refetchReview()
      if (compareMode && filters.compareSinceDate && filters.compareUntilDate) {
        refetchComparePR()
        refetchCompareReview()
      }
    }, 0)
  }, [credentials, filters, compareMode, refetchPR, refetchReview, refetchComparePR, refetchCompareReview])

  const loading = prLoading || reviewLoading || (compareMode && (comparePrLoading || compareReviewLoading))
  const showComparison = compareMode && comparePrData && compareReviewData

  // Filter PR data to exclude stale PRs and/or weekends
  const maxDaysOpen = filters.maxDaysOpen ? parseInt(filters.maxDaysOpen, 10) : null
  const excludeWeekends = filters.excludeWeekends || false
  const filteredPrData = useMemo(() => filterPRStats(prData, maxDaysOpen, excludeWeekends), [prData, maxDaysOpen, excludeWeekends])
  const filteredComparePrData = useMemo(() => filterPRStats(comparePrData, maxDaysOpen, excludeWeekends), [comparePrData, maxDaysOpen, excludeWeekends])

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-600 rounded-lg">
              <BarChart3 className="h-6 w-6 text-white" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-gray-900">Bitbucket Stats Dashboard</h1>
              <p className="text-sm text-gray-500">Developer performance metrics</p>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        {/* Auth Form */}
        <AuthForm credentials={credentials} setCredentials={setCredentials} />

        {/* Filter Bar */}
        {credentials && (
          <FilterBar
            filters={filters}
            setFilters={setFilters}
            onFetch={handleFetch}
            loading={loading}
            compareMode={compareMode}
            setCompareMode={setCompareMode}
          />
        )}

        {/* Stats Sections */}
        {credentials && shouldFetch && (
          <>
            {/* Filter Notice */}
            {filteredPrData?._filtered && (filteredPrData._excludedCount > 0 || filteredPrData._excludeWeekends) && (
              <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 flex items-center gap-2 text-amber-800">
                <AlertTriangle className="h-5 w-5 flex-shrink-0" />
                <span className="text-sm">
                  {filteredPrData._excludedCount > 0 && (
                    <>Excluded <strong>{filteredPrData._excludedCount}</strong> stale PRs (open &gt; {maxDaysOpen} days). </>
                  )}
                  {filteredPrData._excludeWeekends && (
                    <>Weekends excluded from time calculations. </>
                  )}
                  Showing {filteredPrData._filteredCount} of {filteredPrData._originalCount} PRs.
                </span>
              </div>
            )}

            {/* Export and Summary */}
            <div className="flex items-center justify-between">
              <div></div>
              <ExportButton
                prData={filteredPrData}
                reviewData={reviewData}
                comparePrData={showComparison ? filteredComparePrData : null}
                compareReviewData={showComparison ? compareReviewData : null}
                compareMode={showComparison}
              />
            </div>

            {/* Comparison Summary */}
            {showComparison && (
              <ComparisonSummary
                prData={filteredPrData}
                comparePrData={filteredComparePrData}
                reviewData={reviewData}
                compareReviewData={compareReviewData}
              />
            )}

            <MyPRsSection
              data={filteredPrData}
              compareData={showComparison ? filteredComparePrData : null}
              loading={prLoading}
              error={prError}
              compareMode={showComparison}
            />

            <ReviewSection
              data={reviewData}
              compareData={showComparison ? compareReviewData : null}
              loading={reviewLoading}
              error={reviewError}
              compareMode={showComparison}
            />

            {/* PR Details List */}
            {filteredPrData?.pull_request_details && (
              <PRDetailsList
                pullRequests={filteredPrData.pull_request_details}
                loading={prLoading}
              />
            )}
          </>
        )}

        {/* Empty State */}
        {!credentials && (
          <div className="text-center py-12">
            <BarChart3 className="h-12 w-12 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900">Connect to Bitbucket</h3>
            <p className="text-gray-500 mt-1">
              Enter your credentials above to start viewing your PR statistics
            </p>
          </div>
        )}

        {credentials && !shouldFetch && (
          <div className="text-center py-12">
            <BarChart3 className="h-12 w-12 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900">Configure Filters</h3>
            <p className="text-gray-500 mt-1">
              Add repositories and click "Fetch Stats" to load your metrics
            </p>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-gray-200 bg-white mt-auto">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <p className="text-sm text-gray-500 text-center">
            Bitbucket Stats Dashboard - Developer Performance Metrics
          </p>
        </div>
      </footer>
    </div>
  )
}
