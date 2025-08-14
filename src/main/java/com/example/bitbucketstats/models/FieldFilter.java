package com.example.bitbucketstats.models;

public record FieldFilter(String key, String value) {

  public static final  String AUTHOR_UUID = "author.uuid";
  public static final  String AUTHOR_NICKNAME = "author.nickname";
  public static final  String AUTHOR_USERNAME = "author.username";
  public static final  String REVIEWERS_UUID = "reviewers.uuid";

  public static FieldFilter of(String key, String value) {
    return new FieldFilter(key, value);
  }
}
