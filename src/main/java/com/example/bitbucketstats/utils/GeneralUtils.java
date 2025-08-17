package com.example.bitbucketstats.utils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GeneralUtils {

  public static double avg(long sum, int count) {
    return count == 0 ? 0 : Math.round((double) sum / count);
  }

  public static double pct(int part, int total) {
    if (total == 0) return 0;
    double v = (part * 100.0) / total;
    return Math.round(v * 100.0) / 100.0; // round 2 decimals
  }

  public static String period(LocalDate since, LocalDate until) {
    return String.format("FROM: %s TO: %s", since, until);
  }

  public static int safeInt(Integer v) {
    return v == null ? 0 : v;
  }

  public static String quote(String s) {
    return "\"" + s.replace("\"", "\\\"") + "\"";
  }

  public static String urlEncode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  public static boolean isUrlAbsolute(String url) {
    try {
      return URI.create(url).isAbsolute();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
