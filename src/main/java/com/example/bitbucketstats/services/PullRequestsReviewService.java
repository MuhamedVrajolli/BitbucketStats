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
import java.util.ArrayList;
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
  private final ResponseAssembler responseAssembler;

  /**
   * Fetches pull request review statistics for a specific reviewer or the authenticated user.
   *
   * @param auth   The Bitbucket authentication details.
   * @param params The parameters for filtering and fetching pull requests.
   * @return A Mono containing the summarized pull request review statistics.
   */
  public Mono<PullRequestReviewResponse> getReviewStats(BitbucketAuth auth, PullRequestReviewParams params) {
    log.info(
        "Pull requests reviews: ws={}, repos={}, includeCommentDetails={}, since={}, until={}, states={}, maxConc={}",
        params.getWorkspace(), params.getRepo(), params.isIncludeCommentDetails(),
        params.getSinceDate(), params.getUntilDate(), params.getState(), params.getMaxConcurrency());

    return resolveReviewerUuid(auth, params)
        .flatMap(reviewerUuid ->
            bitBucketService.searchPullRequestsAcrossRepos(
                    FieldFilter.of(REVIEWERS_UUID, reviewerUuid),
                    params.getRepo(), auth, params)
                .collectList()
                .doOnNext(prs -> log.info("Fetched {} PRs (deduped)", prs.size()))
                .flatMap(prs -> {
                  if (!params.isIncludeCommentDetails() || prs.isEmpty()) {
                    return Mono.just(responseAssembler.toPullRequestReviewResponse(
                        prs, params, reviewerUuid, List.of(), null));
                  }
                  return fetchMyCommentAgg(prs, params, auth, reviewerUuid)
                      .map(agg -> responseAssembler.toPullRequestReviewResponse(
                          prs, params, reviewerUuid, agg.summaries(), agg.totalComments()));
                })
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

  /**
   * Fetches the count of comments made by the authenticated user on pull requests that have been reviewed.
   *
   * @param allReviewedPrs The list of all reviewed pull requests.
   * @param params         The parameters for filtering and fetching pull requests.
   * @param auth           The Bitbucket authentication details.
   * @param myUuid         The UUID of the authenticated user.
   * @return A Mono containing a CommentAgg with the summaries and total comment count.
   */
  private Mono<CommentAgg> fetchMyCommentAgg(
      List<PullRequest> allReviewedPrs,
      PullRequestReviewParams params,
      BitbucketAuth auth,
      String myUuid) {
    var needingComments = allReviewedPrs.stream()
        .filter(pr -> pr.commentCount() != null && pr.commentCount() > 0)
        .toList();

    if (needingComments.isEmpty()) {
      return Mono.just(new CommentAgg(List.of(), 0));
    }

    return Flux.fromIterable(needingComments)
        .flatMap(pr ->
                bitBucketService.fetchMyCommentCount(auth, params.getWorkspace(), pr.repo(), pr.id(), myUuid)
                    .doOnSubscribe(s -> log.trace("Fetching comments for {}#{}", pr.repo(), pr.id()))
                    .doOnSuccess(c -> log.debug("My comments on {}#{} = {}", pr.repo(), pr.id(), c))
                    .map(myComments -> Map.entry(pr, myComments)),
            Math.max(1, params.getMaxConcurrency()))
        .filter(e -> e.getValue() > 0)
        .collectList()
        .map(entries -> {
          int total = entries.stream().mapToInt(Map.Entry::getValue).sum();
          List<PullRequestCommentSummary> summaries = new ArrayList<>();
          for (var e : entries) {
            var pr = e.getKey();
            int myComments = e.getValue();
            summaries.add(new PullRequestCommentSummary(
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
