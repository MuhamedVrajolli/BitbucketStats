package com.example.bitbucketstats.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GeneralUtils {

  public static double round2(double v) {
    return Math.round(v * 100.0) / 100.0;
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
}
