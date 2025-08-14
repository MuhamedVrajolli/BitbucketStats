package com.example.bitbucketstats.controllers;

import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.request.MyPullRequestsParams;
import com.example.bitbucketstats.models.response.MyPullRequestsResponse;
import com.example.bitbucketstats.services.MyPullRequestsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequiredArgsConstructor
public class MyPullRequestsController {

  private final MyPullRequestsService myPullRequestsService;

  @GetMapping("/pull-requests/stats")
  public Mono<MyPullRequestsResponse> getMyPrStats(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestHeader(required = false) String username,
      @RequestHeader(required = false) String appPassword,
      @Valid MyPullRequestsParams params
  ) {
    return myPullRequestsService.getMyPullRequestsStats(
        BitbucketAuth.fromHeaders(authorization, username, appPassword), params);
  }
}
