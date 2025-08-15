package com.example.bitbucketstats.services;

import static com.example.bitbucketstats.utils.GeneralUtils.avg;
import static com.example.bitbucketstats.utils.GeneralUtils.pct;
import static com.example.bitbucketstats.utils.GeneralUtils.period;
import static com.example.bitbucketstats.utils.GeneralUtils.safeInt;
import static com.example.bitbucketstats.utils.PullRequestUtils.prKey;
import static com.example.bitbucketstats.utils.PullRequestUtils.prLink;

import com.example.bitbucketstats.models.DiffDetails;
import com.example.bitbucketstats.models.EnrichedPullRequest;
import com.example.bitbucketstats.models.request.MyPullRequestsParams;
import com.example.bitbucketstats.models.request.PullRequestReviewParams;
import com.example.bitbucketstats.models.response.MyPullRequestsResponse;
import com.example.bitbucketstats.models.response.MyPullRequestsSummary;
import com.example.bitbucketstats.models.response.PullRequestCommentSummary;
import com.example.bitbucketstats.models.response.PullRequestReviewResponse;
import com.example.bitbucketstats.utils.PullRequestUtils;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ResponseAssembler {

  public PullRequestReviewResponse toPullRequestReviewResponse(
      List<EnrichedPullRequest> prs,
      PullRequestReviewParams params,
      String reviewerUuid,
      @Nullable List<PullRequestCommentSummary> commentedList,
      @Nullable Integer totalComments
  ) {
    int totalReviewed = prs.size();
    int approvedCount = (int) prs.stream().filter(pr -> pr.approvedBy(reviewerUuid)).count();

    boolean hasComments = commentedList != null && !commentedList.isEmpty();
    int commentedPrs = hasComments ? commentedList.size() : 0;

    double approvedPct  = pct(approvedCount, totalReviewed);
    Double commentedPct = hasComments ? pct(commentedPrs, totalReviewed) : null;

    return new PullRequestReviewResponse(
        period(params.getSinceDate(), params.getUntilDate()),
        approvedCount,
        totalReviewed,
        hasComments ? commentedPrs : null,
        hasComments ? totalComments : null,
        approvedPct,
        commentedPct,
        hasComments ? commentedList : null
    );
  }

  public MyPullRequestsResponse toMyPullRequestsResponse(
      List<EnrichedPullRequest> prs,
      MyPullRequestsParams params,
      Map<String, DiffDetails> diffsByKey // empty if not requested
  ) {
    int total = prs.size();

    long sumHours = prs.stream().mapToLong(PullRequestUtils::hoursOpen).sum();
    long sumComments = prs.stream().mapToLong(pr -> safeInt(pr.commentCount())).sum();

    Double avgFilesChanged = null;
    if (params.isIncludeDiffDetails()) {
      int totalFilesChanged = diffsByKey.values().stream().mapToInt(DiffDetails::filesChanged).sum();
      avgFilesChanged = avg(totalFilesChanged, total);
    }

    List<MyPullRequestsSummary> details = params.isIncludePullRequestDetails()
        ? buildMyPullRequestsSummaries(prs, params.getWorkspace(), params.isIncludeDiffDetails(), diffsByKey)
        : null;

    return new MyPullRequestsResponse(
        period(params.getSinceDate(), params.getUntilDate()),
        total,
        avg(sumHours, total),
        avg(sumComments, total),
        avgFilesChanged,
        details
    );
  }

  private List<MyPullRequestsSummary> buildMyPullRequestsSummaries(
      List<EnrichedPullRequest> prs, String workspace, boolean includeDiffs, Map<String, DiffDetails> diffsByKey) {

    return prs.stream()
        .map(pr -> new MyPullRequestsSummary(
            pr.id(),
            pr.title(),
            prLink(workspace, pr.repo(), pr.id()),
            (int) PullRequestUtils.hoursOpen(pr),
            safeInt(pr.commentCount()),
            pr.repo(),
            includeDiffs ? diffsByKey.get(prKey(pr)) : null
        ))
        .toList();
  }
}
