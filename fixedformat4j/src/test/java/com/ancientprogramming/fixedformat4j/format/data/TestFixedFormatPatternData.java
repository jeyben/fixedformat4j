package com.ancientprogramming.fixedformat4j.format.data;

import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFixedFormatPatternData {

  @Test
  void getPattern_returnsConstructorArg() {
    FixedFormatPatternData data = new FixedFormatPatternData("yyyyMMdd");
    assertEquals("yyyyMMdd", data.getPattern());
  }

  @Test
  void getPattern_distinctForDifferentPatterns() {
    FixedFormatPatternData date = new FixedFormatPatternData("yyyyMMdd");
    FixedFormatPatternData datetime = new FixedFormatPatternData("yyyy-MM-dd'T'HH:mm:ss");
    assertNotEquals(date.getPattern(), datetime.getPattern());
  }

  @Test
  void getPattern_exactlyMatchesInput() {
    String pattern = "dd/MM/yyyy HH:mm";
    assertEquals(pattern, new FixedFormatPatternData(pattern).getPattern());
  }

  @Test
  void defaultInstance_pattern_matchesDatePattern() {
    assertEquals(FixedFormatPattern.DATE_PATTERN, FixedFormatPatternData.DEFAULT.getPattern());
  }

  @Test
  void localDateDefaultInstance_pattern_matchesLocalDatePattern() {
    assertEquals(FixedFormatPattern.LOCALDATE_PATTERN, FixedFormatPatternData.LOCALDATE_DEFAULT.getPattern());
  }

  @Test
  void datetimeDefaultInstance_pattern_matchesDatetimePattern() {
    assertEquals(FixedFormatPattern.DATETIME_PATTERN, FixedFormatPatternData.DATETIME_DEFAULT.getPattern());
  }

  @Test
  void threeDefaults_haveDistinctPatterns() {
    assertNotEquals(FixedFormatPatternData.DEFAULT.getPattern(),
        FixedFormatPatternData.LOCALDATE_DEFAULT.getPattern());
    assertNotEquals(FixedFormatPatternData.DEFAULT.getPattern(),
        FixedFormatPatternData.DATETIME_DEFAULT.getPattern());
    assertNotEquals(FixedFormatPatternData.LOCALDATE_DEFAULT.getPattern(),
        FixedFormatPatternData.DATETIME_DEFAULT.getPattern());
  }

  @Test
  void toString_containsPattern() {
    FixedFormatPatternData data = new FixedFormatPatternData("yyyyMMdd");
    assertTrue(data.toString().contains("yyyyMMdd"),
        "toString should contain pattern: " + data.toString());
  }

  @Test
  void toString_containsClassName() {
    FixedFormatPatternData data = new FixedFormatPatternData("yyyyMMdd");
    assertTrue(data.toString().contains("FixedFormatPatternData"),
        "toString should contain class name: " + data.toString());
  }

  @Test
  void toString_containsPatternFieldLabel() {
    FixedFormatPatternData data = new FixedFormatPatternData("yyyyMMdd");
    assertTrue(data.toString().contains("pattern"),
        "toString should label the pattern field: " + data.toString());
  }

  @Test
  void toString_distinctForDifferentPatterns() {
    FixedFormatPatternData a = new FixedFormatPatternData("yyyyMMdd");
    FixedFormatPatternData b = new FixedFormatPatternData("ddMMyyyy");
    assertNotEquals(a.toString(), b.toString());
  }
}
