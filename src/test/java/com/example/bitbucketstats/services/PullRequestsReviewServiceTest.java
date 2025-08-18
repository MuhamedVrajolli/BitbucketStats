package com.example.bitbucketstats.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bitbucketstats.integration.BitBucketService;
import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.EnrichedPullRequest;
import com.example.bitbucketstats.models.FieldFilter;
import com.example.bitbucketstats.integration.response.User;
import com.example.bitbucketstats.controllers.request.PullRequestReviewParams;
import com.example.bitbucketstats.controllers.response.PullRequestCommentSummary;
import com.example.bitbucketstats.controllers.response.PullRequestReviewResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class PullRequestsReviewServiceTest {

  @InjectMocks
  private PullRequestsReviewService service;

  @Mock private BitBucketService bitBucketService;
  @Mock private ResponseAssembler responseAssembler;

  private PullRequestReviewParams baseParams() {
    var p = new PullRequestReviewParams();
    p.setWorkspace("acme");
    p.setRepo(List.of("svc-a", "svc-b"));
    p.setSinceDate(LocalDate.of(2025, 8, 1));
    p.setUntilDate(LocalDate.of(2025, 8, 10));
    p.setMaxConcurrency(4);
    return p;
  }

  private BitbucketAuth auth() {
    return new BitbucketAuth("tok", "alice", "pwd");
  }

  @Test
  void reviewerUuidProvided_noCurrentUser_lookup_includeCommentDetailsFalse() {
    var params = baseParams();
    params.setReviewerUuid("rev-uuid");
    params.setIncludeCommentDetails(false);

    var pr1 = mock(EnrichedPullRequest.class);
    var pr2 = mock(EnrichedPullRequest.class);

    when(bitBucketService.searchPullRequestsAcrossRepos(any(FieldFilter.class), anyList(), any(BitbucketAuth.class),
        same(params)))
        .thenReturn(Flux.just(pr1, pr2));

    var expected = mock(PullRequestReviewResponse.class);
    when(responseAssembler.toPullRequestReviewResponse(anyList(), same(params), anyString(), anyList(), isNull()))
        .thenReturn(expected);

    var mono = service.getReviewStats(auth(), params);

    StepVerifier.create(mono)
        .expectNext(expected)
        .verifyComplete();

    // verify no current-user call
    verify(bitBucketService, never()).getCurrentUser(any());

    // verify assembler got empty summaries & null total
    ArgumentCaptor<List<EnrichedPullRequest>> prsCap = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<String> reviewerUuidCap = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List<PullRequestCommentSummary>> sumsCap = ArgumentCaptor.forClass(List.class);

    verify(responseAssembler).toPullRequestReviewResponse(
        prsCap.capture(), same(params), reviewerUuidCap.capture(), sumsCap.capture(), isNull());

    assertThat(prsCap.getValue()).containsExactly(pr1, pr2);
    assertThat(reviewerUuidCap.getValue()).isEqualTo("rev-uuid");
    assertThat(sumsCap.getValue()).isEmpty();
  }

  @Test
  void reviewerUuidMissing_fallsBackToCurrentUser_includeCommentDetailsFalse() {
    var params = baseParams();
    params.setIncludeCommentDetails(false);

    var me = mock(User.class);
    when(me.uuid()).thenReturn("me-uuid");
    when(bitBucketService.getCurrentUser(any(BitbucketAuth.class))).thenReturn(Mono.just(me));

    var pr = mock(EnrichedPullRequest.class);
    when(bitBucketService.searchPullRequestsAcrossRepos(any(FieldFilter.class), anyList(), any(BitbucketAuth.class),
        same(params)))
        .thenReturn(Flux.just(pr));

    var expected = mock(PullRequestReviewResponse.class);
    when(responseAssembler.toPullRequestReviewResponse(anyList(), same(params), anyString(), anyList(), isNull()))
        .thenReturn(expected);

    var mono = service.getReviewStats(auth(), params);

    StepVerifier.create(mono)
        .expectNext(expected)
        .verifyComplete();

    // ensure order: get current user -> search
    InOrder inOrder = inOrder(bitBucketService);
    inOrder.verify(bitBucketService).getCurrentUser(any(BitbucketAuth.class));
    inOrder.verify(bitBucketService)
        .searchPullRequestsAcrossRepos(any(FieldFilter.class), anyList(), any(BitbucketAuth.class), same(params));

    // verify assembler reviewer UUID is me-uuid
    ArgumentCaptor<String> reviewerUuidCap = ArgumentCaptor.forClass(String.class);
    verify(responseAssembler).toPullRequestReviewResponse(anyList(), same(params), reviewerUuidCap.capture(), anyList(),
        isNull());
    assertThat(reviewerUuidCap.getValue()).isEqualTo("me-uuid");
  }

  @Test
  void includeCommentDetailsTrue_fetchesCounts_onlyForPositiveCommentCount_andFiltersZeroes() {
    var params = baseParams();
    params.setIncludeCommentDetails(true);

    // PRs with various commentCount values
    var pr0 = mock(EnrichedPullRequest.class);
    when(pr0.commentCount()).thenReturn(0); // should NOT trigger fetch
    var prA = mock(EnrichedPullRequest.class);
    when(prA.commentCount()).thenReturn(2);
    var prB = mock(EnrichedPullRequest.class);
    when(prB.commentCount()).thenReturn(5);

    when(prA.repo()).thenReturn("svc-a");
    when(prA.id()).thenReturn(100);
    when(prA.title()).thenReturn("Fix A");

    when(prB.repo()).thenReturn("svc-b");
    when(prB.id()).thenReturn(200);

    var me = mock(User.class);
    when(me.uuid()).thenReturn("me-uuid");
    when(bitBucketService.getCurrentUser(any(BitbucketAuth.class))).thenReturn(Mono.just(me));

    when(bitBucketService.searchPullRequestsAcrossRepos(any(FieldFilter.class), anyList(), any(BitbucketAuth.class),
        same(params)))
        .thenReturn(Flux.just(pr0, prA, prB));

    // Only PRs with commentCount()>0 get fetched; one returns 1, the other 0 (filtered out)
    when(
        bitBucketService.fetchMyCommentCount(any(BitbucketAuth.class), anyString(), anyString(), anyInt(), anyString()))
        .thenAnswer(inv -> {
          String repo = inv.getArgument(2);
          Integer id = inv.getArgument(3);
          if ("svc-a".equals(repo) && id == 100) {
            return Mono.just(1); // kept
          }
          if ("svc-b".equals(repo) && id == 200) {
            return Mono.just(0); // filtered out
          }
          return Mono.just(0);
        });

    // assemble response; capture summaries and totalComments
    ArgumentCaptor<List<PullRequestCommentSummary>> sumsCap = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<Integer> totalCap = ArgumentCaptor.forClass(Integer.class);
    var expected = mock(PullRequestReviewResponse.class);
    when(responseAssembler.toPullRequestReviewResponse(anyList(), same(params), anyString(), anyList(), any()))
        .thenReturn(expected);

    var mono = service.getReviewStats(auth(), params);

    StepVerifier.create(mono)
        .expectNext(expected)
        .verifyComplete();

    // verify counts fetched for prA and prB only (not pr0)
    verify(bitBucketService, never()).fetchMyCommentCount(any(), anyString(), isNull(), anyInt(),
        anyString()); // sanity
    verify(bitBucketService, times(2)).fetchMyCommentCount(any(BitbucketAuth.class), anyString(), anyString(), anyInt(),
        anyString());

    // verify assembler got exactly one summary (for prA) and total=1
    verify(responseAssembler).toPullRequestReviewResponse(anyList(), same(params), anyString(), sumsCap.capture(),
        totalCap.capture());
    var summaries = sumsCap.getValue();
    assertThat(summaries).hasSize(1);
    PullRequestCommentSummary s = summaries.get(0);
    assertThat(s.id()).isEqualTo(100);
    assertThat(s.commentsMade()).isEqualTo(1);
    assertThat(s.repo()).isEqualTo("svc-a");
    assertThat(totalCap.getValue()).isEqualTo(1);
  }

  @Test
  void includeCommentDetailsTrue_butNoPrs_earlyReturn_emptySummaries_nullTotal() {
    var params = baseParams();
    params.setIncludeCommentDetails(true);

    var me = mock(User.class);
    when(me.uuid()).thenReturn("me-uuid");
    when(bitBucketService.getCurrentUser(any(BitbucketAuth.class))).thenReturn(Mono.just(me));

    when(bitBucketService.searchPullRequestsAcrossRepos(any(FieldFilter.class), anyList(), any(BitbucketAuth.class),
        same(params)))
        .thenReturn(Flux.empty());

    var expected = mock(PullRequestReviewResponse.class);
    when(responseAssembler.toPullRequestReviewResponse(anyList(), same(params), anyString(), anyList(), isNull()))
        .thenReturn(expected);

    var mono = service.getReviewStats(auth(), params);

    StepVerifier.create(mono)
        .expectNext(expected)
        .verifyComplete();

    verify(bitBucketService, never()).fetchMyCommentCount(any(), anyString(), anyString(), anyInt(), anyString());

    ArgumentCaptor<List<EnrichedPullRequest>> prsCap = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List<PullRequestCommentSummary>> sumsCap = ArgumentCaptor.forClass(List.class);

    verify(responseAssembler).toPullRequestReviewResponse(prsCap.capture(), same(params), anyString(),
        sumsCap.capture(), isNull());
    assertThat(prsCap.getValue()).isEmpty();
    assertThat(sumsCap.getValue()).isEmpty();
  }
}
