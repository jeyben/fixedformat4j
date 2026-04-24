package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatMatchPattern;
import com.ancientprogramming.fixedformat4j.io.read.RegexFixedFormatMatchPattern;
import com.ancientprogramming.fixedformat4j.io.segment.ParsedSegment;
import com.ancientprogramming.fixedformat4j.io.segment.Segment;
import com.ancientprogramming.fixedformat4j.io.segment.UnmatchedSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatSegmentReader;
import com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter;

class TestFixedFormatRoundTrip {

  @TempDir
  Path tempDir;

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");
  private static final FixedFormatMatchPattern B_PATTERN = new RegexFixedFormatMatchPattern("^B");

  // --- ParsedSegment ---

  @Test
  void parsedSegmentExposesTypeAndRecord() {
    TenCharRecord record = tenChar("AAAAAAAAAA");
    ParsedSegment<TenCharRecord> segment = new ParsedSegment<>(TenCharRecord.class, record);

    assertSame(TenCharRecord.class, segment.getType());
    assertSame(record, segment.getRecord());
  }

  @Test
  void parsedSegmentIsOfMatchesExactType() {
    ParsedSegment<TenCharRecord> segment = new ParsedSegment<>(TenCharRecord.class, tenChar("AAAAAAAAAA"));

    assertTrue(segment.isOf(TenCharRecord.class));
    assertFalse(segment.isOf(FiveCharRecord.class));
  }

  @Test
  void parsedSegmentRecordCanBeReplaced() {
    TenCharRecord original = tenChar("AAAAAAAAAA");
    TenCharRecord replacement = tenChar("ZZZZZZZZZZ");
    ParsedSegment<TenCharRecord> segment = new ParsedSegment<>(TenCharRecord.class, original);

    segment.setRecord(replacement);

    assertSame(replacement, segment.getRecord());
  }

  @Test
  void parsedSegmentSetRecordRejectsNull() {
    ParsedSegment<TenCharRecord> segment = new ParsedSegment<>(TenCharRecord.class, tenChar("AAAAAAAAAA"));

    assertThrows(NullPointerException.class, () -> segment.setRecord(null));
  }

  // --- UnmatchedSegment ---

  @Test
  void unmatchedSegmentHoldsRawLineVerbatim() {
    UnmatchedSegment segment = new UnmatchedSegment("  COMMENT  ");

    assertEquals("  COMMENT  ", segment.getRawLine());
  }

  // --- FixedFormatWriter ---

  @Test
  void writerProducesEmptyOutputForEmptySegmentList() {
    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(List.of(), out);

    assertEquals("", out.toString());
  }

  @Test
  void writerExportsMatchedSegmentsViaManager() {
    List<Segment> segments = List.of(new ParsedSegment<>(TenCharRecord.class, tenChar("AAAAAAAAAA")));

    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(segments, out);

    assertEquals("AAAAAAAAAA\n", out.toString());
  }

  @Test
  void writerEmitsUnmatchedSegmentsVerbatim() {
    List<Segment> segments = List.of(new UnmatchedSegment("  COMMENT  "));

    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(segments, out);

    assertEquals("  COMMENT  \n", out.toString());
  }

  @Test
  void writerPreservesMixedOrderExactly() {
    FixedFormatSegmentReader reader = readerAB();
    List<Segment> segments = reader.readAsSegments(
        new StringReader("AAAAAAAAAA\nCOMMENT\nBBBBB     "));

    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(segments, out);

    assertEquals("AAAAAAAAAA\nCOMMENT\nBBBBB\n", out.toString());
  }

  @Test
  void writerWorksWithFile() throws IOException {
    Path file = tempDir.resolve("out.txt");
    List<Segment> segments = List.of(
        new ParsedSegment<>(TenCharRecord.class, tenChar("AAAAAAAAAA")),
        new UnmatchedSegment("COMMENT"));

    new FixedFormatWriter(new FixedFormatManagerImpl()).write(segments, file.toFile());

    assertEquals("AAAAAAAAAA\nCOMMENT\n", Files.readString(file));
  }

  @Test
  void writerWorksWithPath() throws IOException {
    Path file = tempDir.resolve("out.txt");
    List<Segment> segments = List.of(new UnmatchedSegment("RAW"));

    new FixedFormatWriter(new FixedFormatManagerImpl()).write(segments, file);

    assertEquals("RAW\n", Files.readString(file));
  }

  // --- Full round-trip ---

  @Test
  void roundTripEditsRecordAndPreservesOtherSegmentsInOrder() {
    FixedFormatSegmentReader reader = readerAB();
    List<Segment> segments = reader.readAsSegments(
        new StringReader("AAAAAAAAAA\nCOMMENT\nBBBBB     "));

    segments.stream()
        .filter(s -> s instanceof ParsedSegment && ((ParsedSegment<?>) s).isOf(TenCharRecord.class))
        .map(s -> (ParsedSegment<TenCharRecord>) s)
        .findFirst()
        .ifPresent(ps -> ps.getRecord().setValue("AAAAZZZZZZ"));

    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(segments, out);
    List<Segment> reread = reader.readAsSegments(new StringReader(out.toString()));

    assertEquals(3, reread.size());
    assertInstanceOf(ParsedSegment.class, reread.get(0));
    assertInstanceOf(UnmatchedSegment.class, reread.get(1));
    assertInstanceOf(ParsedSegment.class, reread.get(2));
    assertEquals("AAAAZZZZZZ", tenCharOf(reread.get(0)));
    assertEquals("COMMENT", ((UnmatchedSegment) reread.get(1)).getRawLine());
    assertEquals("BBBBB", fiveCharOf(reread.get(2)));
  }

  // --- helpers ---

  private FixedFormatSegmentReader readerAB() {
    return FixedFormatSegmentReader.builder()
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
  private String tenCharOf(Segment segment) {
    return ((ParsedSegment<TenCharRecord>) segment).getRecord().getValue();
  }

  @SuppressWarnings("unchecked")
  private String fiveCharOf(Segment segment) {
    return ((ParsedSegment<FiveCharRecord>) segment).getRecord().getCode();
  }
}
