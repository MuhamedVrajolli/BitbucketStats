package com.example.bitbucketstats.models.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/** Common query params shared by both endpoints. */
@Data
public class BaseParams {
  /** e.g. "acme" */
  @NotBlank
  private String workspace;

  /** Multiple repositories: bind from ?repo=a&repo=b */
  @NotEmpty
  private List<String> repo;

  /** ISO date, required: yyyy-MM-dd */
  @NotNull
  @PastOrPresent
  @DateTimeFormat(iso = ISO.DATE)
  private LocalDate sinceDate;

  /** ISO date, optional: yyyy-MM-dd */
  @NotNull
  @PastOrPresent
  @DateTimeFormat(iso = ISO.DATE)
  private LocalDate untilDate = LocalDate.now(ZoneId.systemDefault());

  /** Bind from `?state=OPEN&state=MERGED` (Bitbucket supports IN(...)) */
  private List<String> state = List.of("MERGED");

  /** Optional draft filter: queued=true|false */
  private Boolean queued;

  /** Concurrency cap for parallel fetches */
  private int maxConcurrency = 8;

  @AssertTrue(message = "sinceDate must be ≤ untilDate")
  public boolean isDateRangeValid() {
    return sinceDate == null || untilDate == null || !sinceDate.isAfter(untilDate);
  }
}

