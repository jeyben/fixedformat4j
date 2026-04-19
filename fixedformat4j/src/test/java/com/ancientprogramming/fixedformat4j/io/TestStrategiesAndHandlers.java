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
        .multiMatchStrategy(MultiMatchStrategy.FIRST_MATCH)
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
        .multiMatchStrategy(MultiMatchStrategy.THROW_ON_AMBIGUITY)
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.readAsList(new StringReader("AAAAAAAAAA")));
  }

  @Test
  void multiMatchAllMatchesEmitsOneRecordPerMatchingMapping() {
    FixedFormatReader<Object> reader = FixedFormatReader.<Object>builder()
        .addMapping(TenCharRecord.class, ANY)
        .addMapping(FiveCharRecord.class, ANY)
        .multiMatchStrategy(MultiMatchStrategy.ALL_MATCHES)
        .build();

    List<Class<?>> classes = new ArrayList<>();
    reader.readWithCallback(new StringReader("AAAAAAAAAA"), (clazz, r) -> classes.add(clazz));

    assertEquals(2, classes.size());
  }

  @Test
  void unmatchedSkipDoesNotEmitRecordOrThrow() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^A"))
        .unmatchedLineStrategy(UnmatchedLineStrategy.SKIP)
        .build();

    List<TenCharRecord> results = reader.readAsList(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));
    assertEquals(1, results.size());
  }

  @Test
  void unmatchedThrowThrowsOnUnmatchedLine() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^A"))
        .unmatchedLineStrategy(UnmatchedLineStrategy.THROW)
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.readAsList(new StringReader("AAAAAAAAAA\nBBBBBBBBBB")));
  }

  @Test
  void unmatchedForwardToHandlerInvokesHandlerWithLineNumberAndContent() {
    List<String> captured = new ArrayList<>();

    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^A"))
        .unmatchedLineStrategy(UnmatchedLineStrategy.FORWARD_TO_HANDLER)
        .unmatchedLineHandler((lineNumber, line) -> captured.add(lineNumber + ":" + line))
        .build();

    reader.readAsList(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    assertEquals(1, captured.size());
    assertEquals("2:BBBBBBBBBB", captured.get(0));
  }

  @Test
  void parseErrorSkipAndLogSkipsBadLineAndContinues() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, ANY)
        .parseErrorStrategy(ParseErrorStrategy.SKIP_AND_LOG)
        .manager(failOnLine(2))
        .build();

    try (Stream<TenCharRecord> stream = reader.readAsStream(new StringReader(THREE_LINES))) {
      List<TenCharRecord> results = stream.collect(Collectors.toList());
      assertEquals(2, results.size(), "Bad line should be skipped; two good lines emitted");
    }
  }

  @Test
  void parseErrorForwardToHandlerInvokesHandlerAndDoesNotEmitRecord() {
    List<String> captured = new ArrayList<>();

    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, ANY)
        .parseErrorStrategy(ParseErrorStrategy.FORWARD_TO_HANDLER)
        .parseErrorHandler((lineNumber, line, cause) -> captured.add(lineNumber + ":" + line))
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
  void unmatchedLineHandlerAcceptsLambda() {
    List<String> captured = new ArrayList<>();
    UnmatchedLineHandler handler = (lineNumber, line) -> captured.add(lineNumber + ":" + line);
    handler.handle(3L, "unmatched line");
    assertEquals("3:unmatched line", captured.get(0));
  }

  @Test
  void parseErrorHandlerAcceptsLambda() {
    List<String> captured = new ArrayList<>();
    ParseErrorHandler handler = (lineNumber, line, cause) ->
        captured.add(lineNumber + ":" + line + ":" + cause.getMessage());
    handler.handle(7L, "bad line", new FixedFormatException("parse failed"));
    assertEquals("7:bad line:parse failed", captured.get(0));
  }
}
