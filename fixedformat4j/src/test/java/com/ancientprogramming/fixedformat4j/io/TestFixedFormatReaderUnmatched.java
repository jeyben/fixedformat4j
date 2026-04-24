package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.io.read.UnmatchStrategy;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;

class TestFixedFormatReaderUnmatched {

  @Test
  void throwsOnUnmatchedLineWhenStrategyIsThrowException() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, Pattern.compile("^A").asPredicate())
        .unmatchStrategy(UnmatchStrategy.throwException())
        .build();

    FixedFormatException ex = assertThrows(FixedFormatException.class, () ->
        reader.read(new StringReader("BBBBBBBBBB")));
    assertTrue(ex.getMessage().contains("1"), "Should contain line number: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("BBBBBBBBBB"), "Should contain raw line: " + ex.getMessage());
  }

  @Test
  void customLambdaStrategyReceivesLineNumberAndContent() {
    List<String> captured = new ArrayList<>();
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, Pattern.compile("^A").asPredicate())
        .unmatchStrategy((lineNumber, line) -> captured.add(lineNumber + ":" + line))
        .build();

    reader.read(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));
    assertEquals(1, captured.size());
    assertEquals("2:BBBBBBBBBB", captured.get(0));
  }

  @Test
  void strategyNotInvokedForMatchedLines() {
    List<String> captured = new ArrayList<>();
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, Pattern.compile(".*").asPredicate())
        .unmatchStrategy((lineNumber, line) -> captured.add(line))
        .build();

    reader.read(new StringReader("AAAAAAAAAA"));
    assertTrue(captured.isEmpty());
  }
}
