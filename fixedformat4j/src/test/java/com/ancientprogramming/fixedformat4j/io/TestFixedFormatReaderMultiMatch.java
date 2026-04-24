package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.io.read.RegexLinePattern;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;
import com.ancientprogramming.fixedformat4j.io.read.MultiMatchStrategy;

class TestFixedFormatReaderMultiMatch {

  private static Reader readerOf(String... lines) {
    return new StringReader(String.join("\n", lines));
  }

  @Test
  void firstMatchWinsWhenTwoPatternsMatch() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexLinePattern("^A"))
        .addMapping(TenCharRecord.class, new RegexLinePattern(".*"))
        .multiMatchStrategy(MultiMatchStrategy.firstMatch())
        .build();

    long count;
    try (Stream<Object> stream = reader.readAsStream(readerOf("AAAAAAAAAA"))) {
      count = stream.count();
    }
    assertEquals(1, count);
  }

  @Test
  void throwsOnAmbiguityWhenTwoPatternsMatch() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexLinePattern("^A"))
        .addMapping(TenCharRecord.class, new RegexLinePattern(".*"))
        .multiMatchStrategy(MultiMatchStrategy.throwOnAmbiguity())
        .build();

    FixedFormatException ex = assertThrows(FixedFormatException.class, () -> {
      try (Stream<Object> stream = reader.readAsStream(readerOf("AAAAAAAAAA"))) {
        stream.collect(Collectors.toList());
      }
    });
    assertTrue(ex.getMessage().contains("TenCharRecord"));
    assertTrue(ex.getMessage().contains("1"), "Should mention line 1: " + ex.getMessage());
  }

  @Test
  void noAmbiguityExceptionWhenOnlyOnePatternMatches() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexLinePattern("^A"))
        .addMapping(TenCharRecord.class, new RegexLinePattern("^B"))
        .multiMatchStrategy(MultiMatchStrategy.throwOnAmbiguity())
        .build();

    long count;
    try (Stream<Object> stream = reader.readAsStream(readerOf("AAAAAAAAAA"))) {
      count = stream.count();
    }
    assertEquals(1, count);
  }

  @Test
  void customStrategyNotCalledWhenOnlyOnePatternMatches() {
    AtomicBoolean called = new AtomicBoolean(false);
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexLinePattern("^A"))
        .addMapping(TenCharRecord.class, new RegexLinePattern("^B"))
        .multiMatchStrategy((matched, lineNumber) -> {
          called.set(true);
          return matched;
        })
        .build();

    try (Stream<Object> stream = reader.readAsStream(readerOf("AAAAAAAAAA"))) {
      stream.count();
    }
    assertFalse(called.get(), "MultiMatchStrategy.resolve() must not be called when only one pattern matches");
  }

  @Test
  void allMatchesEmitsTwoObjectsForOneMatchingLine() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexLinePattern("^A"))
        .addMapping(TenCharRecord.class, new RegexLinePattern(".*"))
        .multiMatchStrategy(MultiMatchStrategy.allMatches())
        .build();

    long count;
    try (Stream<Object> stream = reader.readAsStream(readerOf("AAAAAAAAAA"))) {
      count = stream.count();
    }
    assertEquals(2, count);
  }
}
