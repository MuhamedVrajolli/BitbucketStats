package com.example.bitbucketstats.models.request;

import static com.example.bitbucketstats.utils.GeneralUtils.addBracesToUuid;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PullRequestReviewParams extends BaseParams {

  /** Optional reviewer filter: reviewers.uuid="..." */
  private String reviewerUuid;

  /** Include comment details in response */
  private boolean includeCommentDetails = false;

  public void setReviewerUuid(String reviewerUuid) {
    this.reviewerUuid = addBracesToUuid(reviewerUuid);
  }
}
