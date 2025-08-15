package com.example.bitbucketstats.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.bitbucketstats.models.request.PullRequestReviewParams;
import com.example.bitbucketstats.models.response.PullRequestReviewResponse;
import com.example.bitbucketstats.services.PullRequestsReviewService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@Tag("unit")
@WebFluxTest(PullRequestsReviewController.class)
class PullRequestsReviewControllerTest {

  @Autowired
  WebTestClient webTestClient;

  @MockitoBean
  PullRequestsReviewService pullRequestsReviewService;

  @BeforeEach
  void setup() {
    when(pullRequestsReviewService.getReviewStats(any(), any()))
        .thenReturn(Mono.just(new PullRequestReviewResponse(null, 0,
            0, null, null, null,
            null, null)));
  }

  @Test
  void getReviewStats_bindsAllParams_andDelegates() {
    var since = LocalDate.of(2025, 8, 1);
    var until = LocalDate.of(2025, 8, 10);

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/reviews/stats")
            .queryParam("workspace", "acme")
            .queryParam("repo", "svc-a")
            .queryParam("repo", "svc-b")
            .queryParam("sinceDate", since)
            .queryParam("untilDate", until)
            .queryParam("state", "OPEN")
            .queryParam("queued", false)
            .queryParam("maxConcurrency", 3)
            .queryParam("reviewerUuid", "1234-uuid")
            .queryParam("includeCommentDetails", true)
            .build())
        .header("Authorization", "Basic abc123==")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

    var paramsCap = ArgumentCaptor.forClass(PullRequestReviewParams.class);
    verify(pullRequestsReviewService).getReviewStats(any(), paramsCap.capture());
    var p = paramsCap.getValue();

    assertThat(p.getWorkspace()).isEqualTo("acme");
    assertThat(p.getRepo()).containsExactly("svc-a", "svc-b");
    assertThat(p.getSinceDate()).isEqualTo(since);
    assertThat(p.getUntilDate()).isEqualTo(until);
    assertThat(p.getState()).containsExactly("OPEN");
    assertThat(p.getQueued()).isFalse();
    assertThat(p.getMaxConcurrency()).isEqualTo(3);
    assertThat(p.getReviewerUuid()).isEqualTo("1234-uuid");
    assertThat(p.isIncludeCommentDetails()).isTrue();
  }

  @Test
  void getReviewStats_missingRequiredParams_returns400() {
    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/reviews/stats")
            .queryParam("untilDate", LocalDate.of(2025, 8, 10))
            .build())
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(String.class)
        .value(body -> {
          assertThat(body).contains("workspace", "must not be blank");
          assertThat(body).contains("repo", "must not be empty");
          assertThat(body).contains("sinceDate", "must not be null");
        });

    verifyNoMoreInteractions(pullRequestsReviewService);
  }

  @Test
  void getReviewStats_invalidDateRange_returns400() {
    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/reviews/stats")
            .queryParam("workspace", "acme")
            .queryParam("repo", "svc-a")
            .queryParam("sinceDate", "2025-08-11")
            .queryParam("untilDate", "2025-08-01")
            .build())
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(String.class)
        .value(body -> assertThat(body)
            .contains("sinceDate must be ≤ untilDate"));

    verifyNoMoreInteractions(pullRequestsReviewService);
  }

  @Test
  void getReviewStats_futureDate_rejectedByPastOrPresent() {
    var future = LocalDate.now().plusDays(1);

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/reviews/stats")
            .queryParam("workspace", "acme")
            .queryParam("repo", "svc-a")
            .queryParam("sinceDate", future) // violates @PastOrPresent
            .build())
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(String.class)
        .value(body -> assertThat(body)
            .contains("sinceDate", "must be a date in the past or in the present"));

    verifyNoMoreInteractions(pullRequestsReviewService);
  }

  @Test
  void getReviewStats_defaultUntilDate_appliesWhenMissing() {
    var since = LocalDate.now().minusDays(2);

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/reviews/stats")
            .queryParam("workspace", "acme")
            .queryParam("repo", "svc-a")
            .queryParam("sinceDate", since)
            .build())
        .header("Authorization", "Basic abc123==")
        .exchange()
        .expectStatus().isOk();

    var paramsCap = ArgumentCaptor.forClass(PullRequestReviewParams.class);
    verify(pullRequestsReviewService).getReviewStats(any(), paramsCap.capture());
    var p = paramsCap.getValue();

    assertThat(p.getSinceDate()).isEqualTo(since);
    assertThat(p.getUntilDate()).isNotNull();
    assertThat(p.getUntilDate()).isAfterOrEqualTo(p.getSinceDate());
  }

  @Test
  void getReviewStats_badDateFormat_returns400() {
    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/reviews/stats")
            .queryParam("workspace", "acme")
            .queryParam("repo", "svc-a")
            .queryParam("sinceDate", "2025-08-01")
            .queryParam("untilDate", "2025/08/10") // invalid format
            .build())
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void controller_missingAuth_returns401() {
    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/reviews/stats")
            .queryParam("workspace", "acme")
            .queryParam("repo", "svc-a")
            .queryParam("sinceDate", "2025-08-01")
            .build())
        .exchange()
        .expectStatus().isUnauthorized();

    verifyNoInteractions(pullRequestsReviewService);
  }
}
