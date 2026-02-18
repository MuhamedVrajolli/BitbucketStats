package com.example.bitbucketstats.integration.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
class CommentTest {

  @ParameterizedTest
  @CsvSource({
      "u1,u1,true",
      "u1,u2,false",
      "u1,'',false",
      "u1,NULL,false",
      "NULL,u1,false"
  })
  void authoredBy_handlesNullsAndMatch(String userUuid, String inputUuid, boolean expected) {
    User u = "NULL".equals(userUuid) ? null : user(userUuid);
    String in = "NULL".equals(inputUuid) ? null : inputUuid;
    var c = new Comment(1, u, null, null, null);
    assertThat(c.authoredBy(in)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource(
      value = {"null,true", "true,false", "false,true"},
      nullValues = {"null"}
  )
  void isPublished_checksPending(Boolean pending, boolean expected) {
    var c = new Comment(1, null, null, pending, null);
    assertThat(c.isPublished()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource(
      value = {"null,true", "true,false", "false,true"},
      nullValues = {"null"}
  )
  void isNotDeleted_checksDeleted(Boolean deleted, boolean expected) {
    var c = new Comment(1, null, deleted, null, null);
    assertThat(c.isNotDeleted()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "NULL,false",
      "'',false",
      "'   ',false",
      "'ok',true"
  })
  void hasText_checksContent(String raw, boolean expected) {
    Comment.Content content = raw == null || "NULL".equals(raw) ? null : new Comment.Content(raw);
    var c = new Comment(1, null, null, null, content);
    assertThat(c.hasText()).isEqualTo(expected);
  }

  private static User user(String uuid) {
    var u = mock(User.class);
    when(u.uuid()).thenReturn(uuid);
    return u;
  }
}
