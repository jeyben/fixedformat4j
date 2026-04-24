package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.io.read.ParseErrorStrategy;
import com.ancientprogramming.fixedformat4j.io.read.UnmatchStrategy;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;
import com.ancientprogramming.fixedformat4j.io.read.MultiMatchStrategy;

class TestStrategiesAndHandlers {

  private static final Predicate<String> ANY = Pattern.compile(".*").asPredicate();
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
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, ANY)
        .addMapping(FiveCharRecord.class, ANY)
        .multiMatchStrategy(MultiMatchStrategy.firstMatch())
        .build();

    List<Object> records = reader.readAsResult(new StringReader("AAAAAAAAAA")).getAll();

    assertEquals(1, records.size());
    assertInstanceOf(TenCharRecord.class, records.get(0));
  }

  @Test
  void multiMatchThrowOnAmbiguityThrowsWhenTwoPatternsMatch() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, ANY)
        .addMapping(FiveCharRecord.class, ANY)
        .multiMatchStrategy(MultiMatchStrategy.throwOnAmbiguity())
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.readAsResult(new StringReader("AAAAAAAAAA")));
  }

  @Test
  void multiMatchAllMatchesEmitsOneRecordPerMatchingMapping() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, ANY)
        .addMapping(FiveCharRecord.class, ANY)
        .multiMatchStrategy(MultiMatchStrategy.allMatches())
        .build();

    List<Object> records = reader.readAsResult(new StringReader("AAAAAAAAAA")).getAll();

    assertEquals(2, records.size());
  }

  @Test
  void multiMatchCustomStrategyIsHonoured() {
    MultiMatchStrategy lastMatch =
        (matched, lineNumber) -> matched.isEmpty() ? matched : matched.subList(matched.size() - 1, matched.size());

    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, ANY)
        .addMapping(FiveCharRecord.class, ANY)
        .multiMatchStrategy(lastMatch)
        .build();

    List<Object> records = reader.readAsResult(new StringReader("AAAAA")).getAll();

    assertEquals(1, records.size());
    assertInstanceOf(FiveCharRecord.class, records.get(0));
  }

  @Test
  void unmatchedSkipDoesNotEmitRecordOrThrow() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, Pattern.compile("^A").asPredicate())
        .unmatchStrategy(UnmatchStrategy.skip())
        .build();

    List<Object> results = reader.readAsResult(new StringReader("AAAAAAAAAA\nBBBBBBBBBB")).getAll();
    assertEquals(1, results.size());
  }

  @Test
  void unmatchedThrowExceptionThrowsOnUnmatchedLine() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, Pattern.compile("^A").asPredicate())
        .unmatchStrategy(UnmatchStrategy.throwException())
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.readAsResult(new StringReader("AAAAAAAAAA\nBBBBBBBBBB")));
  }

  @Test
  void unmatchedCustomLambdaInvokesWithLineNumberAndContent() {
    List<String> captured = new ArrayList<>();

    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, Pattern.compile("^A").asPredicate())
        .unmatchStrategy((lineNumber, segment) -> captured.add(lineNumber + ":" + segment))
        .build();

    reader.readAsResult(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    assertEquals(1, captured.size());
    assertEquals("2:BBBBBBBBBB", captured.get(0));
  }

  @Test
  void parseErrorSkipAndLogSkipsBadLineAndContinues() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, ANY)
        .parseErrorStrategy(ParseErrorStrategy.skipAndLog())
        .manager(failOnLine(2))
        .build();

    List<Object> results = reader.readAsResult(new StringReader(THREE_LINES)).getAll();
    assertEquals(2, results.size(), "Bad line should be skipped; two good lines emitted");
  }

  @Test
  void parseErrorCustomLambdaInvokesHandlerAndDoesNotEmitRecord() {
    List<String> captured = new ArrayList<>();

    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, ANY)
        .parseErrorStrategy((wrapped, line, lineNumber) -> captured.add(lineNumber + ":" + line))
        .manager(failOnLine(2))
        .build();

    List<Object> results = reader.readAsResult(new StringReader(THREE_LINES)).getAll();
    assertEquals(2, results.size(), "Record for bad line must not be emitted");
    assertEquals(1, captured.size());
    assertTrue(captured.get(0).startsWith("2:"));
  }

  @Test
  void parseErrorCustomLambdaCanRethrowToAbortProcessing() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, ANY)
        .parseErrorStrategy((wrapped, line, lineNumber) -> { throw wrapped; })
        .manager(failOnLine(2))
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.readAsResult(new StringReader(THREE_LINES)));
  }
}
