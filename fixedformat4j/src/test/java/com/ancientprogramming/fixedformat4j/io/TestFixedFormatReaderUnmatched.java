package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.io.read.RegexLinePattern;
import com.ancientprogramming.fixedformat4j.io.read.UnmatchStrategy;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;

class TestFixedFormatReaderUnmatched {

  @Test
  void throwsOnUnmatchedLineWhenStrategyIsThrowException() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexLinePattern("^A"))
        .unmatchStrategy(UnmatchStrategy.throwException())
        .build();

    FixedFormatException ex = assertThrows(FixedFormatException.class, () -> {
      try (Stream<Object> stream = reader.readAsStream(new StringReader("BBBBBBBBBB"))) {
        stream.collect(Collectors.toList());
      }
    });
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

    try (Stream<Object> stream = reader.readAsStream(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"))) {
      stream.collect(Collectors.toList());
    }
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

    try (Stream<Object> stream = reader.readAsStream(new StringReader("AAAAAAAAAA"))) {
      stream.collect(Collectors.toList());
    }
    assertTrue(captured.isEmpty());
  }
}
