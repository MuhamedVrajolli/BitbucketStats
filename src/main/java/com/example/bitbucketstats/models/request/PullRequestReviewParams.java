package com.example.bitbucketstats.models.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Query params for: GET /stats/reactive-pr-reviews */
@Data
@EqualsAndHashCode(callSuper = true)
public class PullRequestReviewParams extends BaseParams {

  /** Optional reviewer filter: reviewers.uuid="..." */
  private String reviewerUuid;

  /** Include comment details in response */
  private boolean includeCommentDetails = false;
}
