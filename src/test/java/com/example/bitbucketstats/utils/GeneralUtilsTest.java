package com.example.bitbucketstats.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class GeneralUtilsTest {

  @Test
  void avg_handlesNormalAndZeroCount() {
    assertThat(GeneralUtils.avg(10, 2)).isEqualTo(5.0);
    assertThat(GeneralUtils.avg(3, 2)).isEqualTo(2.0); // rounded
    assertThat(GeneralUtils.avg(0, 0)).isEqualTo(0.0);
  }

  @Test
  void pct_handlesNormalAndZeroTotal() {
    assertThat(GeneralUtils.pct(1, 4)).isEqualTo(25.0);
    assertThat(GeneralUtils.pct(1, 3)).isEqualTo(33.33); // rounded to 2 decimals
    assertThat(GeneralUtils.pct(0, 0)).isEqualTo(0.0);
  }

  @Test
  void period_formatsCorrectly() {
    var s = LocalDate.of(2025, 8, 1);
    var u = LocalDate.of(2025, 8, 10);
    assertThat(GeneralUtils.period(s, u))
        .isEqualTo("FROM: 2025-08-01 TO: 2025-08-10");
  }

  @Test
  void safeInt_returnsZeroForNull_orValueOtherwise() {
    assertThat(GeneralUtils.safeInt(null)).isZero();
  }

  @Test
  void quote_wrapsAndEscapesQuotes() {
    assertThat(GeneralUtils.quote("hello")).isEqualTo("\"hello\"");
    assertThat(GeneralUtils.quote("he\"llo")).isEqualTo("\"he\\\"llo\"");
  }

  @Test
  void urlEncode_encodesUtf8() {
    assertThat(GeneralUtils.urlEncode("a b+c"))
        .isEqualTo("a+b%2Bc"); // space -> +, plus -> %2B
  }

  @Test
  void isUrlAbsolute() {
    assertTrue(GeneralUtils.isUrlAbsolute("https://example.com/test"));
    assertFalse(GeneralUtils.isUrlAbsolute("/test"));
    assertFalse(GeneralUtils.isUrlAbsolute("/https://example.com/has a space"));
  }
}
