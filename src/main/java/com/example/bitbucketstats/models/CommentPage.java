package com.example.bitbucketstats.models;

import java.util.List;

public record CommentPage(String next, List<Comment> values) implements NextPage {}
