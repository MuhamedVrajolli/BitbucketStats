package com.example.bitbucketstats.controllers.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

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
