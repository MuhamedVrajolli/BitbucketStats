package com.example.bitbucketstats.integration;

import static com.example.bitbucketstats.configuration.CachingConfig.BITBUCKET_USER_CACHE;
import static com.example.bitbucketstats.utils.GeneralUtils.quote;
import static com.example.bitbucketstats.utils.GeneralUtils.urlEncode;

import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.DiffDetails;
import com.example.bitbucketstats.models.EnrichedPullRequest;
import com.example.bitbucketstats.models.FieldFilter;
import com.example.bitbucketstats.integration.response.User;
import com.example.bitbucketstats.integration.response.page.CommentPage;
import com.example.bitbucketstats.integration.response.page.DiffStatPage;
import com.example.bitbucketstats.integration.response.page.PullRequestPage;
import com.example.bitbucketstats.controllers.request.BaseParams;
import com.example.bitbucketstats.utils.GeneralUtils;
import com.example.bitbucketstats.utils.PullRequestUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BitBucketService {

  private static final Logger log = LoggerFactory.getLogger(BitBucketService.class);
  private static final String PR_FIELDS = String.join(",",
      "next",
      "values.id",
      "values.title",
      "values.author.uuid",
      "values.created_on",
      "values.updated_on",
      "values.comment_count",
      "values.participants.user.uuid",
      "values.participants.approved"
  );

  private final BitbucketClient bitbucketClient;

  /**
   * Fetch the current user from Bitbucket using the provided authentication details.
   *
   * @param auth the authentication details
   * @return a Mono containing the User object representing the current user
   */
  @Cacheable(cacheNames = BITBUCKET_USER_CACHE, key = "#auth.cacheKey()")
  public Mono<User> getCurrentUser(BitbucketAuth auth) {
    String url = "/user";
    log.debug("GET current user {}", url);
    return bitbucketClient.retrieveJson(auth, url, User.class)
        .doOnSuccess(u -> log.debug("Fetched current user uuid={}", u != null ? u.uuid() : "<null>"));
  }

  /**
   * Search pull requests across multiple repositories using the given filter.
   *
   * @param filter the filter to apply (e.g. author, reviewer)
   * @param repos list of repository names to search in
   * @param auth authentication details
   * @param params additional parameters like date range and state
   * @return a Flux of PullRequest objects matching the filter across all specified repositories
   */
  public Flux<EnrichedPullRequest> searchPullRequestsAcrossRepos(
      FieldFilter filter, List<String> repos, BitbucketAuth auth, BaseParams params) {
    int cc = Math.max(1, params.getMaxConcurrency());
    return Flux.fromIterable(repos)
        .flatMap(repo -> searchPullRequestsByFilter(filter, repo, auth, params), cc)
        .distinct(PullRequestUtils::prKey)
        .doOnError(e -> log.warn("Error while fetching PRs", e));
  }

  /**
   * Search pull requests in a specific repository using the given filter.
   *
   * @param fieldFilter the filter to apply (e.g. author, reviewer)
   * @param repo the repository name
   * @param auth authentication details
   * @param params additional parameters like date range and state
   * @return a Flux of PullRequest objects matching the filter
   */
  public Flux<EnrichedPullRequest> searchPullRequestsByFilter(
      FieldFilter fieldFilter, String repo, BitbucketAuth auth, BaseParams params) {
    String query = buildPullRequestsQuery(fieldFilter ,params.getSinceDate(), params.getUntilDate(),
        params.getState(), params.getQueued());

    var url = String.format("/repositories/%s/%s/pullrequests?q=%s&pagelen=50&fields=%s",
        params.getWorkspace(), repo, urlEncode(query), PR_FIELDS);

    log.info("Pull requests url: {}", url);
    log.debug("Search PRs by {}: repo={} value={} states={} queued={} since={} until={} url={}",
        fieldFilter.key(), repo, fieldFilter.value(), params.getState(), params.getQueued(),
        params.getSinceDate(), params.getUntilDate(), url);

    return bitbucketClient.fetchAll(auth, url, PullRequestPage.class)
        .map(p -> EnrichedPullRequest.from(p, repo));
  }

  /**
   * Fetch the count of comments made by the current user on a specific pull request.
   *
   * @param auth authentication details
   * @param workspace the Bitbucket workspace
   * @param repo the repository name
   * @param prId the pull request ID
   * @param myUuid the UUID of the current user
   * @return a Mono containing the count of comments made by the user
   */
  public Mono<Integer> fetchMyCommentCount(BitbucketAuth auth, String workspace, String repo, int prId, String myUuid) {
    String url = String.format("/repositories/%s/%s/pullrequests/%d/comments?pagelen=100", workspace, repo, prId);
    log.trace("Pull request comments url: {}", url);

    return bitbucketClient.fetchAll(auth, url, CommentPage.class)
        .doOnSubscribe(s -> log.trace("Begin comments paging for {}#{}", repo, prId))
        .filter(c -> c.authoredBy(myUuid) && c.isNotDeleted() && c.isPublished() && c.hasText())
        .count()
        .map(Long::intValue)
        .doOnSuccess(c -> log.trace("My comment count for {}#{} = {}", repo, prId, c));
  }

  /**
   * Fetch the number of files changed in a pull request, along with lines added and removed.
   *
   * @param auth authentication details
   * @param workspace the Bitbucket workspace
   * @param repo the repository name
   * @param prId the pull request ID
   * @return a Mono containing DiffDetails with files changed, lines added, and lines removed
   */
  public Mono<DiffDetails> fetchDiffFilesChanged(BitbucketAuth auth, String workspace, String repo, int prId) {
    String url = String.format("/repositories/%s/%s/pullrequests/%d/diffstat?pagelen=100", workspace, repo, prId);
    log.trace("Pull requests diff-stat url: {}", url);

    return bitbucketClient.fetchAll(auth, url, DiffStatPage.class)
        .reduce(new DiffDetails(0, 0, 0), (acc, ds) -> new DiffDetails(
            acc.filesChanged() + 1,
            acc.linesAdded() + (ds.linesAdded() == null ? 0 : ds.linesAdded()),
            acc.linesRemoved() + (ds.linesRemoved() == null ? 0 : ds.linesRemoved())
        ))
        .doOnSuccess(d -> log.trace("Diff summary for {}#{} => files={}, +{} -{}",
            repo, prId, d.filesChanged(), d.linesAdded(), d.linesRemoved()));
  }

  private static String buildPullRequestsQuery(FieldFilter filterField, LocalDate since, LocalDate until,
      @Nullable List<String> states, @Nullable Boolean queued) {
    StringBuilder q = new StringBuilder()
        .append(filterField.key()).append('=').append(quote(filterField.value()))
        .append(" AND updated_on>=").append(quote(since.toString()))
        .append(" AND updated_on<=").append(quote(until.toString()));

    if (states != null && !states.isEmpty()) {
      if (states.size() == 1) {
        q.append(" AND state=").append(quote(states.get(0)));
      } else {
        String joined = states.stream()
            .map(GeneralUtils::quote)
            .collect(Collectors.joining(","));
        q.append(" AND state IN (").append(joined).append(')');
      }
    }

    if (queued != null) {
      q.append(" AND queued=").append(queued);
    }

    return q.toString();
  }
}
