package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TestPackedRecordReader {

  @TempDir
  Path tempDir;

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");

  // --- builder validation ---

  @Test
  void builderThrowsWhenNoMappings() {
    assertThrows(IllegalArgumentException.class,
        () -> PackedRecordReader.builder().recordWidth(10).build());
  }

  @Test
  void builderThrowsWhenRecordWidthNotSet() {
    assertThrows(IllegalArgumentException.class,
        () -> PackedRecordReader.builder()
            .addMapping(TenCharRecord.class, A_PATTERN)
            .build());
  }

  @Test
  void builderThrowsWhenRecordWidthIsZero() {
    assertThrows(IllegalArgumentException.class,
        () -> PackedRecordReader.builder()
            .addMapping(TenCharRecord.class, A_PATTERN)
            .recordWidth(0)
            .build());
  }

  @Test
  void builderThrowsWhenRecordWidthIsNegative() {
    assertThrows(IllegalArgumentException.class,
        () -> PackedRecordReader.builder()
            .addMapping(TenCharRecord.class, A_PATTERN)
            .recordWidth(-1)
            .build());
  }

  // --- single chunk per line (degenerate case) ---

  @Test
  void singleChunkPerLineActsLikeRegularReader() {
    List<Object> results = reader10A()
        .readAsList(new StringReader("AAAAAAAAAA\nAAAAAAAAAAAA"));

    assertEquals(2, results.size());
    assertEquals("AAAAAAAAAA", tenCharOf(results.get(0)));
    assertEquals("AAAAAAAAAA", tenCharOf(results.get(1)));
  }

  // --- two full chunks per line ---

  @Test
  void twoChunksPerLineFirstMatchSecondUnmatched() {
    List<Object> results = reader10A()
        .readAsList(new StringReader("AAAAAAAAAA1111111111"));

    // "1111111111" doesn't start with A → skipped by default unmatchStrategy
    assertEquals(1, results.size());
    assertEquals("AAAAAAAAAA", tenCharOf(results.get(0)));
  }

  @Test
  void twoMatchingChunksPerLineBothEmitted() {
    List<Object> results = reader10A()
        .readAsList(new StringReader("AAAAAA1234AAAAAA5678"));

    assertEquals(2, results.size());
    assertEquals("AAAAAA1234", tenCharOf(results.get(0)));
    assertEquals("AAAAAA5678", tenCharOf(results.get(1)));
  }

  @Test
  void threeFullChunksOnOneLineAllParsed() {
    List<Object> results = reader10A()
        .readAsList(new StringReader("AAAAAAAAAA" + "AAAAAAAAAA" + "AAAAAAAAAA"));

    assertEquals(3, results.size());
  }

  @Test
  void chunksFromMultipleLinesEmittedInOrder() {
    List<Object> results = reader10A()
        .readAsList(new StringReader("AAAAAAAAAA" + "AAAAAA1234" + "\n" + "AAAAAAAAAA"));

    assertEquals(3, results.size());
  }

  // --- partial trailing chunk ---

  @Test
  void partialChunkWithDefaultSkipIsDiscarded() {
    // "AAAAAAAAAA" (10) + "AAAAAAAAAA" (10) + "AAAAA" (5 — partial) = 25 chars
    List<Object> results = reader10A()
        .readAsList(new StringReader("AAAAAAAAAA" + "AAAAAAAAAA" + "AAAAA"));

    assertEquals(2, results.size());
  }

  @Test
  void partialChunkWithExplicitSkipIsDiscarded() {
    List<Object> results = PackedRecordReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .partialChunkStrategy(PartialChunkStrategy.skip())
        .build()
        .readAsList(new StringReader("AAAAAAAAAA" + "AAAAA"));

    assertEquals(1, results.size());
  }

  @Test
  void partialChunkWithPadStrategyIsPaddedAndParsed() {
    List<Object> results = PackedRecordReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .partialChunkStrategy(PartialChunkStrategy.pad())
        .build()
        .readAsList(new StringReader("AAAAAAAAAA" + "AAAAA"));

    // "AAAAA" padded with spaces to "AAAAA     " — starts with A, matched
    assertEquals(2, results.size());
    assertEquals("AAAAA", tenCharOf(results.get(1)));
  }

  @Test
  void partialChunkWithThrowExceptionStrategyThrows() {
    PackedRecordReader reader = PackedRecordReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .partialChunkStrategy(PartialChunkStrategy.throwException())
        .build();

    assertThrows(FixedFormatException.class,
        () -> reader.readAsList(new StringReader("AAAAAAAAAA" + "AAAAA")));
  }

  // --- unmatched full chunk ---

  @Test
  void unmatchedChunkWithDefaultSkipIsIgnored() {
    List<Object> results = reader10A()
        .readAsList(new StringReader("BBBBBBBBBB"));

    assertTrue(results.isEmpty());
  }

  @Test
  void unmatchedChunkFiresUnmatchStrategy() {
    List<String> captured = new ArrayList<>();

    PackedRecordReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .unmatchStrategy((lineNumber, segment) -> captured.add(lineNumber + ":" + segment))
        .build()
        .readAsList(new StringReader("AAAAAAAAAA" + "BBBBBBBBBB"));

    assertEquals(1, captured.size());
    assertEquals("1:BBBBBBBBBB", captured.get(0));
  }

  @Test
  void unmatchedChunkWithThrowExceptionStrategyThrows() {
    PackedRecordReader reader = PackedRecordReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .unmatchStrategy(UnmatchStrategy.throwException())
        .build();

    assertThrows(FixedFormatException.class,
        () -> reader.readAsList(new StringReader("BBBBBBBBBB")));
  }

  // --- readAsTypedResult ---

  @Test
  void readAsTypedResultProvidesTypedAccess() {
    TypedReadResult result = reader10A()
        .readAsTypedResult(new StringReader("AAAAAAAAAA"));

    List<TenCharRecord> records = result.get(TenCharRecord.class);
    assertEquals(1, records.size());
    assertEquals("AAAAAAAAAA", records.get(0).getValue());
  }

  @Test
  void readAsTypedResultAggregatesMultipleChunks() {
    TypedReadResult result = reader10A()
        .readAsTypedResult(new StringReader("AAAAAAAAAA" + "AAAAAA5678"));

    List<TenCharRecord> records = result.get(TenCharRecord.class);
    assertEquals(2, records.size());
  }

  // --- readAsStream ---

  @Test
  void readAsStreamEmitsAllMatchedChunks() {
    try (Stream<Object> stream = reader10A()
        .readAsStream(new StringReader("AAAAAAAAAA" + "AAAAAAAAAA"))) {
      List<Object> results = stream.collect(Collectors.toList());
      assertEquals(2, results.size());
    }
  }

  // --- input-source overloads ---

  @Test
  void worksWithInputStream() {
    byte[] bytes = ("AAAAAAAAAA" + "1111111111").getBytes(StandardCharsets.UTF_8);
    List<Object> results = reader10A().readAsList(new ByteArrayInputStream(bytes));

    assertEquals(1, results.size());
  }

  @Test
  void worksWithInputStreamAndCharset() {
    byte[] bytes = "AAAAAAAAAA".getBytes(StandardCharsets.ISO_8859_1);
    List<Object> results = reader10A().readAsList(
        new ByteArrayInputStream(bytes), StandardCharsets.ISO_8859_1);

    assertEquals(1, results.size());
  }

  @Test
  void worksWithFile() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA" + "AAAAAAAAAA");

    List<Object> results = reader10A().readAsList(file.toFile());
    assertEquals(2, results.size());
  }

  @Test
  void worksWithPath() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA");

    List<Object> results = reader10A().readAsList(file);
    assertEquals(1, results.size());
  }

  // --- includeLines predicate ---

  @Test
  void filteredLineDropsAllItsChunks() {
    List<Object> results = PackedRecordReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .includeLines(line -> !line.startsWith("#"))
        .build()
        .readAsList(new StringReader("#comment...\n" + "AAAAAAAAAA"));

    assertEquals(1, results.size());
    assertEquals("AAAAAAAAAA", tenCharOf(results.get(0)));
  }

  // --- helpers ---

  private PackedRecordReader reader10A() {
    return PackedRecordReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build();
  }

  private String tenCharOf(Object record) {
    return ((TenCharRecord) record).getValue();
  }
}
