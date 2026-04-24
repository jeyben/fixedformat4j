package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatMatchPattern;
import com.ancientprogramming.fixedformat4j.io.read.RegexFixedFormatMatchPattern;
import com.ancientprogramming.fixedformat4j.io.row.ParsedRow;
import com.ancientprogramming.fixedformat4j.io.row.Row;
import com.ancientprogramming.fixedformat4j.io.row.UnmatchedRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatRowReader;
import com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter;

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

  // --- FixedFormatWriter ---

  @Test
  void writerProducesEmptyOutputForEmptyRowList() {
    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(List.of(), out);

    assertEquals("", out.toString());
  }

  @Test
  void writerExportsMatchedRowsViaManager() {
    List<Row> rows = List.of(new ParsedRow<>(TenCharRecord.class, tenChar("AAAAAAAAAA")));

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
    FixedFormatRowReader reader = readerAB();
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
    FixedFormatRowReader reader = readerAB();
    List<Row> rows = reader.readAsRows(
        new StringReader("AAAAAAAAAA\nCOMMENT\nBBBBB     "));

    rows.stream()
        .filter(r -> r instanceof ParsedRow && ((ParsedRow<?>) r).isOf(TenCharRecord.class))
        .map(r -> (ParsedRow<TenCharRecord>) r)
        .findFirst()
        .ifPresent(pr -> pr.getRecord().setValue("AAAAZZZZZZ"));

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

  private FixedFormatRowReader readerAB() {
    return FixedFormatRowReader.builder()
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
