package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;
import com.ancientprogramming.fixedformat4j.io.read.MultiMatchStrategy;

class TestFixedFormatReaderMultiMatch {

  private static Reader readerOf(String... lines) {
    return new StringReader(String.join("\n", lines));
  }

  @Test
  void firstMatchEmitsOneRecordWhenTwoPatternsMatch() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .multiMatchStrategy(MultiMatchStrategy.firstMatch())
        .build();

    long count = reader.read(readerOf("AAAAAAAAAA")).getAll().size();
    assertEquals(1, count);
  }

  @Test
  void firstMatchPicksDeeperPatternEvenWhenRegisteredLater() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(FiveCharRecord.class, LinePattern.matchAll())     // depth 0, registered first
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))     // depth 1, registered second
        .multiMatchStrategy(MultiMatchStrategy.firstMatch())
        .build();

    List<Object> all = reader.read(readerOf("AAAAAAAAAA")).getAll();
    assertEquals(1, all.size());
    assertInstanceOf(TenCharRecord.class, all.get(0));
  }

  @Test
  void firstMatchUsesRegistrationOrderAmongSameDepthMatches() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())   // depth 0, registered first
        .addMapping(FiveCharRecord.class, LinePattern.matchAll())   // depth 0, registered second
        .multiMatchStrategy(MultiMatchStrategy.firstMatch())
        .build();

    List<Object> all = reader.read(readerOf("AAAAAAAAAA")).getAll();
    assertEquals(1, all.size());
    assertInstanceOf(TenCharRecord.class, all.get(0));
  }

  @Test
  void throwsOnAmbiguityWhenTwoPatternsMatch() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .multiMatchStrategy(MultiMatchStrategy.throwOnAmbiguity())
        .build();

    FixedFormatException ex = assertThrows(FixedFormatException.class, () ->
        reader.read(readerOf("AAAAAAAAAA")));
    assertTrue(ex.getMessage().contains("TenCharRecord"));
    assertTrue(ex.getMessage().contains("1"), "Should mention line 1: " + ex.getMessage());
  }

  @Test
  void noAmbiguityExceptionWhenOnlyOnePatternMatches() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .addMapping(TenCharRecord.class, LinePattern.prefix("B"))
        .multiMatchStrategy(MultiMatchStrategy.throwOnAmbiguity())
        .build();

    long count = reader.read(readerOf("AAAAAAAAAA")).getAll().size();
    assertEquals(1, count);
  }

  @Test
  void customStrategyNotCalledWhenOnlyOnePatternMatches() {
    AtomicBoolean called = new AtomicBoolean(false);
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .addMapping(TenCharRecord.class, LinePattern.prefix("B"))
        .multiMatchStrategy((matched, lineNumber) -> {
          called.set(true);
          return matched;
        })
        .build();

    reader.read(readerOf("AAAAAAAAAA"));
    assertFalse(called.get(), "MultiMatchStrategy.resolve() must not be called when only one pattern matches");
  }

  @Test
  void allMatchesEmitsTwoObjectsForOneMatchingLine() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .multiMatchStrategy(MultiMatchStrategy.allMatches())
        .build();

    long count = reader.read(readerOf("AAAAAAAAAA")).getAll().size();
    assertEquals(2, count);
  }
}
