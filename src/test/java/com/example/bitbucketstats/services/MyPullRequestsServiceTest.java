package com.example.bitbucketstats.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bitbucketstats.integration.BitBucketService;
import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.DiffDetails;
import com.example.bitbucketstats.models.EnrichedPullRequest;
import com.example.bitbucketstats.models.FieldFilter;
import com.example.bitbucketstats.integration.response.User;
import com.example.bitbucketstats.controllers.request.MyPullRequestsParams;
import com.example.bitbucketstats.controllers.response.MyPullRequestsResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class MyPullRequestsServiceTest {

  @InjectMocks
  private MyPullRequestsService service;

  @Mock
  private BitBucketService bitBucketService;

  @Mock
  private ResponseAssembler responseAssembler;

  private MyPullRequestsParams baseParams(boolean includeDiffs) {
    var p = new MyPullRequestsParams();
    p.setWorkspace("acme");
    p.setRepo(List.of("svc-a", "svc-b"));
    p.setSinceDate(LocalDate.of(2025, 8, 1));
    p.setUntilDate(LocalDate.of(2025, 8, 10));
    p.setMaxConcurrency(4);
    p.setIncludeDiffDetails(includeDiffs);
    return p;
  }

  private BitbucketAuth auth(String username) {
    return new BitbucketAuth("tok", username, "pwd");
  }

  @BeforeEach
  void resetAll() {
    Mockito.reset(bitBucketService, responseAssembler);
  }

  @Test
  void nickname_precedence_noDiffs() {
    var params = baseParams(false);
    params.setNickname("nick");

    // one PR instance (mock); service will read repo() and id() ONLY when diffs=true (here it's false)
    var pr = mock(EnrichedPullRequest.class);

    when(bitBucketService.searchPullRequestsAcrossRepos(any(FieldFilter.class), eq(params.getRepo()),
        any(BitbucketAuth.class), eq(params)))
        .thenReturn(Flux.just(pr));

    var expected = mock(MyPullRequestsResponse.class);
    when(responseAssembler.toMyPullRequestsResponse(List.of(pr), params, Map.of()))
        .thenReturn(expected);

    var mono = service.getMyPullRequestsStats(auth("ignoredUser"), params);

    StepVerifier.create(mono)
        .expectNext(expected)
        .verifyComplete();

    // Nickname path should not hit current user
    verify(bitBucketService, never()).getCurrentUser(any());
    // No diffs when flag is false
    verify(bitBucketService, never()).fetchDiffFilesChanged(any(), anyString(), anyString(), anyInt());
  }

  @Test
  void username_used_when_noNickname() {
    var params = baseParams(false);

    var pr = mock(EnrichedPullRequest.class);
    when(bitBucketService.searchPullRequestsAcrossRepos(any(FieldFilter.class), eq(params.getRepo()),
        any(BitbucketAuth.class), eq(params)))
        .thenReturn(Flux.just(pr));

    var expected = mock(MyPullRequestsResponse.class);
    when(responseAssembler.toMyPullRequestsResponse(eq(List.of(pr)), eq(params), anyMap()))
        .thenReturn(expected);

    var mono = service.getMyPullRequestsStats(auth("alice"), params);

    StepVerifier.create(mono)
        .expectNext(expected)
        .verifyComplete();

    verify(bitBucketService, never()).getCurrentUser(any());
    verify(bitBucketService, never()).fetchDiffFilesChanged(any(), anyString(), anyString(), anyInt());
  }

  @Test
  void falls_back_to_currentUser_when_noNickname_and_noUsername() {
    var params = baseParams(false);

    var me = mock(User.class);
    when(bitBucketService.getCurrentUser(any(BitbucketAuth.class))).thenReturn(Mono.just(me));

    var pr = mock(EnrichedPullRequest.class);
    when(bitBucketService.searchPullRequestsAcrossRepos(any(FieldFilter.class), eq(params.getRepo()),
        any(BitbucketAuth.class), eq(params)))
        .thenReturn(Flux.just(pr));

    when(responseAssembler.toMyPullRequestsResponse(eq(List.of(pr)), eq(params), anyMap()))
        .thenReturn(mock(MyPullRequestsResponse.class));

    var mono = service.getMyPullRequestsStats(auth(null), params);

    StepVerifier.create(mono)
        .expectNextCount(1)
        .verifyComplete();

    InOrder inOrder = inOrder(bitBucketService);
    inOrder.verify(bitBucketService).getCurrentUser(any(BitbucketAuth.class));
    inOrder.verify(bitBucketService)
        .searchPullRequestsAcrossRepos(any(FieldFilter.class), eq(params.getRepo()), any(BitbucketAuth.class),
            eq(params));
  }

  @Test
  @SuppressWarnings("unchecked")
  void includeDiffDetails_true_fetches_diffs_for_each_pr_and_passes_map_to_assembler() {
    var params = baseParams(true);

    // Two PRs that must expose repo() and id() (Mockito can mock records/final if mockito-inline is on classpath)
    var pr1 = mock(EnrichedPullRequest.class);
    when(pr1.repo()).thenReturn("svc-a");
    when(pr1.id()).thenReturn(100);

    var pr2 = mock(EnrichedPullRequest.class);
    when(pr2.repo()).thenReturn("svc-b");
    when(pr2.id()).thenReturn(200);

    when(bitBucketService.searchPullRequestsAcrossRepos(any(FieldFilter.class), eq(params.getRepo()),
        any(BitbucketAuth.class), eq(params)))
        .thenReturn(Flux.just(pr1, pr2));

    var dd1 = mock(DiffDetails.class);
    var dd2 = mock(DiffDetails.class);

    when(bitBucketService.fetchDiffFilesChanged(any(BitbucketAuth.class), eq("acme"), eq("svc-a"), eq(100)))
        .thenReturn(Mono.just(dd1));
    when(bitBucketService.fetchDiffFilesChanged(any(BitbucketAuth.class), eq("acme"), eq("svc-b"), eq(200)))
        .thenReturn(Mono.just(dd2));

    ArgumentCaptor<Map<String, DiffDetails>> diffsCap = ArgumentCaptor.forClass(Map.class);
    var expected = mock(MyPullRequestsResponse.class);
    when(responseAssembler.toMyPullRequestsResponse(eq(List.of(pr1, pr2)), eq(params), anyMap()))
        .thenReturn(expected);

    var mono = service.getMyPullRequestsStats(auth("alice"), params);

    StepVerifier.create(mono)
        .expectNext(expected)
        .verifyComplete();

    // verify each diff call
    verify(bitBucketService).fetchDiffFilesChanged(any(BitbucketAuth.class), eq("acme"), eq("svc-a"), eq(100));
    verify(bitBucketService).fetchDiffFilesChanged(any(BitbucketAuth.class), eq("acme"), eq("svc-b"), eq(200));

    // capture and assert the assembled map contains our values (keys are internal; we assert size & values)
    verify(responseAssembler).toMyPullRequestsResponse(eq(List.of(pr1, pr2)), eq(params), diffsCap.capture());
    Map<String, DiffDetails> diffs = diffsCap.getValue();
    assertThat(diffs).hasSize(2);
    assertThat(diffs.values()).containsExactlyInAnyOrder(dd1, dd2);
  }

  @Test
  void includeDiffDetails_true_but_no_prs_skips_diff_fetch_and_passes_empty_map() {
    var params = baseParams(true);

    when(bitBucketService.searchPullRequestsAcrossRepos(any(FieldFilter.class), eq(params.getRepo()),
        any(BitbucketAuth.class), eq(params)))
        .thenReturn(Flux.empty());

    var expected = mock(MyPullRequestsResponse.class);
    when(responseAssembler.toMyPullRequestsResponse(List.of(), params, Map.of()))
        .thenReturn(expected);

    var mono = service.getMyPullRequestsStats(auth("alice"), params);

    StepVerifier.create(mono)
        .expectNext(expected)
        .verifyComplete();

    verify(bitBucketService, never()).fetchDiffFilesChanged(any(), anyString(), anyString(), anyInt());
    verify(responseAssembler).toMyPullRequestsResponse(List.of(), params, Map.of());
  }
}
