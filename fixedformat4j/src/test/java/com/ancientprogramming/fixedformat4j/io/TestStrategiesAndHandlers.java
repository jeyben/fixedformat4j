package com.ancientprogramming.fixedformat4j.io;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.read.MultiMatchStrategy;
import com.ancientprogramming.fixedformat4j.io.read.ParseErrorStrategy;
import com.ancientprogramming.fixedformat4j.io.read.UnmatchStrategy;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TestStrategiesAndHandlers {

  private static final LinePattern ANY = LinePattern.matchAll();
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

    List<Object> records = reader.read(new StringReader("AAAAAAAAAA")).getAll();

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
        reader.read(new StringReader("AAAAAAAAAA")));
  }

  @Test
  void multiMatchAllMatchesEmitsOneRecordPerMatchingMapping() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, ANY)
        .addMapping(FiveCharRecord.class, ANY)
        .multiMatchStrategy(MultiMatchStrategy.allMatches())
        .build();

    List<Object> records = reader.read(new StringReader("AAAAAAAAAA")).getAll();

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

    List<Object> records = reader.read(new StringReader("AAAAA")).getAll();

    assertEquals(1, records.size());
    assertInstanceOf(FiveCharRecord.class, records.get(0));
  }

  @Test
  void unmatchedSkipDoesNotEmitRecordOrThrow() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .unmatchStrategy(UnmatchStrategy.skip())
        .build();

    List<Object> results = reader.read(new StringReader("AAAAAAAAAA\nBBBBBBBBBB")).getAll();
    assertEquals(1, results.size());
  }

  @Test
  void unmatchedThrowExceptionThrowsOnUnmatchedLine() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .unmatchStrategy(UnmatchStrategy.throwException())
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.read(new StringReader("AAAAAAAAAA\nBBBBBBBBBB")));
  }

  @Test
  void unmatchedCustomLambdaInvokesWithLineNumberAndContent() {
    List<String> captured = new ArrayList<>();

    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .unmatchStrategy((lineNumber, line) -> captured.add(lineNumber + ":" + line))
        .build();

    reader.read(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    assertEquals(1, captured.size());
    assertEquals("2:BBBBBBBBBB", captured.get(0));
  }

  @Test
  void unmatchedSkipLogsWarnWithLineNumberAndContent() {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(UnmatchStrategy.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      FixedFormatReader reader = FixedFormatReader.builder()
          .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
          .unmatchStrategy(UnmatchStrategy.skip())
          .build();

      assertDoesNotThrow(() ->
          reader.read(new StringReader("AAAAAAAAAA\nBBBBBBBBBB")));

      assertEquals(1, appender.list.size(), "Expected exactly one WARN log entry");
      ILoggingEvent event = appender.list.get(0);
      assertEquals(Level.WARN, event.getLevel());
      assertTrue(event.getFormattedMessage().contains("2"), "Message must include line number");
      assertTrue(event.getFormattedMessage().contains("BBBBBBBBBB"), "Message must include line content");
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void defaultUnmatchStrategyThrowsOnUnmatchedLine() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.read(new StringReader("AAAAAAAAAA\nBBBBBBBBBB")));
  }

  @Test
  void parseErrorSkipAndLogSkipsBadLineAndContinues() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, ANY)
        .parseErrorStrategy(ParseErrorStrategy.skipAndLog())
        .manager(failOnLine(2))
        .build();

    List<Object> results = reader.read(new StringReader(THREE_LINES)).getAll();
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

    List<Object> results = reader.read(new StringReader(THREE_LINES)).getAll();
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
        reader.read(new StringReader(THREE_LINES)));
  }

  @Test
  void excludeLinesSilentlySkipsMatchedLinesBeforePatternMatching() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .excludeLines(line -> line.isBlank())
        .build();

    List<Object> results = reader.read(new StringReader("AAAAAAAAAA\n\nAAAAAAAAAA")).getAll();

    assertEquals(2, results.size());
  }

  @Test
  void withoutExcludeLinesBlankLineTriggerUnmatchStrategy() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .build();

    assertThrows(FixedFormatException.class, () ->
        reader.read(new StringReader("AAAAAAAAAA\n\nAAAAAAAAAA")));
  }

  @Test
  void excludeLinesLogsDebugWithLineNumberAndContent() {
    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
        LoggerFactory.getLogger("com.ancientprogramming.fixedformat4j.io.read.FixedFormatLineProcessor");
    logger.setLevel(Level.DEBUG);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      FixedFormatReader reader = FixedFormatReader.builder()
          .addMapping(TenCharRecord.class, ANY)
          .excludeLines(line -> line.isBlank())
          .build();

      reader.read(new StringReader("AAAAAAAAAA\n\nAAAAAAAAAA"));

      assertEquals(1, appender.list.size(), "Expected exactly one DEBUG log entry");
      ILoggingEvent event = appender.list.get(0);
      assertEquals(Level.DEBUG, event.getLevel());
      assertTrue(event.getFormattedMessage().contains("2"), "Message must include line number");
    } finally {
      logger.detachAppender(appender);
    }
  }

  // --- ParseErrorStrategy unit tests ---

  @Test
  void throwExceptionRethrowsSameInstance() {
    ParseErrorStrategy strategy = ParseErrorStrategy.throwException();
    FixedFormatException original = new FixedFormatException("original");

    FixedFormatException thrown = assertThrows(FixedFormatException.class, () ->
        strategy.handle(original, "bad line", 1));

    assertSame(original, thrown, "throwException must rethrow the identical exception instance");
  }

  @Test
  void skipAndLogWithCauseLogsCauseMessage() {
    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
        LoggerFactory.getLogger(ParseErrorStrategy.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      RuntimeException cause = new RuntimeException("root cause detail");
      FixedFormatException wrapped = new FixedFormatException("wrapper message", cause);

      assertDoesNotThrow(() ->
          ParseErrorStrategy.skipAndLog().handle(wrapped, "bad line", 5));

      assertEquals(1, appender.list.size());
      String msg = appender.list.get(0).getFormattedMessage();
      assertTrue(msg.contains("root cause detail"), "Log must contain cause message");
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void skipAndLogWithoutCauseLogsWrappedMessage() {
    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
        LoggerFactory.getLogger(ParseErrorStrategy.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      FixedFormatException wrapped = new FixedFormatException("the wrapped message");

      assertDoesNotThrow(() ->
          ParseErrorStrategy.skipAndLog().handle(wrapped, "bad line", 3));

      assertEquals(1, appender.list.size());
      String msg = appender.list.get(0).getFormattedMessage();
      assertTrue(msg.contains("the wrapped message"), "Log must contain wrapped exception message");
    } finally {
      logger.detachAppender(appender);
    }
  }
}
