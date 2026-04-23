package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TestStrategiesAndHandlers {

  private static final FixedFormatMatchPattern ANY = new RegexFixedFormatMatchPattern(".*");
  private static final String THREE_LINES = "line1     \nline2     \nline3     ";

  @SuppressWarnings("unchecked")
  private static FixedFormatManager failOnLine(int failLine) {
    AtomicInteger calls = new AtomicInteger(0);
    return new FixedFormatManager() {
      @Override public <T> T load(Class<T> clazz, String data) {
        if (calls.incrementAndGet() == failLine) throw new FixedFormatException("bad data");
        TenCharRecord r = new TenCharRecord();
        r.setValue("ok");
        return (T) r;
      }
      @Override public <T> String export(T instance) { return ""; }
      @Override public <T> String export(String template, T instance) { return ""; }
    };
  }

  @Test
  void multiMatchFirstMatchUsesFirstRegisteredMappingOnly() {
    FixedFormatReader<Object> reader = FixedFormatReader.<Object>builder()
        .addMapping(TenCharRecord.class, ANY)
        .addMapping(FiveCharRecord.class, ANY)
        .multiMatchStrategy(MultiMatchStrategy.firstMatch())
        .build();

    List<Class<?>> classes = new ArrayList<>();
    reader.readWithCallback(new StringReader("AAAAAAAAAA"), (clazz, r) -> classes.add(clazz));

    assertEquals(1, classes.size());
    assertEquals(TenCharRecord.class, classes.get(0));
  }

  @Test
  void multiMatchThrowOnAmbiguityThrowsWhenTwoPatternsMatch() {
    FixedFormatReader<Object> reader = FixedFormatReader.<Object>builder()
        .addMapping(TenCharRecord.class, ANY)
        .addMapping(FiveCharRecord.class, ANY)
        .multiMatchStrategy(MultiMatchStrategy.throwOnAmbiguity())
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.readAsList(new StringReader("AAAAAAAAAA")));
  }

  @Test
  void multiMatchAllMatchesEmitsOneRecordPerMatchingMapping() {
    FixedFormatReader<Object> reader = FixedFormatReader.<Object>builder()
        .addMapping(TenCharRecord.class, ANY)
        .addMapping(FiveCharRecord.class, ANY)
        .multiMatchStrategy(MultiMatchStrategy.allMatches())
        .build();

    List<Class<?>> classes = new ArrayList<>();
    reader.readWithCallback(new StringReader("AAAAAAAAAA"), (clazz, r) -> classes.add(clazz));

    assertEquals(2, classes.size());
  }

  @Test
  void multiMatchCustomStrategyIsHonoured() {
    MultiMatchStrategy lastMatch = new MultiMatchStrategy() {
      @Override
      public <T> List<ClassPatternMapping<? extends T>> resolve(
          List<ClassPatternMapping<? extends T>> matched, long lineNumber) {
        return matched.isEmpty() ? matched : matched.subList(matched.size() - 1, matched.size());
      }
    };

    FixedFormatReader<Object> reader = FixedFormatReader.<Object>builder()
        .addMapping(TenCharRecord.class, ANY)
        .addMapping(FiveCharRecord.class, ANY)
        .multiMatchStrategy(lastMatch)
        .build();

    List<Class<?>> classes = new ArrayList<>();
    reader.readWithCallback(new StringReader("AAAAA"), (clazz, r) -> classes.add(clazz));

    assertEquals(1, classes.size());
    assertEquals(FiveCharRecord.class, classes.get(0));
  }

  @Test
  void unmatchedSkipDoesNotEmitRecordOrThrow() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^A"))
        .unmatchedLineStrategy(UnmatchedLineStrategy.skip())
        .build();

    List<TenCharRecord> results = reader.readAsList(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));
    assertEquals(1, results.size());
  }

  @Test
  void unmatchedThrowExceptionThrowsOnUnmatchedLine() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^A"))
        .unmatchedLineStrategy(UnmatchedLineStrategy.throwException())
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.readAsList(new StringReader("AAAAAAAAAA\nBBBBBBBBBB")));
  }

  @Test
  void unmatchedCustomLambdaInvokesWithLineNumberAndContent() {
    List<String> captured = new ArrayList<>();

    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^A"))
        .unmatchedLineStrategy((lineNumber, line) -> captured.add(lineNumber + ":" + line))
        .build();

    reader.readAsList(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    assertEquals(1, captured.size());
    assertEquals("2:BBBBBBBBBB", captured.get(0));
  }

  @Test
  void parseErrorSkipAndLogSkipsBadLineAndContinues() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, ANY)
        .parseErrorStrategy(ParseErrorStrategy.skipAndLog())
        .manager(failOnLine(2))
        .build();

    try (Stream<TenCharRecord> stream = reader.readAsStream(new StringReader(THREE_LINES))) {
      List<TenCharRecord> results = stream.collect(Collectors.toList());
      assertEquals(2, results.size(), "Bad line should be skipped; two good lines emitted");
    }
  }

  @Test
  void parseErrorCustomLambdaInvokesHandlerAndDoesNotEmitRecord() {
    List<String> captured = new ArrayList<>();

    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, ANY)
        .parseErrorStrategy((wrapped, line, lineNumber) -> captured.add(lineNumber + ":" + line))
        .manager(failOnLine(2))
        .build();

    try (Stream<TenCharRecord> stream = reader.readAsStream(new StringReader(THREE_LINES))) {
      List<TenCharRecord> results = stream.collect(Collectors.toList());
      assertEquals(2, results.size(), "Record for bad line must not be emitted");
    }
    assertEquals(1, captured.size());
    assertTrue(captured.get(0).startsWith("2:"));
  }

  @Test
  void parseErrorCustomLambdaCanRethrowToAbortProcessing() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, ANY)
        .parseErrorStrategy((wrapped, line, lineNumber) -> { throw wrapped; })
        .manager(failOnLine(2))
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.readAsList(new StringReader(THREE_LINES)));
  }
}
