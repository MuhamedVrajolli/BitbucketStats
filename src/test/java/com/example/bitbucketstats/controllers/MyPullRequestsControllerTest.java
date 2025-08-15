package com.example.bitbucketstats.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.bitbucketstats.models.request.MyPullRequestsParams;
import com.example.bitbucketstats.models.response.MyPullRequestsResponse;
import com.example.bitbucketstats.services.MyPullRequestsService;
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
@WebFluxTest(MyPullRequestsController.class)
class MyPullRequestsControllerTest {

  @Autowired
  WebTestClient webTestClient;

  @MockitoBean
  MyPullRequestsService myPullRequestsService;

  @BeforeEach
  void setup() {
    when(myPullRequestsService.getMyPullRequestsStats(any(), any()))
        .thenReturn(Mono.just(new MyPullRequestsResponse(null, 0, null,
            null, null, null)));
  }

  @Test
  void getMyPrStats_bindsHeadersAndParams_andDelegates() {
    var since = LocalDate.of(2025, 8, 1);
    var until = LocalDate.of(2025, 8, 10);

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/stats")
            .queryParam("workspace", "acme")
            .queryParam("repo", "svc-a")
            .queryParam("repo", "svc-b")
            .queryParam("sinceDate", since)
            .queryParam("untilDate", until)
            .queryParam("state", "MERGED")
            .queryParam("state", "OPEN")
            .queryParam("queued", true)
            .queryParam("maxConcurrency", 4)
            .queryParam("includePullRequestDetails", true)
            .queryParam("includeDiffDetails", true)
            .queryParam("nickname", "nick")
            .build())
        .header("Authorization", "Basic abc")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

    // Verify service delegation and captured bound params
    var paramsCaptor = ArgumentCaptor.forClass(MyPullRequestsParams.class);
    verify(myPullRequestsService).getMyPullRequestsStats(any(), paramsCaptor.capture());
    var p = paramsCaptor.getValue();

    // Assertions on binding
    assertThat(p.getWorkspace()).isEqualTo("acme");
    assertThat(p.getRepo()).containsExactly("svc-a", "svc-b");
    assertThat(p.getSinceDate()).isEqualTo(since);
    assertThat(p.getUntilDate()).isEqualTo(until);
    assertThat(p.getState()).containsExactly("MERGED", "OPEN");
    assertThat(p.getQueued()).isTrue();
    assertThat(p.getMaxConcurrency()).isEqualTo(4);
    assertThat(p.isIncludePullRequestDetails()).isTrue();
    assertThat(p.isIncludeDiffDetails()).isTrue();
    assertThat(p.getNickname()).isEqualTo("nick");
  }

  @Test
  void getMyPrStats_missingRequiredParams_returns400WithConstraintMessages() {
    // No workspace, no repo, no sinceDate
    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/stats")
            .queryParam("untilDate", LocalDate.of(2025, 8, 10))
            .build())
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(String.class)
        .value(body -> {
          // Error payload structure can vary; check message fragments
          assertThat(body)
              .contains("workspace", "must not be blank");
          assertThat(body)
              .contains("repo", "must not be empty");
          assertThat(body)
              .contains("sinceDate", "must not be null");
        });

    verifyNoMoreInteractions(myPullRequestsService);
  }

  @Test
  void getMyPrStats_invalidDateRange_returns400() {
    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/stats")
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

    verifyNoMoreInteractions(myPullRequestsService);
  }

  @Test
  void getMyPrStats_futureDate_rejectedByPastOrPresent() {
    var future = LocalDate.now().plusDays(1);

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/stats")
            .queryParam("workspace", "acme")
            .queryParam("repo", "svc-a")
            .queryParam("sinceDate", future)   // violates @PastOrPresent
            .build())
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(String.class)
        .value(body -> assertThat(body)
            .contains("sinceDate", "must be a date in the past or in the present"));

    verifyNoMoreInteractions(myPullRequestsService);
  }

  @Test
  void getMyPrStats_defaultUntilDate_appliedWhenMissing() {
    var since = LocalDate.now().minusDays(2);

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/stats")
            .queryParam("workspace", "acme")
            .queryParam("repo", "svc-a")
            .queryParam("sinceDate", since)
            .build())
        .header("Authorization", "Basic abc")
        .exchange()
        .expectStatus().isOk();

    var paramsCaptor = ArgumentCaptor.forClass(MyPullRequestsParams.class);
    verify(myPullRequestsService).getMyPullRequestsStats(any(), paramsCaptor.capture());
    var p = paramsCaptor.getValue();

    assertThat(p.getSinceDate()).isEqualTo(since);
    // Field default should kick in when query param is absent
    assertThat(p.getUntilDate()).isNotNull();
    assertThat(p.getUntilDate())
        .isAfterOrEqualTo(p.getSinceDate()); // satisfies @AssertTrue
  }

  @Test
  void getMyPrStats_badDateFormat_returns400() {
    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/pull-requests/stats")
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
            .path("/pull-requests/stats")
            .queryParam("workspace", "acme")
            .queryParam("repo", "svc-a")
            .queryParam("sinceDate", "2025-08-01")
            .build())
        .exchange()
        .expectStatus().isUnauthorized();

    verifyNoInteractions(myPullRequestsService);
  }
}
