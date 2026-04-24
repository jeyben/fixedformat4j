package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
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

class TestPackedRecordRowReader {

  @TempDir
  Path tempDir;

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");

  // --- builder validation ---

  @Test
  void builderThrowsWhenNoMappings() {
    assertThrows(IllegalArgumentException.class,
        () -> PackedRecordRowReader.builder().recordWidth(10).build());
  }

  @Test
  void builderThrowsWhenRecordWidthNotSet() {
    assertThrows(IllegalArgumentException.class,
        () -> PackedRecordRowReader.builder()
            .addMapping(TenCharRecord.class, A_PATTERN)
            .build());
  }

  @Test
  void builderThrowsWhenRecordWidthIsZero() {
    assertThrows(IllegalArgumentException.class,
        () -> PackedRecordRowReader.builder()
            .addMapping(TenCharRecord.class, A_PATTERN)
            .recordWidth(0)
            .build());
  }

  // --- single chunk per line → ParsedRow ---

  @Test
  void singleMatchingChunkBecomesASingleParsedRow() {
    List<Row> rows = reader10A().readAsRows(new StringReader("AAAAAAAAAA"));

    assertEquals(1, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertEquals("AAAAAAAAAA", tenCharOf(rows.get(0)));
  }

  // --- two full chunks per line ---

  @Test
  void twoMatchingChunksPerLineProduceTwoParsedRowsInOrder() {
    List<Row> rows = reader10A()
        .readAsRows(new StringReader("AAAAAA1234AAAAAA5678"));

    assertEquals(2, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(ParsedRow.class, rows.get(1));
    assertEquals("AAAAAA1234", tenCharOf(rows.get(0)));
    assertEquals("AAAAAA5678", tenCharOf(rows.get(1)));
  }

  // --- unmatched full chunk → UnmatchedRow ---

  @Test
  void unmatchedChunkBecomesUnmatchedRowWithRawChunkText() {
    List<Row> rows = reader10A()
        .readAsRows(new StringReader("AAAAAAAAAA" + "BBBBBBBBBB"));

    assertEquals(2, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
    assertEquals("BBBBBBBBBB", ((UnmatchedRow) rows.get(1)).getRawLine());
  }

  // --- partial trailing chunk ---

  @Test
  void partialChunkWithSkipStrategySilentlyDropped() {
    List<Row> rows = PackedRecordRowReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .partialChunkStrategy(PartialChunkStrategy.skip())
        .build()
        .readAsRows(new StringReader("AAAAAAAAAA" + "AAAAA"));

    assertEquals(1, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
  }

  @Test
  void partialChunkWithPadStrategyBecomesAParsedRow() {
    List<Row> rows = PackedRecordRowReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .partialChunkStrategy(PartialChunkStrategy.pad())
        .build()
        .readAsRows(new StringReader("AAAAAAAAAA" + "AAAAA"));

    assertEquals(2, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(1));
    assertEquals("AAAAA", tenCharOf(rows.get(1)));
  }

  // --- filtered physical line → single UnmatchedRow ---

  @Test
  void filteredPhysicalLineBecomesASingleUnmatchedRow() {
    List<Row> rows = PackedRecordRowReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .includeLines(line -> !line.startsWith("#"))
        .build()
        .readAsRows(new StringReader("#comment...\n" + "AAAAAAAAAA"));

    assertEquals(2, rows.size());
    assertInstanceOf(UnmatchedRow.class, rows.get(0));
    assertEquals("#comment...", ((UnmatchedRow) rows.get(0)).getRawLine());
    assertInstanceOf(ParsedRow.class, rows.get(1));
  }

  // --- mixed: parsed + unmatched + filtered → all in correct order ---

  @Test
  void mixedInputPreservesExactOrder() {
    List<Row> rows = PackedRecordRowReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .includeLines(line -> !line.startsWith("#"))
        .build()
        .readAsRows(new StringReader(
            "AAAAAA1234" + "BBBBBBBBBB" + "\n"  // chunk0=parsed, chunk1=unmatched
            + "#filtered\n"                       // whole line → UnmatchedRow
            + "AAAAAA5678"));                     // chunk2=parsed

    assertEquals(4, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
    assertInstanceOf(UnmatchedRow.class, rows.get(2));
    assertInstanceOf(ParsedRow.class, rows.get(3));
    assertEquals("AAAAAA1234", tenCharOf(rows.get(0)));
    assertEquals("BBBBBBBBBB", ((UnmatchedRow) rows.get(1)).getRawLine());
    assertEquals("#filtered", ((UnmatchedRow) rows.get(2)).getRawLine());
    assertEquals("AAAAAA5678", tenCharOf(rows.get(3)));
  }

  // --- round-trip: edit a record and write back via FixedFormatWriter ---

  @Test
  void roundTripEditsRecordAndWritesBackUnpacked() {
    List<Row> rows = reader10A()
        .readAsRows(new StringReader("AAAAAAAAAA" + "AAAAAA5678"));

    rows.stream()
        .filter(r -> r instanceof ParsedRow && ((ParsedRow<?>) r).isOf(TenCharRecord.class))
        .map(r -> (ParsedRow<TenCharRecord>) r)
        .filter(pr -> pr.getRecord().getValue().endsWith("5678"))
        .findFirst()
        .ifPresent(pr -> pr.getRecord().setValue("AAAAAA9999"));

    StringWriter out = new StringWriter();
    // FixedFormatWriter writes one row per line — packed structure is unpacked
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(rows, out);

    List<Row> reread = reader10A()
        .readAsRows(new StringReader(out.toString()));
    assertEquals(2, reread.size());
    assertEquals("AAAAAAAAAA", tenCharOf(reread.get(0)));
    assertEquals("AAAAAA9999", tenCharOf(reread.get(1)));
  }

  // --- input-source overloads ---

  @Test
  void worksWithInputStream() {
    byte[] bytes = ("AAAAAAAAAA" + "BBBBBBBBBB").getBytes(StandardCharsets.UTF_8);
    List<Row> rows = reader10A().readAsRows(new ByteArrayInputStream(bytes));

    assertEquals(2, rows.size());
    assertInstanceOf(ParsedRow.class, rows.get(0));
    assertInstanceOf(UnmatchedRow.class, rows.get(1));
  }

  @Test
  void worksWithFile() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA" + "AAAAAAAAAA");

    List<Row> rows = reader10A().readAsRows(file.toFile());
    assertEquals(2, rows.size());
  }

  @Test
  void worksWithPath() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA");

    List<Row> rows = reader10A().readAsRows(file);
    assertEquals(1, rows.size());
  }

  // --- helpers ---

  private PackedRecordRowReader reader10A() {
    return PackedRecordRowReader.builder()
        .recordWidth(10)
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build();
  }

  @SuppressWarnings("unchecked")
  private String tenCharOf(Row row) {
    return ((ParsedRow<TenCharRecord>) row).getRecord().getValue();
  }
}
