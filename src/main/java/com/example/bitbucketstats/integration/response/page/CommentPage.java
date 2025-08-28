package com.example.bitbucketstats.integration.response.page;

import com.example.bitbucketstats.integration.response.Comment;
import java.util.List;

public record CommentPage(String next, List<Comment> values) implements Page<Comment> {}
