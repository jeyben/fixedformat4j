package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.read.ParseErrorStrategy;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;

class TestFixedFormatReaderParseError {

  @SuppressWarnings("unchecked")
  private static FixedFormatManager failOnSecondCall() {
    AtomicInteger calls = new AtomicInteger(0);
    return new FixedFormatManager() {
      @Override public <T> T load(Class<T> clazz, String data) {
        if (calls.incrementAndGet() == 2) throw new FixedFormatException("bad data");
        TenCharRecord r = new TenCharRecord();
        r.setValue("ok");
        return (T) r;
      }
      @Override public <T> String export(T instance) { return ""; }
      @Override public <T> String export(String template, T instance) { return ""; }
    };
  }

  @Test
  void skipAndLogSkipsBadLineAndContinuesParsing() {
    AtomicInteger goodLines = new AtomicInteger(0);
    FixedFormatManager countingManager = new FixedFormatManager() {
      private final AtomicInteger calls = new AtomicInteger(0);
      @Override public <T> T load(Class<T> clazz, String data) {
        int call = calls.incrementAndGet();
        if (call == 2) throw new FixedFormatException("bad data");
        goodLines.incrementAndGet();
        return null;
      }
      @Override public <T> String export(T instance) { return ""; }
      @Override public <T> String export(String template, T instance) { return ""; }
    };

    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .parseErrorStrategy(ParseErrorStrategy.skipAndLog())
        .manager(countingManager)
        .build();

    reader.read(new StringReader("line1     \nline2     \nline3     "));
    assertEquals(2, goodLines.get(), "Two good lines should have been processed");
  }

  @Test
  void customLambdaStrategyReceivesLineNumberLineAndCause() {
    List<String> captured = new ArrayList<>();

    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .parseErrorStrategy((wrapped, line, lineNumber) ->
            captured.add(lineNumber + ":" + line + ":" + wrapped.getMessage()))
        .manager(failOnSecondCall())
        .build();

    reader.read(new StringReader("line1     \nline2     "));
    assertEquals(1, captured.size());
    assertTrue(captured.get(0).startsWith("2:line2     "));
  }

  @Test
  void customLambdaStrategyDoesNotEmitRecordForFailedLine() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .parseErrorStrategy((wrapped, line, lineNumber) -> {})
        .manager(failOnSecondCall())
        .build();

    List<Object> results = reader.read(new StringReader("line1     \nline2     ")).getAll();
    assertEquals(1, results.size());
  }

  @Test
  void throwExceptionStrategy_rethrowsOnBadLine() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .parseErrorStrategy(ParseErrorStrategy.throwException())
        .manager(failOnSecondCall())
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.read(new StringReader("line1     \nline2     ")));
  }

  @Test
  void throwExceptionStrategy_doesNotContinuePastBadLine() {
    AtomicInteger processedCount = new AtomicInteger(0);
    FixedFormatManager countingManager = new FixedFormatManager() {
      private final AtomicInteger calls = new AtomicInteger(0);
      @Override public <T> T load(Class<T> clazz, String data) {
        int call = calls.incrementAndGet();
        if (call == 1) throw new FixedFormatException("bad first line");
        processedCount.incrementAndGet();
        return null;
      }
      @Override public <T> String export(T instance) { return ""; }
      @Override public <T> String export(String template, T instance) { return ""; }
    };

    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .parseErrorStrategy(ParseErrorStrategy.throwException())
        .manager(countingManager)
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.read(new StringReader("line1     \nline2     \nline3     ")));
    assertEquals(0, processedCount.get(), "no lines should be processed after throwException fires");
  }

  @Test
  void skipAndLogStrategy_readResultContainsOnlySuccessfulRecords() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .parseErrorStrategy(ParseErrorStrategy.skipAndLog())
        .manager(failOnSecondCall())
        .build();

    List<Object> results = reader.read(new StringReader("line1     \nline2     \nline3     ")).getAll();
    assertEquals(2, results.size());
  }

  @Test
  void throwExceptionStrategy_wrapsExceptionFromManager() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .parseErrorStrategy(ParseErrorStrategy.throwException())
        .manager(failOnSecondCall())
        .build();

    FixedFormatException ex = assertThrows(FixedFormatException.class, () ->
        reader.read(new StringReader("line1     \nline2     ")));
    assertNotNull(ex, "exception should not be null");
  }
}
