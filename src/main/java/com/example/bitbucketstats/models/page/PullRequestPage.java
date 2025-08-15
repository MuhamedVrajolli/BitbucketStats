package com.example.bitbucketstats.models.page;

import com.example.bitbucketstats.models.bitbucket.PullRequest;
import java.util.List;

public record PullRequestPage(String next, List<PullRequest> values) implements Page<PullRequest> {

}
