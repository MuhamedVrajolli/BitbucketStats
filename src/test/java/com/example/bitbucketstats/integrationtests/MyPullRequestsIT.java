package com.example.bitbucketstats.integrationtests;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

class MyPullRequestsIT extends BaseTestIT {

  @Autowired
  WebTestClient webTestClient;

  @Test
  void myPrStats_basic() {
    webTestClient.get()
        .uri(b -> b.path("/pull-requests/stats")
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
        .jsonPath("$.total_pull_requests").isEqualTo(3)
        .jsonPath("$.avg_time_open_hours").isEqualTo(66.0)
        .jsonPath("$.avg_comment_count").isEqualTo(2.0);
  }

  @Test
  void myPrStats_includes_pr_and_diff_details() {
    webTestClient.get()
        .uri(b -> b.path("/pull-requests/stats")
            .queryParam("workspace", "ws")
            .queryParam("repo", "repo-1")
            .queryParam("sinceDate", "2025-07-01")
            .queryParam("untilDate", "2025-08-01")
            .queryParam("includePullRequestDetails", true)
            .queryParam("includeDiffDetails", true)
            .build())
        .header("username", "john-doe")
        .header("appPassword", "password")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
        .expectBody()
        // top-level
        .jsonPath("$.period").isEqualTo("FROM: 2025-07-01 TO: 2025-08-01")
        .jsonPath("$.total_pull_requests").isEqualTo(3)
        .jsonPath("$.avg_time_open_hours").isEqualTo(66.0)
        .jsonPath("$.avg_comment_count").isEqualTo(2.0)
        .jsonPath("$.avg_files_changed").isEqualTo(2.0)
        .jsonPath("$.pull_request_details.length()").isEqualTo(3)

        // PR #101
        .jsonPath("$.pull_request_details[0].id").isEqualTo(101)
        .jsonPath("$.pull_request_details[0].title").isEqualTo("Fix NPE in diff aggregation")
        .jsonPath("$.pull_request_details[0].link").isEqualTo("https://bitbucket.org/ws/repo-1/pull-requests/101")
        .jsonPath("$.pull_request_details[0].time_open_hours").isEqualTo(175)
        .jsonPath("$.pull_request_details[0].comment_count").isEqualTo(4)
        .jsonPath("$.pull_request_details[0].repo").isEqualTo("repo-1")
        .jsonPath("$.pull_request_details[0].diff_details.files_changed").isEqualTo(2)
        .jsonPath("$.pull_request_details[0].diff_details.lines_added").isEqualTo(43)
        .jsonPath("$.pull_request_details[0].diff_details.lines_removed").isEqualTo(3)

        // PR #102
        .jsonPath("$.pull_request_details[1].id").isEqualTo(102)
        .jsonPath("$.pull_request_details[1].title").isEqualTo("Add cache layer for PR stats")
        .jsonPath("$.pull_request_details[1].link").isEqualTo("https://bitbucket.org/ws/repo-1/pull-requests/102")
        .jsonPath("$.pull_request_details[1].time_open_hours").isEqualTo(22)
        .jsonPath("$.pull_request_details[1].comment_count").isEqualTo(1)
        .jsonPath("$.pull_request_details[1].repo").isEqualTo("repo-1")
        .jsonPath("$.pull_request_details[1].diff_details.files_changed").isEqualTo(1)
        .jsonPath("$.pull_request_details[1].diff_details.lines_added").isEqualTo(6)
        .jsonPath("$.pull_request_details[1].diff_details.lines_removed").isEqualTo(10)

        // PR #103
        .jsonPath("$.pull_request_details[2].id").isEqualTo(103)
        .jsonPath("$.pull_request_details[2].title").isEqualTo("Instrumentation for latency metrics")
        .jsonPath("$.pull_request_details[2].link").isEqualTo("https://bitbucket.org/ws/repo-1/pull-requests/103")
        .jsonPath("$.pull_request_details[2].time_open_hours").isEqualTo(1)
        .jsonPath("$.pull_request_details[2].comment_count").isEqualTo(0)
        .jsonPath("$.pull_request_details[2].repo").isEqualTo("repo-1")
        .jsonPath("$.pull_request_details[2].diff_details.files_changed").isEqualTo(2)
        .jsonPath("$.pull_request_details[2].diff_details.lines_added").isEqualTo(35)
        .jsonPath("$.pull_request_details[2].diff_details.lines_removed").isEqualTo(2);
  }

  @Test
  void test() {
    var str = webTestClient.get()
        .uri(b -> b.path("/pull-requests/stats")
            .queryParam("workspace", "ws")
            .queryParam("repo", "repo-1")
            .queryParam("sinceDate", "2025-07-01")
            .queryParam("untilDate", "2025-08-01")
            .queryParam("includePullRequestDetails", true)
            .queryParam("includeDiffDetails", true)
            .build())
        .header("username", "john-doe")
        .header("appPassword", "password")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
        .expectBody(String.class)
        .returnResult();

    System.out.println("RESPONSE:\n" + str);
  }
}

