package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatIOException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatReaderStream {

  private static Reader readerOf(String... lines) {
    return new StringReader(String.join("\n", lines));
  }

  private FixedFormatReader<TenCharRecord> singleTypeReader() {
    return FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .build();
  }

  @Test
  void emitsOneRecordPerMatchingLine() {
    List<TenCharRecord> results;
    try (Stream<TenCharRecord> stream = singleTypeReader().readAsStream(readerOf("hello     ", "world     "))) {
      results = stream.collect(Collectors.toList());
    }
    assertEquals(2, results.size());
    assertEquals("hello", results.get(0).getValue());
    assertEquals("world", results.get(1).getValue());
  }

  @Test
  void skipsUnmatchedLinesByDefault() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern("^A"))
        .build();

    List<TenCharRecord> results;
    try (Stream<TenCharRecord> stream = reader.readAsStream(readerOf("AAAAAAAAAA", "BBBBBBBBBB", "AAAAAAAAAA"))) {
      results = stream.collect(Collectors.toList());
    }
    assertEquals(2, results.size());
  }

  @Test
  void throwsFixedFormatExceptionWithLineNumberOnParseError() {
    FixedFormatManager failingManager = new FixedFormatManager() {
      private int callCount = 0;
      @Override public <T> T load(Class<T> clazz, String data) {
        callCount++;
        if (callCount == 2) throw new FixedFormatException("bad data");
        return null;
      }
      @Override public <T> String export(T instance) { return ""; }
      @Override public <T> String export(String template, T instance) { return ""; }
    };

    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .manager(failingManager)
        .build();

    FixedFormatException ex = assertThrows(FixedFormatException.class, () -> {
      try (Stream<TenCharRecord> stream = reader.readAsStream(readerOf("line1     ", "line2     "))) {
        stream.collect(Collectors.toList());
      }
    });
    assertTrue(ex.getMessage().contains("2"), "Exception should mention line 2, was: " + ex.getMessage());
  }

  @Test
  void wrapsIOExceptionAsFixedFormatIOException() {
    Reader brokenReader = new Reader() {
      @Override public int read(char[] cbuf, int off, int len) throws IOException {
        throw new IOException("disk failure");
      }
      @Override public void close() {}
    };

    assertThrows(FixedFormatIOException.class, () -> {
      try (Stream<TenCharRecord> stream = singleTypeReader().readAsStream(brokenReader)) {
        stream.collect(Collectors.toList());
      }
    });
  }

  @Test
  void includeLinesPreventsMatchingOnExcludedLines() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .includeLines(line -> !line.startsWith("#"))
        .build();

    List<TenCharRecord> results;
    try (Stream<TenCharRecord> stream = reader.readAsStream(readerOf("hello     ", "# comment ", "world     "))) {
      results = stream.collect(Collectors.toList());
    }
    assertEquals(2, results.size());
    assertEquals("hello", results.get(0).getValue());
    assertEquals("world", results.get(1).getValue());
  }

  @Test
  void closingStreamClosesUnderlyingReader() {
    boolean[] closed = {false};
    Reader trackingReader = new StringReader("hello     ") {
      @Override public void close() {
        closed[0] = true;
        super.close();
      }
    };

    try (Stream<TenCharRecord> stream = singleTypeReader().readAsStream(trackingReader)) {
      stream.findFirst();
    }
    assertTrue(closed[0]);
  }
}
