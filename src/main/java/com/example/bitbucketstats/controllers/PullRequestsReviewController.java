package com.example.bitbucketstats.controllers;

import com.example.bitbucketstats.models.BitbucketAuth;
import com.example.bitbucketstats.models.request.PullRequestReviewParams;
import com.example.bitbucketstats.models.response.PullRequestReviewResponse;
import com.example.bitbucketstats.services.PullRequestsReviewService;
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
class PullRequestsReviewController {

  private final PullRequestsReviewService pullRequestsReviewService;

  @GetMapping("/pull-requests/reviews/stats")
  public Mono<PullRequestReviewResponse> getReviewStats(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestHeader(required = false) String username,
      @RequestHeader(required = false) String appPassword,
      @Valid PullRequestReviewParams params
  ) {
    return pullRequestsReviewService.getReviewStats(
        BitbucketAuth.fromHeaders(authorization, username, appPassword), params);
  }
}

