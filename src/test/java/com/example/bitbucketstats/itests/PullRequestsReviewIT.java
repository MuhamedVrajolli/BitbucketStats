package com.example.bitbucketstats.itests;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

@Tag("integration")
class PullRequestsReviewIT extends BaseIT {

  @Autowired
  WebTestClient webTestClient;

  @Test
  void prReviewStats_basic() {
    webTestClient.get()
        .uri(b -> b.path("/pull-requests/reviews/stats")
            .queryParam("workspace", "ws")
            .queryParam("repo", "repo-1")
            .queryParam("sinceDate", "2025-07-01")
            .queryParam("untilDate", "2025-08-01")
            .build())
        .header("username", "john-doe")
        .header("appPassword", "password")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.period").isEqualTo("FROM: 2025-07-01 TO: 2025-08-01")
        .jsonPath("$.total_pull_requests_approved").isEqualTo(3)
        .jsonPath("$.total_pull_requests_reviewed").isEqualTo(4)
        .jsonPath("$.approved_percentage").isEqualTo(75.0);

    // Verify that the request to the user endpoint is made to resolve reviewer uuid
    verify(exactly(1), getRequestedFor(urlPathEqualTo("/user")));
  }

  @Test
  void prReviewStats_withReviewerUuid_includeCommentDetails() {
    webTestClient.get()
        .uri(b -> b.path("/pull-requests/reviews/stats")
            .queryParam("workspace", "ws")
            .queryParam("repo", "repo-1")
            .queryParam("sinceDate", "2025-07-01")
            .queryParam("untilDate", "2025-08-01")
            .queryParam("reviewerUuid", "1a2b3c4d-0000-1111-2222-333344445555")
            .queryParam("includeCommentDetails", true)
            .build())
        .header("username", "john-doe")
        .header("appPassword", "password")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
        .expectBody()
        // summary fields
        .jsonPath("$.period").isEqualTo("FROM: 2025-07-01 TO: 2025-08-01")
        .jsonPath("$.total_pull_requests_approved").isEqualTo(3)
        .jsonPath("$.total_pull_requests_reviewed").isEqualTo(4)
        .jsonPath("$.total_pull_requests_commented").isEqualTo(2)
        .jsonPath("$.total_comments").isEqualTo(2)
        .jsonPath("$.approved_percentage").isEqualTo(75.0)
        .jsonPath("$.commented_percentage").isEqualTo(50.0)
        // pull_requests_commented array basics
        .jsonPath("$.pull_requests_commented").isArray()
        .jsonPath("$.pull_requests_commented.length()").isEqualTo(2)
        // first item
        .jsonPath("$.pull_requests_commented[0].id").isEqualTo(201)
        .jsonPath("$.pull_requests_commented[0].title").isEqualTo("Add retry policy to client")
        .jsonPath("$.pull_requests_commented[0].link").isEqualTo("https://bitbucket.org/ws/repo-1/pull-requests/201")
        .jsonPath("$.pull_requests_commented[0].comments_made").isEqualTo(1)
        .jsonPath("$.pull_requests_commented[0].repo").isEqualTo("repo-1")
        // second item
        .jsonPath("$.pull_requests_commented[1].id").isEqualTo(203)
        .jsonPath("$.pull_requests_commented[1].title").isEqualTo("Fix NPE in diff aggregation")
        .jsonPath("$.pull_requests_commented[1].link").isEqualTo("https://bitbucket.org/ws/repo-1/pull-requests/203")
        .jsonPath("$.pull_requests_commented[1].comments_made").isEqualTo(1)
        .jsonPath("$.pull_requests_commented[1].repo").isEqualTo("repo-1");

    // Verify that the request to the user endpoint is not made when reviewer uuid provided
    verify(0, getRequestedFor(urlPathEqualTo("/user")));
  }
}
