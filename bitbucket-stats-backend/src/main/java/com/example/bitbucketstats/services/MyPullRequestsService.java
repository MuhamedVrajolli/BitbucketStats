package com.example.bitbucketstats.services;

import static com.example.bitbucketstats.models.FieldFilter.AUTHOR_NICKNAME;
import static com.example.bitbucketstats.models.FieldFilter.AUTHOR_USERNAME;
import static com.example.bitbucketstats.models.FieldFilter.AUTHOR_UUID;
import static com.example.bitbucketstats.utils.PullRequestUtils.prKey;

import com.example.bitbucketstats.integration.BitBucketService;
import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.DiffDetails;
import com.example.bitbucketstats.models.FieldFilter;
import com.example.bitbucketstats.models.EnrichedPullRequest;
import com.example.bitbucketstats.controllers.request.MyPullRequestsParams;
import com.example.bitbucketstats.controllers.response.MyPullRequestsResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MyPullRequestsService {

  private final BitBucketService bitBucketService;
  private final ResponseAssembler responseAssembler;

  /**
   * Fetches pull request statistics for a specific author (filtered by nickname) or the authenticated user and based on
   * the other provided parameters.
   *
   * @param auth   The Bitbucket authentication details.
   * @param params The parameters for filtering and fetching pull requests.
   * @return A Mono containing the summarized pull request statistics.
   */
  public Mono<MyPullRequestsResponse> getMyPullRequestsStats(BitbucketAuth auth, MyPullRequestsParams params) {
    return resolveAuthorFilter(auth, auth.user(), params)
        .flatMapMany(filter -> bitBucketService.searchPullRequestsAcrossRepos(
            filter, params.getRepo(), auth, params))
        .collectList()
        .flatMap(prs -> {
          Mono<Map<String, DiffDetails>> diffsMono = params.isIncludeDiffDetails()
              ? loadDiffDetailsMap(auth, prs, params)
              : Mono.just(Map.of());
          return diffsMono.map(diffs -> responseAssembler.toMyPullRequestsResponse(prs, params, diffs));
        });
  }

  private Mono<FieldFilter> resolveAuthorFilter(BitbucketAuth auth, String username, MyPullRequestsParams params) {
    if (StringUtils.hasText(params.getNickname())) {
      return Mono.just(FieldFilter.of(AUTHOR_NICKNAME, params.getNickname()));
    }
    if (StringUtils.hasText(username)) {
      return Mono.just(FieldFilter.of(AUTHOR_USERNAME, username));
    }
    return bitBucketService.getCurrentUser(auth)
        .map(me -> FieldFilter.of(AUTHOR_UUID, me.uuid()));
  }

  /**
   * Loads the diff details for each pull.
   *
   * @param auth   The Bitbucket authentication details.
   * @param prs    The list of pull requests to fetch diff details for.
   * @param params The parameters containing the workspace and max concurrency.
   * @return A Mono containing a map of pull request keyed by "repo#id" and their diff details.
   */
  private Mono<Map<String, DiffDetails>> loadDiffDetailsMap(
      BitbucketAuth auth, List<EnrichedPullRequest> prs, MyPullRequestsParams params) {
    if (prs.isEmpty()) {
      return Mono.just(Map.of());
    }

    return Flux.fromIterable(prs)
        .flatMap(pr -> bitBucketService
                .fetchDiffFilesChanged(auth, params.getWorkspace(), pr.repo(), pr.id())
                .map(dd -> Map.entry(prKey(pr), dd)),
            Math.max(1, params.getMaxConcurrency()))
        .collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }
}
