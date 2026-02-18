package com.example.bitbucketstats.models;

import com.example.bitbucketstats.controllers.response.PullRequestCommentSummary;
import java.util.List;

public record CommentAgg(List<PullRequestCommentSummary> summaries, int totalComments) {

}
