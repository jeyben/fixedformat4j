package com.ancientprogramming.fixedformat4j.issues;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.read.ReadResult;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Issue 98.
 *
 * A positional pattern at offset 127 must silently no-match lines shorter than
 * 128 characters rather than throwing StringIndexOutOfBoundsException.
 * The guard lives in RecordMappingIndex.findMatches() and these tests pin it.
 */
public class TestIssue98 {

  @Record(length = 50)
  public static class HeaderRecord {
    private String prefix;

    @Field(offset = 1, length = 3)
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
  }

  /** Represents a long sub-type record identified at position 127 (0-indexed). */
  @Record(length = 128)
  public static class SubTypeRecord {
    private String marker;

    @Field(offset = 128, length = 1)
    public String getMarker() { return marker; }
    public void setMarker(String marker) { this.marker = marker; }
  }

  @Record(length = 50)
  public static class CatchAllRecord {
    private String data;

    @Field(offset = 1, length = 3)
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
  }

  private static final LinePattern POSITIONAL_AT_127 =
      LinePattern.positional(new int[]{127}, "X");

  /** 50-char line starting with "HDR" — shorter than the 128 chars SubTypeRecord needs. */
  private static String shortHdrLine() {
    return String.format("HDR%47s", "");
  }

  @Test
  void positionalAt127DoesNotThrowForShortLine_prefixMappingStillFires() {
    // positional pattern at 127 + prefix "HDR"; feed a 50-char HDR line
    // → only the prefix mapping fires, no StringIndexOutOfBoundsException
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(HeaderRecord.class, LinePattern.prefix("HDR"))
        .addMapping(SubTypeRecord.class, POSITIONAL_AT_127)
        .build();

    ReadResult result = assertDoesNotThrow(() ->
        reader.read(new StringReader(shortHdrLine())));

    assertEquals(1, result.get(HeaderRecord.class).size(),
        "HDR prefix mapping must fire for the short HDR line");
    assertTrue(result.get(SubTypeRecord.class).isEmpty(),
        "Positional-at-127 mapping must not fire for a 50-char line");
  }

  @Test
  void positionalAt127DoesNotThrowForShortLine_catchAllReceivesLine() {
    // positional pattern at 127 + matchAll() catch-all; feed a 50-char line
    // → no exception, line lands in the catch-all because the positional silently no-matches
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(CatchAllRecord.class, LinePattern.matchAll())
        .addMapping(SubTypeRecord.class, POSITIONAL_AT_127)
        .build();

    String shortLine = "ABC" + " ".repeat(47);
    ReadResult result = assertDoesNotThrow(() ->
        reader.read(new StringReader(shortLine)));

    assertEquals(1, result.get(CatchAllRecord.class).size(),
        "matchAll mapping must catch the short line when positional silently no-matches");
    assertTrue(result.get(SubTypeRecord.class).isEmpty(),
        "Positional-at-127 mapping must not fire for a 50-char line");
  }
}
