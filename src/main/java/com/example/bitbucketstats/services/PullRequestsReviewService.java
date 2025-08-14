package com.example.bitbucketstats.services;

import static com.example.bitbucketstats.models.FieldFilter.REVIEWERS_UUID;
import static com.example.bitbucketstats.utils.PullRequestUtils.buildPullRequestLink;

import com.example.bitbucketstats.integration.BitBucketService;
import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.CommentAgg;
import com.example.bitbucketstats.models.FieldFilter;
import com.example.bitbucketstats.models.PullRequest;
import com.example.bitbucketstats.models.User;
import com.example.bitbucketstats.models.request.PullRequestReviewParams;
import com.example.bitbucketstats.models.response.PullRequestCommentSummary;
import com.example.bitbucketstats.models.response.PullRequestReviewResponse;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PullRequestsReviewService {

  private static final Logger log = LoggerFactory.getLogger(PullRequestsReviewService.class);
  private final BitBucketService bitBucketService;

  /**
   * Fetches pull request review statistics for a specific reviewer or the authenticated user.
   *
   * @param auth   The Bitbucket authentication details.
   * @param params The parameters for filtering and fetching pull requests.
   * @return A Mono containing the summarized pull request review statistics.
   */
  public Mono<PullRequestReviewResponse> getReviewStats(BitbucketAuth auth, PullRequestReviewParams params) {
    log.info("Pull requests reviews called: workspace={}, repos={}, includeCommentDetails={}, sinceDate={}, untilDate={}, states={}, maxConcurrency={}",
        params.getWorkspace(), params.getRepo(), params.isIncludeCommentDetails(),
        params.getSinceDate(), params.getUntilDate(), params.getState(), params.getMaxConcurrency());

    int concurrency = Math.max(1, params.getMaxConcurrency());

    return resolveReviewerUuid(auth, params)
        .flatMap(reviewerUuid ->
            bitBucketService.searchPullRequestsAcrossRepos(FieldFilter.of(REVIEWERS_UUID, reviewerUuid),
                    params.getRepo(), auth, params, params.getMaxConcurrency())
                .collectList()
                .doOnSuccess(prs -> log.info("Fetched {} PRs (deduped)", prs != null ? prs.size() : 0))
                .flatMap(prs -> buildReviewResponse(prs, reviewerUuid, params, auth, concurrency))
        );
  }

  private Mono<String> resolveReviewerUuid(BitbucketAuth auth, PullRequestReviewParams params) {
    if (StringUtils.hasText(params.getReviewerUuid())) {
      return Mono.just(params.getReviewerUuid());
    }
    return bitBucketService.getCurrentUser(auth)
        .doOnSubscribe(s -> log.debug("Fetching current Bitbucket user"))
        .map(User::uuid);
  }

  private Mono<PullRequestReviewResponse> buildReviewResponse(List<PullRequest> pullRequests,
      String myUuid,
      PullRequestReviewParams params,
      BitbucketAuth auth,
      int concurrency) {
    int totalReviewed = pullRequests.size();
    int approvedCount = (int) pullRequests.stream().filter(pr -> pr.approvedBy(myUuid)).count();
    log.info("Aggregated PRs: totalReviewed={} approvedByMe={}", totalReviewed, approvedCount);

    if (!params.isIncludeCommentDetails() || totalReviewed == 0) {
      log.debug("Comment details disabled or no PRs. includeCommentDetails={} totalReviewed={}",
          params.isIncludeCommentDetails(), totalReviewed);
      return Mono.just(PullRequestReviewResponse.from(
          pullRequests, params.getSinceDate(), params.getUntilDate(), approvedCount, 0, 0, Map.of()));
    }

    var prsNeedingComments = pullRequests.stream()
        .filter(pr -> pr.commentCount() != null && pr.commentCount() > 0)
        .toList();

    log.info("{} PRs have comments; fetching per-PR comment counts for current user", prsNeedingComments.size());

    return fetchMyCommentAgg(prsNeedingComments, params, auth, myUuid, concurrency)
        .map(agg -> {
          int commentedPrs = agg.summaries().size();
          log.info("Comment aggregation done: commentedPrs={} totalMyComments={}", commentedPrs, agg.totalComments());
          return PullRequestReviewResponse.from(
              pullRequests,
              params.getSinceDate(),
              params.getUntilDate(),
              approvedCount,
              commentedPrs,
              agg.totalComments(),
              agg.summaries()
          );
        });
  }

  private Mono<CommentAgg> fetchMyCommentAgg(List<PullRequest> prsNeedingComments,
      PullRequestReviewParams params,
      BitbucketAuth auth,
      String myUuid,
      int concurrency) {
    return Flux.fromIterable(prsNeedingComments)
        .flatMap(pr ->
                bitBucketService.fetchMyCommentCount(auth, params.getWorkspace(), pr.repo(), pr.id(), myUuid)
                    .doOnSubscribe(s -> log.trace("Fetching comments for {}#{}", pr.repo(), pr.id()))
                    .doOnSuccess(c -> log.debug("My comments on {}#{} = {}", pr.repo(), pr.id(), c))
                    .map(myComments -> new AbstractMap.SimpleEntry<>(pr, myComments)),
            concurrency)
        .filter(entry -> entry.getValue() > 0)
        .collectList()
        .map(entries -> {
          int total = entries.stream().mapToInt(Map.Entry::getValue).sum();
          Map<String, PullRequestCommentSummary> summaries = new LinkedHashMap<>();
          for (var e : entries) {
            var pr = e.getKey();
            int myComments = e.getValue();
            String key = pr.repo() + "#" + pr.id();
            summaries.put(key, new PullRequestCommentSummary(
                pr.id(),
                pr.title(),
                buildPullRequestLink(params.getWorkspace(), pr.repo(), pr.id()),
                myComments,
                pr.repo()
            ));
          }
          return new CommentAgg(summaries, total);
        });
  }
}
