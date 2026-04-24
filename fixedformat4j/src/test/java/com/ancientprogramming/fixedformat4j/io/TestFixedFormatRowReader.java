package com.ancientprogramming.fixedformat4j.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatRowReader {

  @TempDir
  Path tempDir;

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");
  private static final FixedFormatMatchPattern B_PATTERN = new RegexFixedFormatMatchPattern("^B");

  // --- builder ---

  @Test
  void builderRequiresAtLeastOneMapping() {
    assertThrows(IllegalArgumentException.class,
        () -> FixedFormatRowReader.builder().build());
  }

  @Test
  void builderReturnsRowReader() {
    assertNotNull(FixedFormatRowReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build());
  }

  // --- readAsRows: empty input ---

  @Test
  void emptyInputProducesEmptyList() {
    List<Row> rows = readerA().readAsRows(new StringReader(""));

    assertTrue(rows.isEmpty());
  }

  // --- readAsRows: all matched ---

  @Test
  void allMatchedLinesProduceParsedRowsInOrder() {
    List<Row> rows = readerA().readAsRows(new StringReader("AAAAAAAAAA\nAAAAAAAAAAAA"));

    assertEquals(2, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(ParsedRow.class, rows.get(1));
    assertEquals("AAAAAAAAAA", tenCharOf(rows.get(0)));
    assertEquals("AAAAAAAAAA", tenCharOf(rows.get(1)));
  }

  // --- readAsRows: all unmatched ---

  @Test
  void allUnmatchedLinesProduceUnmatchedRowsInOrder() {
    List<Row> rows = readerA().readAsRows(new StringReader("COMMENT1\nCOMMENT2"));

    assertEquals(2, rows.size());
    assertInstanceOf(UnmatchedRow.class, rows.get(0));
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
    assertEquals("COMMENT1", ((UnmatchedRow) rows.get(0)).getRawLine());
    assertEquals("COMMENT2", ((UnmatchedRow) rows.get(1)).getRawLine());
  }

  // --- readAsRows: mixed ---

  @Test
  void mixedFilePreservesExactLineOrder() {
    List<Row> rows = readerAB().readAsRows(
        new StringReader("AAAAAAAAAA\nCOMMENT\nBBBBB     "));

    assertEquals(3, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
    assertInstanceOf(ParsedRow.class, rows.get(2));
    assertEquals("AAAAAAAAAA", tenCharOf(rows.get(0)));
    assertEquals("COMMENT", ((UnmatchedRow) rows.get(1)).getRawLine());
    assertEquals("BBBBB", fiveCharOf(rows.get(2)));
  }

  @Test
  void unmatchedLinesAlwaysCapturedRegardlessOfBuilderConfig() {
    // FixedFormatRowReader has no unmatchStrategy — all unmatched become UnmatchedRow
    FixedFormatRowReader reader = FixedFormatRowReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build();

    List<Row> rows = reader.readAsRows(new StringReader("AAAAAAAAAA\nCOMMENT"));

    assertEquals(2, rows.size());
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
    assertEquals("COMMENT", ((UnmatchedRow) rows.get(1)).getRawLine());
  }

  // --- readAsRows: input-source overloads ---

  @Test
  void worksWithInputStream() {
    byte[] bytes = "AAAAAAAAAA\nCOMMENT".getBytes(StandardCharsets.UTF_8);

    List<Row> rows = readerA().readAsRows(new ByteArrayInputStream(bytes));

    assertEquals(2, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
  }

  @Test
  void worksWithInputStreamAndCharset() {
    byte[] bytes = "AAAAAAAAAA\nCOMMENT".getBytes(StandardCharsets.ISO_8859_1);

    List<Row> rows = readerA().readAsRows(new ByteArrayInputStream(bytes), StandardCharsets.ISO_8859_1);

    assertEquals(2, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
  }

  @Test
  void worksWithFile() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nCOMMENT\nBBBBB     ");

    List<Row> rows = readerAB().readAsRows(file.toFile());

    assertEquals(3, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
    assertInstanceOf(ParsedRow.class, rows.get(2));
  }

  @Test
  void worksWithFileAndCharset() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.write(file, "AAAAAAAAAA\nCOMMENT".getBytes(StandardCharsets.ISO_8859_1));

    List<Row> rows = readerA().readAsRows(file.toFile(), StandardCharsets.ISO_8859_1);

    assertEquals(2, rows.size());
  }

  @Test
  void worksWithPath() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nCOMMENT");

    List<Row> rows = readerA().readAsRows(file);

    assertEquals(2, rows.size());
  }

  @Test
  void worksWithPathAndCharset() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.write(file, "AAAAAAAAAA\nCOMMENT".getBytes(StandardCharsets.ISO_8859_1));

    List<Row> rows = readerA().readAsRows(file, StandardCharsets.ISO_8859_1);

    assertEquals(2, rows.size());
  }

  // --- helpers ---

  private FixedFormatRowReader readerA() {
    return FixedFormatRowReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build();
  }

  private FixedFormatRowReader readerAB() {
    return FixedFormatRowReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();
  }

  @SuppressWarnings("unchecked")
  private String tenCharOf(Row row) {
    return ((ParsedRow<TenCharRecord>) row).getRecord().getValue();
  }

  @SuppressWarnings("unchecked")
  private String fiveCharOf(Row row) {
    return ((ParsedRow<FiveCharRecord>) row).getRecord().getCode();
  }
}
