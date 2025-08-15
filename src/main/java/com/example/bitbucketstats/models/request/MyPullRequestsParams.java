package com.example.bitbucketstats.models.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Query params for: GET /stats/my-prs */
@Data
@EqualsAndHashCode(callSuper = true)
public class MyPullRequestsParams extends BaseParams {

  /** Include details array in response */
  private boolean includePullRequestDetails = false;

  /** Include details array in response */
  private boolean includeDiffDetails = false;

  /** Optional author nickname filter */
  private String nickname;
}
