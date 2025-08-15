package com.example.bitbucketstats.models;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bitbucketstats.models.bitbucket.Author;
import com.example.bitbucketstats.models.bitbucket.Participant;
import com.example.bitbucketstats.models.bitbucket.PullRequest;
import com.example.bitbucketstats.models.bitbucket.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("unit")
class EnrichedPullRequestTest {

  static Stream<Arguments> approvedByCases() {
    String me = "me-uuid";

    // helper to make a participant
    java.util.function.BiFunction<String, Boolean, Participant> part = (uuid, approved) -> {
      var u = mock(User.class);
      when(u.uuid()).thenReturn(uuid);
      var p = mock(Participant.class);
      when(p.user()).thenReturn(u);
      when(p.approved()).thenReturn(approved);
      return p;
    };

    Participant matchingApproved = part.apply(me, true);
    Participant matchingNotApproved = part.apply(me, false);
    Participant otherApproved = part.apply("other", true);
    Participant nullUserApproved = mock(Participant.class);
    when(nullUserApproved.user()).thenReturn(null);
    when(nullUserApproved.approved()).thenReturn(true);

    return Stream.of(
        // participants null -> false
        Arguments.of(null, me, false),
        // empty list -> false
        Arguments.of(List.<Participant>of(), me, false),
        // contains matching & approved -> true
        Arguments.of(List.of(matchingApproved), me, true),
        // contains matching but not approved -> false
        Arguments.of(List.of(matchingNotApproved), me, false),
        // only others approved -> false
        Arguments.of(List.of(otherApproved), me, false),
        // participant with null user is ignored
        Arguments.of(List.of(nullUserApproved), me, false)
    );
  }

  @ParameterizedTest
  @MethodSource("approvedByCases")
  void approvedBy_handlesCommonCases(List<Participant> participants, String myUuid, boolean expected) {
    var pr = new EnrichedPullRequest(
        1, "t", "a", 0, participants, "repo",
        OffsetDateTime.now(), OffsetDateTime.now()
    );
    assertThat(pr.approvedBy(myUuid)).isEqualTo(expected);
  }

  @Test
  void from_mapsAllFields_whenAuthorPresent() {
    var pull = mock(PullRequest.class);
    when(pull.id()).thenReturn(42);
    when(pull.title()).thenReturn("Add feature");
    var author = mock(Author.class);
    when(author.uuid()).thenReturn("u-123");
    when(pull.author()).thenReturn(author);
    when(pull.commentCount()).thenReturn(7);
    var participants = List.<Participant>of(mock(Participant.class));
    when(pull.participants()).thenReturn(participants);
    var created = OffsetDateTime.parse("2025-08-01T12:00:00Z");
    var updated = OffsetDateTime.parse("2025-08-05T15:30:00Z");
    when(pull.createdOn()).thenReturn(created);
    when(pull.updatedOn()).thenReturn(updated);

    var enriched = EnrichedPullRequest.from(pull, "svc-a");

    assertThat(enriched.id()).isEqualTo(42);
    assertThat(enriched.title()).isEqualTo("Add feature");
    assertThat(enriched.authorUuid()).isEqualTo("u-123");
    assertThat(enriched.commentCount()).isEqualTo(7);
    assertThat(enriched.participants()).isSameAs(participants);
    assertThat(enriched.repo()).isEqualTo("svc-a");
    assertThat(enriched.createdOn()).isEqualTo(created);
    assertThat(enriched.updatedOn()).isEqualTo(updated);
  }

  @Test
  void from_setsAuthorUuidNull_whenAuthorIsNull() {
    var pull = mock(PullRequest.class);
    when(pull.id()).thenReturn(1);
    when(pull.title()).thenReturn("t");
    when(pull.author()).thenReturn(null);
    when(pull.commentCount()).thenReturn(null);
    when(pull.participants()).thenReturn(null);
    var ts = OffsetDateTime.parse("2025-08-10T00:00:00Z");
    when(pull.createdOn()).thenReturn(ts);
    when(pull.updatedOn()).thenReturn(ts);

    var enriched = EnrichedPullRequest.from(pull, "svc-b");

    assertThat(enriched.authorUuid()).isNull();
    assertThat(enriched.repo()).isEqualTo("svc-b");
  }
}
