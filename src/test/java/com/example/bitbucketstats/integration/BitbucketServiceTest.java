package com.example.bitbucketstats.integration;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.EnrichedPullRequest;
import com.example.bitbucketstats.models.FieldFilter;
import com.example.bitbucketstats.models.bitbucket.Comment;
import com.example.bitbucketstats.models.bitbucket.DiffStat;
import com.example.bitbucketstats.models.bitbucket.PullRequest;
import com.example.bitbucketstats.models.bitbucket.User;
import com.example.bitbucketstats.models.page.PullRequestPage;
import com.example.bitbucketstats.models.request.BaseParams;
import com.example.bitbucketstats.utils.PullRequestUtils;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BitbucketServiceTest {

  @Mock
  private BitbucketClient bitbucketClient;

  @InjectMocks
  private BitBucketService service;

  @Test
  void getCurrentUser_callsRetrieveJson_withExpectedUrl_andReturnsUser() {
    var auth = new BitbucketAuth("tok", "alice", "pwd");
    var user = mock(User.class);

    when(bitbucketClient.retrieveJson(any(), anyString(), any()))
        .thenReturn(Mono.just(user));

    var result = service.getCurrentUser(auth);

    StepVerifier.create(result)
        .expectNext(user)
        .verifyComplete();

    ArgumentCaptor<BitbucketAuth> aCap = ArgumentCaptor.forClass(BitbucketAuth.class);
    ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Class<?>> clsCap = ArgumentCaptor.forClass(Class.class);

    verify(bitbucketClient).retrieveJson(aCap.capture(), urlCap.capture(), clsCap.capture());
    assertThat(aCap.getValue()).isSameAs(auth);
    assertThat(urlCap.getValue()).endsWith("/user");
    assertThat(clsCap.getValue()).isEqualTo(User.class);
  }

  @Test
  void searchPullRequestsByFilter_buildsUrl_callsFetchAll_andMapsToEnrichedPR() {
    var auth = new BitbucketAuth("tok", "alice", "pwd");

    var params = new BaseParams();
    params.setWorkspace("acme");
    params.setRepo(List.of("svc-a"));
    params.setSinceDate(LocalDate.of(2025, 8, 1));
    params.setUntilDate(LocalDate.of(2025, 8, 10));
    params.setState(List.of("OPEN", "MERGED"));
    params.setQueued(true);

    var filter = FieldFilter.of(FieldFilter.AUTHOR_USERNAME, "alice");

    var page1 = mock(PullRequest.class);
    var page2 = mock(PullRequest.class);

    // fetchAll emits "pages" which are mapped via EnrichedPullRequest.from(page, repo)
    when(bitbucketClient.fetchAll(any(), anyString(), any()))
        .thenReturn(Flux.just(page1, page2));

    var pr1 = mock(EnrichedPullRequest.class);
    var pr2 = mock(EnrichedPullRequest.class);

    try (MockedStatic<EnrichedPullRequest> mocked = mockStatic(EnrichedPullRequest.class)) {
      // Map each page to the repo "svc-a"
      mocked.when(() -> EnrichedPullRequest.from(page1, "svc-a")).thenReturn(pr1);
      mocked.when(() -> EnrichedPullRequest.from(page2, "svc-a")).thenReturn(pr2);

      var flux = service.searchPullRequestsByFilter(filter, "svc-a", auth, params);

      StepVerifier.create(flux.collectList())
          .expectNext(List.of(pr1, pr2))
          .verifyComplete();
    }

    ArgumentCaptor<BitbucketAuth> aCap = ArgumentCaptor.forClass(BitbucketAuth.class);
    ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Class<PullRequestPage>> clsCap = ArgumentCaptor.forClass(Class.class);

    verify(bitbucketClient).fetchAll(aCap.capture(), urlCap.capture(), clsCap.capture());
    assertThat(aCap.getValue()).isSameAs(auth);
    String url = urlCap.getValue();
    assertThat(url).contains("/repositories/acme/svc-a/pullrequests")
        .contains("q=").contains("pagelen=50").contains("fields=").contains("2025-08-01").contains("2025-08-10");
    assertThat(clsCap.getValue()).isEqualTo(PullRequestPage.class);
  }

  @Test
  void searchPullRequestsAcrossRepos_mergesRepos_andDistinctsByPrKey() {
    var auth = new BitbucketAuth("tok", "alice", "pwd");

    var params = new BaseParams();
    params.setWorkspace("acme");
    params.setRepo(List.of("svc-a", "svc-b"));
    params.setSinceDate(LocalDate.of(2025, 8, 1));
    params.setUntilDate(LocalDate.of(2025, 8, 10));
    params.setMaxConcurrency(4);

    var filter = FieldFilter.of(FieldFilter.AUTHOR_USERNAME, "alice");

    var spySvc = Mockito.spy(service);

    var pra1 = mock(EnrichedPullRequest.class);
    var pra2 = mock(EnrichedPullRequest.class);
    var prb1Dup = mock(EnrichedPullRequest.class);

    try (MockedStatic<PullRequestUtils> utils = mockStatic(PullRequestUtils.class)) {
      utils.when(() -> PullRequestUtils.prKey(pra1)).thenReturn("K1");
      utils.when(() -> PullRequestUtils.prKey(prb1Dup)).thenReturn("K1");
      utils.when(() -> PullRequestUtils.prKey(pra2)).thenReturn("K2");

      // Single stub that branches on the repo argument (index 1)
      doAnswer(inv -> {
        String repo = inv.getArgument(1, String.class);
        if ("svc-a".equals(repo)) {
          return Flux.just(pra1, pra2);
        } else if ("svc-b".equals(repo)) {
          return Flux.just(prb1Dup);
        }
        return Flux.empty();
      }).when(spySvc).searchPullRequestsByFilter(any(), anyString(), any(), any());

      var flux = spySvc.searchPullRequestsAcrossRepos(filter, List.of("svc-a", "svc-b"), auth, params);

      StepVerifier.create(flux.collectList())
          .assertNext(list -> {
            // Distinct should keep only two unique keys: K1 and K2
            assertThat(list).hasSize(2);
            assertThat(list).containsExactlyInAnyOrder(pra1, pra2);
          })
          .verifyComplete();
    }

    // called once per repo
    verify(spySvc, times(2)).searchPullRequestsByFilter(any(), anyString(), any(), any());
  }

  @Test
  void fetchMyCommentCount_filtersOnAuthor_notDeleted_published_andHasText() {
    var auth = new BitbucketAuth("tok", "alice", "pwd");

    var c1 = mock(Comment.class);
    when(c1.authoredBy("me-uuid")).thenReturn(true);
    when(c1.isNotDeleted()).thenReturn(true);
    when(c1.isPublished()).thenReturn(true);
    when(c1.hasText()).thenReturn(true);

    var c2 = mock(Comment.class);
    when(c2.authoredBy("me-uuid")).thenReturn(true);
    when(c2.isNotDeleted()).thenReturn(true);
    when(c2.isPublished()).thenReturn(false); // filtered out

    var c3 = mock(Comment.class);
    when(c3.authoredBy("me-uuid")).thenReturn(true);
    when(c3.isNotDeleted()).thenReturn(true);
    when(c3.isPublished()).thenReturn(true);
    when(c3.hasText()).thenReturn(false); // filtered out

    when(bitbucketClient.fetchAll(any(), anyString(), any()))
        .thenReturn(Flux.just(c1, c2, c3));

    var mono = service.fetchMyCommentCount(auth, "acme", "svc-a", 123, "me-uuid");

    StepVerifier.create(mono)
        .expectNext(1) // only c1 passes all filters
        .verifyComplete();

    ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
    verify(bitbucketClient).fetchAll(any(), urlCap.capture(), any());
    assertThat(urlCap.getValue()).contains("/repositories/acme/svc-a/pullrequests/123/comments");
  }

  @Test
  void fetchDiffFilesChanged_reducesStats_summingFiles_andLines() {
    var auth = new BitbucketAuth("tok", "alice", "pwd");

    var ds1 = mock(DiffStat.class);
    when(ds1.linesAdded()).thenReturn(3);
    when(ds1.linesRemoved()).thenReturn(1);

    var ds2 = mock(DiffStat.class);
    when(ds2.linesAdded()).thenReturn(null);  // treated as 0
    when(ds2.linesRemoved()).thenReturn(5);

    var ds3 = mock(DiffStat.class);
    when(ds3.linesAdded()).thenReturn(2);
    when(ds3.linesRemoved()).thenReturn(null); // 0

    when(bitbucketClient.fetchAll(any(), anyString(), any()))
        .thenReturn(Flux.just(ds1, ds2, ds3));

    var mono = service.fetchDiffFilesChanged(auth, "acme", "svc-b", 456);

    StepVerifier.create(mono)
        .assertNext(d -> {
          assertThat(d.filesChanged()).isEqualTo(3); // three entries
          assertThat(d.linesAdded()).isEqualTo(3 + 2); // 5
          assertThat(d.linesRemoved()).isEqualTo(1 + 5); // 6
        })
        .verifyComplete();

    ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
    verify(bitbucketClient).fetchAll(any(), urlCap.capture(), any());
    assertThat(urlCap.getValue()).contains("/repositories/acme/svc-b/pullrequests/456/diffstat");
  }
}
