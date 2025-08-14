package com.example.bitbucketstats.models.response;

import java.util.List;

public record MyPullRequestsResponse(
    String period,
    int totalMyPrs,
    Double avgTimeOpenHours,
    Double avgCommentCount,
    List<MyPullRequestsSummary> myPrDetails
) {

}
