package com.example.bitbucketstats.models;

import com.example.bitbucketstats.models.response.PullRequestCommentSummary;
import java.util.Map;

public record CommentAgg(Map<String, PullRequestCommentSummary> summaries, int totalComments) {

}
