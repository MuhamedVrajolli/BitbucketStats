package com.example.bitbucketstats.models;

import java.util.List;

public record PullRequestPage(String next, List<RawPullRequest> values) implements NextPage {

}
