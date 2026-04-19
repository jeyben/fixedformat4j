package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestStrategiesAndHandlers {

  @Test
  void multiMatchStrategyHasAllValues() {
    assertNotNull(MultiMatchStrategy.FIRST_MATCH);
    assertNotNull(MultiMatchStrategy.THROW_ON_AMBIGUITY);
    assertNotNull(MultiMatchStrategy.ALL_MATCHES);
  }

  @Test
  void unmatchedLineStrategyHasAllValues() {
    assertNotNull(UnmatchedLineStrategy.SKIP);
    assertNotNull(UnmatchedLineStrategy.THROW);
    assertNotNull(UnmatchedLineStrategy.FORWARD_TO_HANDLER);
  }

  @Test
  void parseErrorStrategyHasAllValues() {
    assertNotNull(ParseErrorStrategy.THROW);
    assertNotNull(ParseErrorStrategy.SKIP_AND_LOG);
    assertNotNull(ParseErrorStrategy.FORWARD_TO_HANDLER);
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
