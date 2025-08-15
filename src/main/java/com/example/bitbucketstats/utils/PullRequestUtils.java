package com.example.bitbucketstats.utils;

import com.example.bitbucketstats.models.EnrichedPullRequest;
import java.time.Duration;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PullRequestUtils {

  public static long hoursOpen(EnrichedPullRequest enrichedPullRequest) {
    return enrichedPullRequest.createdOn() == null || enrichedPullRequest.updatedOn() == null
        ? 0 : Duration.between(enrichedPullRequest.createdOn(), enrichedPullRequest.updatedOn()).toHours();
  }

  public static String prLink(String workspace, String repo, long id) {
    return String.format("https://bitbucket.org/%s/%s/pull-requests/%d", workspace, repo, id);
  }

  public static String prKey(EnrichedPullRequest pr) {
    return pr.repo() + "#" + pr.id();
  }
}
