package com.example.bitbucketstats.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.example.bitbucketstats.models.DiffDetails;
import com.example.bitbucketstats.models.EnrichedPullRequest;
import com.example.bitbucketstats.controllers.request.MyPullRequestsParams;
import com.example.bitbucketstats.controllers.request.PullRequestReviewParams;
import com.example.bitbucketstats.controllers.response.PullRequestCommentSummary;
import com.example.bitbucketstats.utils.PullRequestUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ResponseAssemblerTest {

  private final ResponseAssembler assembler = new ResponseAssembler();

  private PullRequestReviewParams reviewParams(LocalDate since, LocalDate until) {
    var p = new PullRequestReviewParams();
    p.setWorkspace("acme");
    p.setRepo(List.of("svc-a", "svc-b"));
    p.setSinceDate(since);
    p.setUntilDate(until);
    return p;
  }

  private MyPullRequestsParams myParams(LocalDate since, LocalDate until,
      boolean includeDiffs, boolean includeDetails) {
    var p = new MyPullRequestsParams();
    p.setWorkspace("acme");
    p.setRepo(List.of("svc-a", "svc-b"));
    p.setSinceDate(since);
    p.setUntilDate(until);
    p.setIncludeDiffDetails(includeDiffs);
    p.setIncludePullRequestDetails(includeDetails);
    return p;
  }

  @Test
  void toPullRequestReviewResponse_noComments_setsNullCommentFields_andComputesApprovedCount() {
    var since = LocalDate.of(2025, 8, 1);
    var until = LocalDate.of(2025, 8, 10);
    var params = reviewParams(since, until);
    var reviewerUuid = "rev-uuid";

    var pr1 = mock(EnrichedPullRequest.class);
    when(pr1.approvedBy(reviewerUuid)).thenReturn(true);

    var pr2 = mock(EnrichedPullRequest.class);
    when(pr2.approvedBy(reviewerUuid)).thenReturn(false);

    var resp = assembler.toPullRequestReviewResponse(
        List.of(pr1, pr2),
        params,
        reviewerUuid,
        null,
        null
    );

    assertThat(resp).isNotNull();
    // Basic shape
    assertThat(resp.totalPullRequestsReviewed()).isEqualTo(2);
    assertThat(resp.totalPullRequestsApproved()).isEqualTo(1);

    // No comments provided → these should be null (per implementation)
    assertThat(resp.pullRequestsCommented()).isNull();
    assertThat(resp.totalComments()).isNull();
    assertThat(resp.commentedPercentage()).isNull();

    // Approved percentage should be > 0 and <= 100 (don’t lock exact rounding)
    assertThat(resp.approvedPercentage()).isNotNull();
    assertThat(resp.approvedPercentage()).isBetween(0.0, 100.0);
    // Period should contain both dates (we don’t assert exact formatting)
    assertThat(resp.period()).contains("2025-08-01").contains("2025-08-10");
  }

  @Test
  void toPullRequestReviewResponse_withComments_populatesCounts_andPercentagesPresent() {
    var since = LocalDate.of(2025, 8, 1);
    var until = LocalDate.of(2025, 8, 10);
    var params = reviewParams(since, until);
    var reviewerUuid = "rev-uuid";

    var pr1 = mock(EnrichedPullRequest.class);
    when(pr1.approvedBy(reviewerUuid)).thenReturn(true);
    var pr2 = mock(EnrichedPullRequest.class);
    when(pr2.approvedBy(reviewerUuid)).thenReturn(true);
    var pr3 = mock(EnrichedPullRequest.class);
    when(pr3.approvedBy(reviewerUuid)).thenReturn(false);
    var prs = List.of(pr1, pr2, pr3);

    var s1 = mock(PullRequestCommentSummary.class);
    var s2 = mock(PullRequestCommentSummary.class);
    var commentedList = List.of(s1, s2);

    var totalComments = 7;

    var resp = assembler.toPullRequestReviewResponse(
        prs, params, reviewerUuid, commentedList, totalComments
    );

    assertThat(resp.totalPullRequestsReviewed()).isEqualTo(3);
    assertThat(resp.totalPullRequestsApproved()).isEqualTo(2);
    assertThat(resp.totalPullRequestsCommented()).isEqualTo(2);
    assertThat(resp.totalComments()).isEqualTo(7);
    assertThat(resp.approvedPercentage()).isNotNull();
    assertThat(resp.commentedPercentage()).isNotNull();
    assertThat(resp.pullRequestsCommented()).containsExactly(s1, s2);
  }

  // ---------- toMyPullRequestsResponse ----------

  @Test
  void toMyPullRequestsResponse_noDiffs_noDetails_computesAveragesFromHoursAndComments() {
    var since = LocalDate.of(2025, 8, 1);
    var until = LocalDate.of(2025, 8, 10);
    var params = myParams(since, until, /*includeDiffs*/ false, /*includeDetails*/ false);

    var pr1 = mock(EnrichedPullRequest.class);
    when(pr1.commentCount()).thenReturn(1);
    var pr2 = mock(EnrichedPullRequest.class);
    when(pr2.commentCount()).thenReturn(2);
    var pr3 = mock(EnrichedPullRequest.class);
    when(pr3.commentCount()).thenReturn(null); // safeInt(null) should be handled in implementation

    try (MockedStatic<PullRequestUtils> utils = mockStatic(PullRequestUtils.class)) {
      utils.when(() -> PullRequestUtils.hoursOpen(pr1)).thenReturn(10L);
      utils.when(() -> PullRequestUtils.hoursOpen(pr2)).thenReturn(20L);
      utils.when(() -> PullRequestUtils.hoursOpen(pr3)).thenReturn(30L);

      var resp = assembler.toMyPullRequestsResponse(
          List.of(pr1, pr2, pr3),
          params,
          Map.of() // diffs ignored because includeDiffs=false
      );

      assertThat(resp).isNotNull();
      assertThat(resp.totalPullRequests()).isEqualTo(3);

      // Average hours: (10+20+30)/3 = 20.0
      assertThat(resp.avgTimeOpenHours()).isNotNull();
      assertThat(resp.avgTimeOpenHours()).isEqualTo(20.0);

      // Average comments: (1+2+safeInt(null=0))/3 = 1.0 (don’t depend on rounding beyond 1 decimal)
      assertThat(resp.avgCommentCount()).isNotNull();
      assertThat(resp.avgCommentCount()).isEqualTo(1.0);

      // No diffs requested
      assertThat(resp.avgFilesChanged()).isNull();
      // No details requested
      assertThat(resp.pullRequestDetails()).isNull();

      assertThat(resp.period()).contains("2025-08-01").contains("2025-08-10");
    }
  }

  @Test
  void toMyPullRequestsResponse_diffsEnabled_detailsOff_setsAverageFilesChanged() {
    var since = LocalDate.of(2025, 8, 1);
    var until = LocalDate.of(2025, 8, 10);
    var params = myParams(since, until, /*includeDiffs*/ true, /*includeDetails*/ false);

    var pr1 = mock(EnrichedPullRequest.class);
    var pr2 = mock(EnrichedPullRequest.class);

    var d1 = mock(DiffDetails.class);
    when(d1.filesChanged()).thenReturn(3);
    var d2 = mock(DiffDetails.class);
    when(d2.filesChanged()).thenReturn(5);

    // Keys don’t matter for avgFilesChanged because only values are summed
    var diffs = Map.of("k1", d1, "k2", d2);

    try (MockedStatic<PullRequestUtils> utils = mockStatic(PullRequestUtils.class)) {
      utils.when(() -> PullRequestUtils.hoursOpen(pr1)).thenReturn(0L);
      utils.when(() -> PullRequestUtils.hoursOpen(pr2)).thenReturn(0L);

      var resp = assembler.toMyPullRequestsResponse(
          List.of(pr1, pr2),
          params,
          diffs
      );

      assertThat(resp.totalPullRequests()).isEqualTo(2);
      // (3 + 5) / 2 = 4.0
      assertThat(resp.avgFilesChanged()).isEqualTo(4.0);
      assertThat(resp.pullRequestDetails()).isNull(); // details off
    }
  }

  @Test
  void toMyPullRequestsResponse_detailsOn_noDiffs_buildsSummaries_withoutDiffDetail() {
    var since = LocalDate.of(2025, 8, 1);
    var until = LocalDate.of(2025, 8, 10);
    var params = myParams(since, until, /*includeDiffs*/ false, /*includeDetails*/ true);

    var pr1 = mock(EnrichedPullRequest.class);
    when(pr1.id()).thenReturn(101);
    when(pr1.title()).thenReturn("Feat A");
    when(pr1.repo()).thenReturn("svc-a");
    when(pr1.commentCount()).thenReturn(4);

    var pr2 = mock(EnrichedPullRequest.class);
    when(pr2.id()).thenReturn(202);
    when(pr2.title()).thenReturn("Fix B");
    when(pr2.repo()).thenReturn("svc-b");
    when(pr2.commentCount()).thenReturn(0);

    try (MockedStatic<PullRequestUtils> utils = mockStatic(PullRequestUtils.class)) {
      utils.when(() -> PullRequestUtils.hoursOpen(pr1)).thenReturn(12L);
      utils.when(() -> PullRequestUtils.hoursOpen(pr2)).thenReturn(8L);

      var resp = assembler.toMyPullRequestsResponse(
          List.of(pr1, pr2),
          params,
          Map.of() // no diffs
      );

      assertThat(resp.pullRequestDetails()).isNotNull();
      assertThat(resp.pullRequestDetails()).hasSize(2);

      var s1 = resp.pullRequestDetails().get(0);
      var s2 = resp.pullRequestDetails().get(1);

      // We don’t assert exact link formatting; only core fields and that diff detail is null
      assertThat(s1.id()).isEqualTo(101);
      assertThat(s1.title()).isEqualTo("Feat A");
      assertThat(s1.repo()).isEqualTo("svc-a");
      assertThat(s1.commentCount()).isEqualTo(4);
      assertThat(s1.diffDetails()).isNull();

      assertThat(s2.id()).isEqualTo(202);
      assertThat(s2.title()).isEqualTo("Fix B");
      assertThat(s2.repo()).isEqualTo("svc-b");
      assertThat(s2.commentCount()).isEqualTo(0);
      assertThat(s2.diffDetails()).isNull();

      // Hours in summaries are ints cast from PullRequestUtils.hoursOpen
      assertThat(s1.timeOpenHours()).isEqualTo(12);
      assertThat(s2.timeOpenHours()).isEqualTo(8);
    }
  }
}
