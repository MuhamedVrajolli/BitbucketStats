package com.example.bitbucketstats.itests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.bitbucketstats.configuration.HttpRetryConfig;
import com.example.bitbucketstats.configuration.HttpRetryOverrideConfig;
import com.example.bitbucketstats.integration.BitbucketClient;
import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.bitbucket.PullRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.Fault;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

@Tag("integration")
@Import(HttpRetryOverrideConfig.class)
@ImportAutoConfiguration(exclude = HttpRetryConfig.class)
class HttpRetryIT extends BaseIT {

  @Autowired ObjectMapper om;
  @Autowired BitbucketClient client;

  @Test
  void retries_on_5xx_then_succeeds() throws Exception {
    // 1st call -> 502
    stubFor(get(urlPathEqualTo("/repositories/ws/repo-1/pullrequests/101"))
        .inScenario("5xx-then-200")
        .whenScenarioStateIs(STARTED)
        .willReturn(aResponse()
            .withStatus(502)
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBody(om.writeValueAsString(Map.of("error", "bad gateway"))))
        .willSetStateTo("RETRY"));

    // 2nd call -> 200
    var body200 = Map.of("id", 101, "title", "ok after retry");
    stubFor(get(urlPathEqualTo("/repositories/ws/repo-1/pullrequests/101"))
        .inScenario("5xx-then-200")
        .whenScenarioStateIs("RETRY")
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBody(om.writeValueAsString(body200))));

    StepVerifier.create(
            client.retrieveJson(auth(), "/repositories/ws/repo-1/pullrequests/101", PullRequest.class))
        .assertNext(pr -> {
          assertThat(pr.id()).isEqualTo(101);
          assertThat(pr.title()).contains("ok");
        })
        .expectComplete()
        .verify();

    verify(2, getRequestedFor(urlPathEqualTo("/repositories/ws/repo-1/pullrequests/101")));
  }

  @Test
  void does_not_retry_on_4xx() throws Exception {
    stubJson("/repositories/ws/repo-1/pullrequests/102",
        400, Map.of("error", "bad request"));

    StepVerifier.create(
            client.retrieveJson(auth(), "/repositories/ws/repo-1/pullrequests/102", PullRequest.class))
        .expectErrorSatisfies(err ->
            assertThat(err).isInstanceOf(WebClientResponseException.BadRequest.class))
        .verify();

    verify(1, getRequestedFor(urlPathEqualTo("/repositories/ws/repo-1/pullrequests/102")));
  }

  @Test
  void retries_up_to_max_then_errors() throws Exception {
    // Always 502 (initial + 3 retries = 4 total)
    stubJson("/repositories/ws/repo-1/pullrequests/103",
        502, Map.of("error", "bad gateway"));

    StepVerifier.create(
            client.retrieveJson(auth(), "/repositories/ws/repo-1/pullrequests/103", PullRequest.class))
        .expectErrorSatisfies(err -> {
          assertThat(err.getCause()).isInstanceOf(WebClientResponseException.class);
          var ex = (WebClientResponseException) err.getCause();
          assertThat(ex.getStatusCode().value()).isEqualTo(502);
        })
        .verify();

    verify(4, getRequestedFor(urlPathEqualTo("/repositories/ws/repo-1/pullrequests/103")));
  }

  @Test
  void retries_on_timeout_or_abort() {
    // Simulate broken connection -> should result in a retryable IO/Netty exception
    stubFor(get(urlPathEqualTo("/break"))
        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    StepVerifier.create(client.retrieveJson(auth(), "/break", Map.class))
        .expectError() // exhausts retries and errors
        .verify();

    verify(moreThanOrExactly(2), getRequestedFor(urlPathEqualTo("/break")));
  }

  private void stubJson(String url, int status, Object body) throws Exception {
    stubFor(get(urlPathEqualTo(url))
        .willReturn(aResponse()
            .withStatus(status)
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBody(om.writeValueAsString(body))));
  }

  private BitbucketAuth auth() {
    return new BitbucketAuth("Basic auth", "", "");
  }
}
