package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatReaderMultiMatch {

  private static Reader readerOf(String... lines) {
    return new StringReader(String.join("\n", lines));
  }

  @Test
  void firstMatchWinsWhenTwoPatternsMatch() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^A"))
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .multiMatchStrategy(MultiMatchStrategy.firstMatch())
        .build();

    List<TenCharRecord> results;
    try (Stream<TenCharRecord> stream = reader.readAsStream(readerOf("AAAAAAAAAA"))) {
      results = stream.collect(Collectors.toList());
    }
    assertEquals(1, results.size());
  }

  @Test
  void throwsOnAmbiguityWhenTwoPatternsMatch() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^A"))
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .multiMatchStrategy(MultiMatchStrategy.throwOnAmbiguity())
        .build();

    FixedFormatException ex = assertThrows(FixedFormatException.class, () -> {
      try (Stream<TenCharRecord> stream = reader.readAsStream(readerOf("AAAAAAAAAA"))) {
        stream.collect(Collectors.toList());
      }
    });
    assertTrue(ex.getMessage().contains("TenCharRecord"));
    assertTrue(ex.getMessage().contains("1"), "Should mention line 1: " + ex.getMessage());
  }

  @Test
  void noAmbiguityExceptionWhenOnlyOnePatternMatches() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^A"))
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^B"))
        .multiMatchStrategy(MultiMatchStrategy.throwOnAmbiguity())
        .build();

    List<TenCharRecord> results;
    try (Stream<TenCharRecord> stream = reader.readAsStream(readerOf("AAAAAAAAAA"))) {
      results = stream.collect(Collectors.toList());
    }
    assertEquals(1, results.size());
  }

  @Test
  void allMatchesEmitsTwoObjectsForOneMatchingLine() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^A"))
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .multiMatchStrategy(MultiMatchStrategy.allMatches())
        .build();

    List<TenCharRecord> results;
    try (Stream<TenCharRecord> stream = reader.readAsStream(readerOf("AAAAAAAAAA"))) {
      results = stream.collect(Collectors.toList());
    }
    assertEquals(2, results.size());
  }
}
