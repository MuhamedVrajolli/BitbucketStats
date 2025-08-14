package com.example.bitbucketstats.models.response;

import static com.example.bitbucketstats.utils.GeneralUtils.round2;

import com.example.bitbucketstats.models.PullRequest;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PullRequestReviewResponse(
    String period,
    int totalApprovedPrs,
    int totalPrsReviewed,
    Integer totalPrsCommented,
    Integer totalComments,
    Double approvedPercentage,
    Double commentedPercentage,
    Map<String, PullRequestCommentSummary> prsCommented
) {
  public static PullRequestReviewResponse from(List<PullRequest> pullRequests, LocalDate since, LocalDate until,
      int approvedCount, int commentedPrs,
      int totalComments, Map<String, PullRequestCommentSummary> commentedMap) {
    int totalReviewed = pullRequests.size();
    double approvedPct = totalReviewed == 0 ? 0 : round2(approvedCount * 100.0 / totalReviewed);
    double commentedPct = totalReviewed == 0 ? 0 : round2(commentedPrs * 100.0 / totalReviewed);
    return new PullRequestReviewResponse(
        String.format("FROM: %s TO: %s", since, until),
        approvedCount,
        totalReviewed,
        commentedMap.isEmpty() ? null : commentedPrs,
        commentedMap.isEmpty() ? null : totalComments,
        approvedPct,
        commentedMap.isEmpty() ? null : commentedPct,
        commentedMap.isEmpty() ? null : commentedMap
    );
  }
}
