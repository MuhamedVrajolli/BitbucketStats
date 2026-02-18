package com.example.bitbucketstats.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler advice = new GlobalExceptionHandler();

  private static ServerHttpRequest req(String path) {
    return MockServerHttpRequest.get(path).build();
  }

  @Test
  void webExchangeBind_returns400_withViolations() {
    // Build a binding error with a field + a global error
    var bex = new WebExchangeBindException(
        new MethodParameter(GlobalExceptionHandlerTest.class.getDeclaredMethods()[0], -1),
        new BeanPropertyBindingResult(new Object(), "obj"));
    bex.addError(new FieldError("obj", "workspace", "must not be blank"));
    bex.addError(new ObjectError("baseParams", "sinceDate must be ≤ untilDate"));

    var resp = advice.handleWebExchangeBind(bex, req("/pull-requests/stats"));

    assertThat(resp.getStatusCode().value()).isEqualTo(400);
    var body = resp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(400);
    assertThat(body.error()).contains("BAD_REQUEST");
    assertThat(body.path()).isEqualTo("/pull-requests/stats");
    assertThat(body.errors()).extracting("field")
        .contains("workspace", "baseParams");
    assertThat(body.errors()).extracting("message")
        .contains("must not be blank", "sinceDate must be ≤ untilDate");
    assertThat(body.timestamp()).isNotBlank(); // don’t assert exact time
  }

  @Test
  void webClientResponse_passthroughStatus_andPrefersResponseBody() {
    var ex = WebClientResponseException.create(
        502, "Bad Gateway", HttpHeaders.EMPTY, "Upstream says nope".getBytes(), null);

    var resp = advice.handleWebClientResponse(ex, req("/pull-requests/reviews/stats"));
    assertThat(resp.getStatusCode().value()).isEqualTo(502);
    var body = resp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(502);
    assertThat(body.message()).isEqualTo("Upstream says nope");
    assertThat(body.path()).isEqualTo("/pull-requests/reviews/stats");
  }

  @Test
  void any_unhandled_returns500_withMessage() {
    var ex = new RuntimeException("boom");
    var resp = advice.handleAny(ex, req("/x"));
    assertThat(resp.getStatusCode().value()).isEqualTo(500);
    var body = resp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(500);
    assertThat(body.error()).contains("INTERNAL_SERVER_ERROR");
    assertThat(body.message()).isEqualTo("boom");
    assertThat(body.path()).isEqualTo("/x");
  }
}
