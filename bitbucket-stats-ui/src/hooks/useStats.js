import { useQuery } from '@tanstack/react-query'
import { fetchMyPRStats, fetchReviewStats } from '../api/bitbucketApi'

export function useMyPRStats(credentials, params, enabled = true) {
  return useQuery({
    // Use a stable key with stringified params to prevent unwanted refetches
    queryKey: ['myPRStats', JSON.stringify(params)],
    queryFn: () => fetchMyPRStats(credentials, params),
    enabled: enabled && !!credentials && !!params.workspace && params.repos?.length > 0 && !!params.sinceDate,
    staleTime: Infinity, // Don't consider data stale automatically
    gcTime: 30 * 60 * 1000, // Keep in cache for 30 minutes
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  })
}

export function useReviewStats(credentials, params, enabled = true) {
  return useQuery({
    queryKey: ['reviewStats', JSON.stringify(params)],
    queryFn: () => fetchReviewStats(credentials, params),
    enabled: enabled && !!credentials && !!params.workspace && params.repos?.length > 0 && !!params.sinceDate,
    staleTime: Infinity,
    gcTime: 30 * 60 * 1000,
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  })
}
