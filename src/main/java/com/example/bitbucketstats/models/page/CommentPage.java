package com.example.bitbucketstats.models.page;

import com.example.bitbucketstats.models.bitbucket.Comment;
import java.util.List;

public record CommentPage(String next, List<Comment> values) implements Page<Comment> {}
