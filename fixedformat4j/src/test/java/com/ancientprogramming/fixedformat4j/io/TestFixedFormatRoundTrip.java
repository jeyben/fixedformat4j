package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatRoundTrip {

  @TempDir
  Path tempDir;

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");
  private static final FixedFormatMatchPattern B_PATTERN = new RegexFixedFormatMatchPattern("^B");

  // --- ParsedRow ---

  @Test
  void parsedRowExposesTypeAndRecord() {
    TenCharRecord record = tenChar("AAAAAAAAAA");
    ParsedRow<TenCharRecord> row = new ParsedRow<>(TenCharRecord.class, record);

    assertSame(TenCharRecord.class, row.getType());
    assertSame(record, row.getRecord());
  }

  @Test
  void parsedRowIsOfMatchesExactType() {
    ParsedRow<TenCharRecord> row = new ParsedRow<>(TenCharRecord.class, tenChar("AAAAAAAAAA"));

    assertTrue(row.isOf(TenCharRecord.class));
    assertFalse(row.isOf(FiveCharRecord.class));
  }

  @Test
  void parsedRowRecordCanBeReplaced() {
    TenCharRecord original = tenChar("AAAAAAAAAA");
    TenCharRecord replacement = tenChar("ZZZZZZZZZZ");
    ParsedRow<TenCharRecord> row = new ParsedRow<>(TenCharRecord.class, original);

    row.setRecord(replacement);

    assertSame(replacement, row.getRecord());
  }

  @Test
  void parsedRowSetRecordRejectsNull() {
    ParsedRow<TenCharRecord> row = new ParsedRow<>(TenCharRecord.class, tenChar("AAAAAAAAAA"));

    assertThrows(NullPointerException.class, () -> row.setRecord(null));
  }

  // --- UnmatchedRow ---

  @Test
  void unmatchedRowHoldsRawLineVerbatim() {
    UnmatchedRow row = new UnmatchedRow("  COMMENT  ");

    assertEquals("  COMMENT  ", row.getRawLine());
  }

  // --- readAsRows: ordering and types ---

  @Test
  void readAsRowsReturnsEmptyListForEmptyInput() {
    FixedFormatReader reader = readerAB();

    List<Row> rows = reader.readAsRows(new StringReader(""));

    assertTrue(rows.isEmpty());
  }

  @Test
  void readAsRowsProducesParsedRowsForAllMatchedLines() {
    FixedFormatReader reader = readerA();

    List<Row> rows = reader.readAsRows(new StringReader("AAAAAAAAAA\nAAAAAAAAAAAA"));

    assertEquals(2, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(ParsedRow.class, rows.get(1));
    assertEquals("AAAAAAAAAA", tenCharOf(rows.get(0)));
    assertEquals("AAAAAAAAAA", tenCharOf(rows.get(1)));
  }

  @Test
  void readAsRowsProducesUnmatchedRowsForAllUnmatchedLines() {
    FixedFormatReader reader = readerA();

    List<Row> rows = reader.readAsRows(new StringReader("COMMENT1\nCOMMENT2"));

    assertEquals(2, rows.size());
    assertInstanceOf(UnmatchedRow.class, rows.get(0));
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
    assertEquals("COMMENT1", ((UnmatchedRow) rows.get(0)).getRawLine());
    assertEquals("COMMENT2", ((UnmatchedRow) rows.get(1)).getRawLine());
  }

  @Test
  void readAsRowsPreservesMixedOrderExactly() {
    FixedFormatReader reader = readerAB();

    List<Row> rows = reader.readAsRows(
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
  void readAsRowsIgnoresConfiguredUnmatchedStrategyAndCaptures() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .unmatchedLineStrategy(UnmatchedLineStrategy.throwException())
        .build();

    // throwException strategy does NOT fire — unmatched line becomes UnmatchedRow
    List<Row> rows = reader.readAsRows(new StringReader("AAAAAAAAAA\nCOMMENT"));

    assertEquals(2, rows.size());
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
    assertEquals("COMMENT", ((UnmatchedRow) rows.get(1)).getRawLine());
  }

  // --- readAsRows: input-source overloads ---

  @Test
  void readAsRowsWorksWithInputStream() {
    FixedFormatReader reader = readerA();
    byte[] bytes = "AAAAAAAAAA\nCOMMENT".getBytes(StandardCharsets.UTF_8);

    List<Row> rows = reader.readAsRows(new ByteArrayInputStream(bytes));

    assertEquals(2, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
  }

  @Test
  void readAsRowsWorksWithInputStreamAndCharset() {
    FixedFormatReader reader = readerA();
    byte[] bytes = "AAAAAAAAAA\nCOMMENT".getBytes(StandardCharsets.ISO_8859_1);

    List<Row> rows = reader.readAsRows(new ByteArrayInputStream(bytes), StandardCharsets.ISO_8859_1);

    assertEquals(2, rows.size());
  }

  @Test
  void readAsRowsWorksWithFile() throws IOException {
    FixedFormatReader reader = readerAB();
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nCOMMENT\nBBBBB     ");

    List<Row> rows = reader.readAsRows(file.toFile());

    assertEquals(3, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
    assertInstanceOf(ParsedRow.class, rows.get(2));
  }

  @Test
  void readAsRowsWorksWithPath() throws IOException {
    FixedFormatReader reader = readerA();
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nCOMMENT");

    List<Row> rows = reader.readAsRows(file);

    assertEquals(2, rows.size());
  }

  // --- FixedFormatWriter ---

  @Test
  void writerProducesEmptyOutputForEmptyRowList() {
    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(List.of(), out);

    assertEquals("", out.toString());
  }

  @Test
  void writerExportsMatchedRowsViaManager() {
    TenCharRecord record = tenChar("AAAAAAAAAA");
    List<Row> rows = List.of(new ParsedRow<>(TenCharRecord.class, record));

    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(rows, out);

    assertEquals("AAAAAAAAAA\n", out.toString());
  }

  @Test
  void writerEmitsUnmatchedRowsVerbatim() {
    List<Row> rows = List.of(new UnmatchedRow("  COMMENT  "));

    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(rows, out);

    assertEquals("  COMMENT  \n", out.toString());
  }

  @Test
  void writerPreservesMixedOrderExactly() {
    FixedFormatReader reader = readerAB();
    List<Row> rows = reader.readAsRows(
        new StringReader("AAAAAAAAAA\nCOMMENT\nBBBBB     "));

    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(rows, out);

    assertEquals("AAAAAAAAAA\nCOMMENT\nBBBBB\n", out.toString());
  }

  @Test
  void writerWorksWithFile() throws IOException {
    Path file = tempDir.resolve("out.txt");
    List<Row> rows = List.of(
        new ParsedRow<>(TenCharRecord.class, tenChar("AAAAAAAAAA")),
        new UnmatchedRow("COMMENT"));

    new FixedFormatWriter(new FixedFormatManagerImpl()).write(rows, file.toFile());

    assertEquals("AAAAAAAAAA\nCOMMENT\n", Files.readString(file));
  }

  @Test
  void writerWorksWithPath() throws IOException {
    Path file = tempDir.resolve("out.txt");
    List<Row> rows = List.of(new UnmatchedRow("RAW"));

    new FixedFormatWriter(new FixedFormatManagerImpl()).write(rows, file);

    assertEquals("RAW\n", Files.readString(file));
  }

  // --- Full round-trip ---

  @Test
  void roundTripEditsRecordAndPreservesOtherLinesInOrder() {
    FixedFormatReader reader = readerAB();
    List<Row> rows = reader.readAsRows(
        new StringReader("AAAAAAAAAA\nCOMMENT\nBBBBB     "));

    rows.stream()
        .filter(r -> r instanceof ParsedRow && ((ParsedRow<?>) r).isOf(TenCharRecord.class))
        .map(r -> (ParsedRow<TenCharRecord>) r)
        .findFirst()
        .ifPresent(pr -> pr.getRecord().setValue("AAAAZZZZZZ"));  // still starts with A

    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(rows, out);
    List<Row> reread = reader.readAsRows(new StringReader(out.toString()));

    assertEquals(3, reread.size());
    assertInstanceOf(ParsedRow.class, reread.get(0));
    assertInstanceOf(UnmatchedRow.class, reread.get(1));
    assertInstanceOf(ParsedRow.class, reread.get(2));
    assertEquals("AAAAZZZZZZ", tenCharOf(reread.get(0)));
    assertEquals("COMMENT", ((UnmatchedRow) reread.get(1)).getRawLine());
    assertEquals("BBBBB", fiveCharOf(reread.get(2)));
  }

  // --- helpers ---

  private FixedFormatReader readerA() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build();
  }

  private FixedFormatReader readerAB() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();
  }

  private TenCharRecord tenChar(String value) {
    TenCharRecord r = new TenCharRecord();
    r.setValue(value);
    return r;
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
