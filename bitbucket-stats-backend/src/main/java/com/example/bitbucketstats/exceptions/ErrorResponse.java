package com.example.bitbucketstats.exceptions;

import java.util.Collection;

public record ErrorResponse(
    String timestamp,
    int status,
    String error,
    String message,
    String path,
    Collection<Violation> errors
) {
  public record Violation(String field, String message) {}
}
