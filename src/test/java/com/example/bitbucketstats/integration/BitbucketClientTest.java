package com.example.bitbucketstats.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bitbucketstats.configuration.BitbucketHttpProperties;
import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.bitbucket.User;
import com.example.bitbucketstats.models.page.Page;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BitbucketClientTest {

  @Mock
  private WebClient webClient;
  @Mock
  private BitbucketHttpProperties bitbucketHttpProperties;
  @Mock
  private Retry retryPolicy;

  @InjectMocks
  private BitbucketClient client;

  @Test
  void fetchAll_pagesThrough_next_and_flattens_values() {
    // Spy so we can stub retrieveJson(...) only
    var spy = Mockito.spy(client);

    var auth = new BitbucketAuth("abc123==", "user", "app");
    String firstUrl = "/repositories/acme/svc-a/pullrequests?page=1";
    String nextUrl  = "/repositories/acme/svc-a/pullrequests?page=2";

    // Mock two pages of strings
    Page<String> page1 = mock(Page.class);
    when(page1.next()).thenReturn(nextUrl);
    when(page1.values()).thenReturn(List.of("A", "B"));

    Page<String> page2 = mock(Page.class);
    when(page2.next()).thenReturn(null);
    when(page2.values()).thenReturn(List.of("C"));

    // Stub retrieveJson for first + next URL
    doReturn(Mono.just(page1)).when(spy).retrieveJson(same(auth), eq(firstUrl), any());
    doReturn(Mono.just(page2)).when(spy).retrieveJson(same(auth), eq(nextUrl), any());

    var flux = spy.fetchAll(auth, firstUrl, Page.class);

    StepVerifier.create(flux)
        .expectNext("A", "B", "C")
        .verifyComplete();

    // Verify we followed pagination exactly twice (first + next)
    verify(spy).retrieveJson(same(auth), eq(firstUrl), any());
    verify(spy).retrieveJson(same(auth), eq(nextUrl), any());
  }

  @Test
  void fetchAll_handles_nullValues_and_noNext_yieldsEmpty() {
    var spy = Mockito.spy(client);

    var auth = new BitbucketAuth("abc123==", "user", "app");
    String firstUrl = "/something?page=1";

    Page<String> page = mock(Page.class);
    when(page.next()).thenReturn(null);
    when(page.values()).thenReturn(null); // should be treated as empty

    doReturn(Mono.just(page)).when(spy).retrieveJson(same(auth), eq(firstUrl), any());

    var flux = spy.fetchAll(auth, firstUrl, Page.class);

    StepVerifier.create(flux)
        .verifyComplete(); // no items

    verify(spy).retrieveJson(same(auth), eq(firstUrl), any());
  }

  @Test
  void retrieveJson_buildsGet_withUri_appliesAuthHeader_registersOnStatus_and_mapsBody() {
    var auth = new BitbucketAuth("abc123==", "user", "app");
    String url = "https://api.bitbucket.org/2.0/user";

    var getSpec = mock(WebClient.RequestHeadersUriSpec.class);
    when(webClient.get()).thenReturn(getSpec);

    var headersSpec = mock(WebClient.RequestHeadersSpec.class);
    // use doReturn to avoid wildcard capture issues
    doReturn(getSpec).when(getSpec).uri(any(URI.class));
    doReturn(headersSpec).when(getSpec).headers(any());

    var responseSpec = mock(WebClient.ResponseSpec.class);
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

    var body = mock(User.class);
    when(responseSpec.bodyToMono(User.class)).thenReturn(Mono.just(body));
    when(retryPolicy.generateCompanion(any()))
        .thenAnswer(inv -> Retry.max(0).generateCompanion(inv.getArgument(0)));

    var mono = client.retrieveJson(auth, url, User.class);

    StepVerifier.create(mono).expectNext(body).verifyComplete();

    verify(webClient).get();
    verify(getSpec).uri(URI.create(url));

    var headersCap = ArgumentCaptor.forClass(java.util.function.Consumer.class);
    verify(getSpec).headers(headersCap.capture());

    var applied = new HttpHeaders();
    headersCap.getValue().accept(applied);
    assertThat(applied.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Basic abc123==");

    verify(responseSpec).onStatus(any(), any());
    verify(responseSpec).bodyToMono(User.class);
  }
}
