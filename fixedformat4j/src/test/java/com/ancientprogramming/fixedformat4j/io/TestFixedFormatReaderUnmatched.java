package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.io.read.RegexLinePattern;
import com.ancientprogramming.fixedformat4j.io.read.UnmatchStrategy;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;

class TestFixedFormatReaderUnmatched {

  @Test
  void throwsOnUnmatchedLineWhenStrategyIsThrowException() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexLinePattern("^A"))
        .unmatchStrategy(UnmatchStrategy.throwException())
        .build();

    FixedFormatException ex = assertThrows(FixedFormatException.class, () ->
        reader.readAsResult(new StringReader("BBBBBBBBBB")));
    assertTrue(ex.getMessage().contains("1"), "Should contain line number: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("BBBBBBBBBB"), "Should contain raw line: " + ex.getMessage());
  }

  @Test
  void customLambdaStrategyReceivesLineNumberAndContent() {
    List<String> captured = new ArrayList<>();
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexLinePattern("^A"))
        .unmatchStrategy((lineNumber, segment) -> captured.add(lineNumber + ":" + segment))
        .build();

    reader.readAsResult(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));
    assertEquals(1, captured.size());
    assertEquals("2:BBBBBBBBBB", captured.get(0));
  }

  @Test
  void strategyNotInvokedForMatchedLines() {
    List<String> captured = new ArrayList<>();
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexLinePattern(".*"))
        .unmatchStrategy((lineNumber, segment) -> captured.add(segment))
        .build();

    reader.readAsResult(new StringReader("AAAAAAAAAA"));
    assertTrue(captured.isEmpty());
  }
}
