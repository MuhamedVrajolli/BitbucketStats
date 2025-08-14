package com.example.bitbucketstats.services;

import static com.example.bitbucketstats.models.FieldFilter.AUTHOR_NICKNAME;
import static com.example.bitbucketstats.models.FieldFilter.AUTHOR_USERNAME;
import static com.example.bitbucketstats.models.FieldFilter.AUTHOR_UUID;
import static com.example.bitbucketstats.utils.GeneralUtils.safeInt;
import static com.example.bitbucketstats.utils.PullRequestUtils.buildPullRequestLink;
import static com.example.bitbucketstats.utils.PullRequestUtils.hoursOpen;

import com.example.bitbucketstats.integration.BitBucketService;
import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.FieldFilter;
import com.example.bitbucketstats.models.PullRequest;
import com.example.bitbucketstats.models.request.MyPullRequestsParams;
import com.example.bitbucketstats.models.response.MyPullRequestsResponse;
import com.example.bitbucketstats.models.response.MyPullRequestsSummary;
import com.example.bitbucketstats.utils.PullRequestUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MyPullRequestsService {

  private static final Logger log = LoggerFactory.getLogger(MyPullRequestsService.class);
  private final BitBucketService bitBucketService;

  /**
   * Fetches pull request statistics for a specific author (filtered by nickname) or the authenticated user
   * and based on the other provided parameters.
   *
   * @param auth   The Bitbucket authentication details.
   * @param params The parameters for filtering and fetching pull requests.
   * @return A Mono containing the summarized pull request statistics.
   */
  public Mono<MyPullRequestsResponse> getMyPullRequestsStats(BitbucketAuth auth, MyPullRequestsParams params) {
    log.info("My pull requests stats called: workspace={}, repo(s)={}, includePRDetails={}, sinceDate={}, untilDate={}, nickname={}",
        params.getWorkspace(), params.getRepo(), params.isIncludeDetails(), params.getSinceDate(),
        params.getUntilDate(), params.getNickname());

    return resolveAuthorFilter(auth, auth.user(), params)
        .flatMapMany(filter ->
            bitBucketService.searchPullRequestsAcrossRepos(filter, params.getRepo(), auth, params, params.getMaxConcurrency()))
        .collectList()
        .map(prs -> summarize(prs, params));
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

  private MyPullRequestsResponse summarize(List<PullRequest> pullRequests, MyPullRequestsParams params) {
    int total = pullRequests.size();

    long totalOpenHours = pullRequests.stream()
        .mapToLong(PullRequestUtils::hoursOpen)
        .sum();

    long totalComments = pullRequests.stream()
        .mapToLong(pr -> safeInt(pr.commentCount()))
        .sum();

    List<MyPullRequestsSummary> details = params.isIncludeDetails()
        ? pullRequests.stream()
        .map(pr -> new MyPullRequestsSummary(
            pr.title(),
            buildPullRequestLink(params.getWorkspace(), pr.repo(), pr.id()),
            (int) hoursOpen(pr),
            safeInt(pr.commentCount())
        ))
        .toList()
        : null;

    double avgTime = total == 0 ? 0 : Math.round((double) totalOpenHours / total);
    double avgComments = total == 0 ? 0 : Math.round((double) totalComments / total);

    return new MyPullRequestsResponse(
        String.format("FROM: %s TO: %s", params.getSinceDate(), params.getUntilDate()),
        total,
        avgTime,
        avgComments,
        details
    );
  }
}
