package com.example.bitbucketstats.models.bitbucket;

public record Comment(
    Integer id,
    User user,
    Boolean deleted,
    Boolean pending,
    Content content
) {

  public boolean authoredBy(String uuid) {
    return user != null && uuid != null && uuid.equals(user.uuid());
  }

  public boolean isPublished() {
    return !Boolean.TRUE.equals(pending);
  }

  public boolean isNotDeleted() {
    return !Boolean.TRUE.equals(deleted);
  }

  public boolean hasText() {
    return content != null && content.raw() != null && !content.raw().isBlank();
  }

  public record Content(String raw) {

  }
}
