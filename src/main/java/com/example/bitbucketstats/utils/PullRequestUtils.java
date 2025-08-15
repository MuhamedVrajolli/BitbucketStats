package com.example.bitbucketstats.utils;

import com.example.bitbucketstats.models.PullRequest;
import java.time.Duration;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PullRequestUtils {

  public static long hoursOpen(PullRequest pullRequest) {
    return pullRequest.createdOn() == null || pullRequest.updatedOn() == null
        ? 0 : Duration.between(pullRequest.createdOn(), pullRequest.updatedOn()).toHours();
  }

  public static String buildPullRequestLink(String workspace, String repo, long id) {
    return String.format("https://bitbucket.org/%s/%s/pull-requests/%d", workspace, repo, id);
  }

  public static String buildPullRequestKey(PullRequest pr) {
    return pr.repo() + "#" + pr.id();
  }
}
