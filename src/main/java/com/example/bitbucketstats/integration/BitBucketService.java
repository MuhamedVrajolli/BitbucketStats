package com.example.bitbucketstats.integration;

import static com.example.bitbucketstats.configuration.CachingConfig.BITBUCKET_USER_CACHE;
import static com.example.bitbucketstats.utils.GeneralUtils.quote;
import static com.example.bitbucketstats.utils.GeneralUtils.urlEncode;

import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.CommentPage;
import com.example.bitbucketstats.models.FieldFilter;
import com.example.bitbucketstats.models.PullRequest;
import com.example.bitbucketstats.models.PullRequestPage;
import com.example.bitbucketstats.models.User;
import com.example.bitbucketstats.models.request.BaseParams;
import com.example.bitbucketstats.utils.GeneralUtils;
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
  private static final String API_BASE = "https://api.bitbucket.org/2.0";
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

  @Cacheable(cacheNames = BITBUCKET_USER_CACHE, key = "#auth.cacheKey()")
  public Mono<User> getCurrentUser(BitbucketAuth auth) {
    String url = API_BASE + "/user";
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
   * @param concurrency maximum number of concurrent requests
   * @return a Flux of PullRequest objects matching the filter across all specified repositories
   */
  public Flux<PullRequest> searchPullRequestsAcrossRepos(
      FieldFilter filter, List<String> repos, BitbucketAuth auth, BaseParams params, int concurrency) {
    int cc = Math.max(1, concurrency);
    return Flux.fromIterable(repos)
        .flatMap(repo -> searchPullRequestsByFilter(filter, repo, auth, params), cc)
        .distinct(pr -> pr.repo() + "#" + pr.id())
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
  public Flux<PullRequest> searchPullRequestsByFilter(
      FieldFilter fieldFilter, String repo, BitbucketAuth auth, BaseParams params) {

    String query = buildPullRequestsQuery(
        fieldFilter.key(), fieldFilter.value(),
        params.getSinceDate(), params.getUntilDate(),
        params.getState(), params.getQueued());

    String url = buildPullRequestsUrl(params.getWorkspace(), repo, query);

    log.debug("Search PRs by {}: repo={} value={} states={} queued={} since={} until={} url={}",
        fieldFilter.key(), repo, fieldFilter.value(), params.getState(), params.getQueued(),
        params.getSinceDate(), params.getUntilDate(), url);

    return bitbucketClient.fetchPaged(auth, url, PullRequestPage.class)
        .flatMapIterable(PullRequestPage::values)
        .map(p -> PullRequest.from(p, repo));
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
    String url = String.format("%s/repositories/%s/%s/pullrequests/%d/comments?pagelen=100",
        API_BASE, workspace, repo, prId);
    log.trace("Fetch comments: repo={} prId={} url={}", repo, prId, url);

    return bitbucketClient.fetchPaged(auth, url, CommentPage.class)
        .doOnSubscribe(s -> log.trace("Begin comments paging for {}#{}", repo, prId))
        .flatMapIterable(CommentPage::values)
        .filter(c -> c.user() != null && myUuid.equals(c.user().uuid()))
        .count()
        .map(Long::intValue)
        .doOnSuccess(c -> log.trace("My comment count for {}#{} = {}", repo, prId, c));
  }

  private static String buildPullRequestsUrl(String workspace, String repo, String query) {
    var url = String.format("%s/repositories/%s/%s/pullrequests?q=%s&pagelen=50&fields=%s",
        API_BASE, workspace, repo, urlEncode(query), PR_FIELDS);
    log.info("Pull requests url: {}", url);
    return url;
  }

  private static String buildPullRequestsQuery(
      String filterField, String filterValue, LocalDate since, LocalDate until,
      @Nullable List<String> states, @Nullable Boolean queued) {

    StringBuilder q = new StringBuilder()
        .append(filterField).append('=').append(quote(filterValue))
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
