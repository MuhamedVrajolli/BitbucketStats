package com.example.bitbucketstats.integration.response.page;

import com.example.bitbucketstats.integration.response.PullRequest;
import java.util.List;

public record PullRequestPage(String next, List<PullRequest> values) implements Page<PullRequest> {

}
