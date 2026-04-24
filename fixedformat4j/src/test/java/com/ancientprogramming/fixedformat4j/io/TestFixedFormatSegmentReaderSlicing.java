package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatSegmentReader;
import com.ancientprogramming.fixedformat4j.io.read.LineSlicingStrategy;
import com.ancientprogramming.fixedformat4j.io.read.PartialChunkStrategy;
import com.ancientprogramming.fixedformat4j.io.read.RegexFixedFormatMatchPattern;
import com.ancientprogramming.fixedformat4j.io.segment.ParsedSegment;
import com.ancientprogramming.fixedformat4j.io.segment.Segment;
import com.ancientprogramming.fixedformat4j.io.segment.UnmatchedSegment;
import com.ancientprogramming.fixedformat4j.io.write.FixedFormatWriter;
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

class TestFixedFormatSegmentReaderSlicing {

  @TempDir
  Path tempDir;

  private static final RegexFixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");

  // --- single chunk per line → ParsedSegment ---

  @Test
  void singleMatchingChunkBecomesASingleParsedSegment() {
    List<Segment> segments = reader10A().readAsSegments(new StringReader("AAAAAAAAAA"));

    assertEquals(1, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(0));
    assertEquals("AAAAAAAAAA", tenCharOf(segments.get(0)));
  }

  // --- two full chunks per line ---

  @Test
  void twoMatchingChunksPerLineProduceTwoParsedSegmentsInOrder() {
    List<Segment> segments = reader10A()
        .readAsSegments(new StringReader("AAAAAA1234AAAAAA5678"));

    assertEquals(2, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(0));
    assertInstanceOf(ParsedSegment.class, segments.get(1));
    assertEquals("AAAAAA1234", tenCharOf(segments.get(0)));
    assertEquals("AAAAAA5678", tenCharOf(segments.get(1)));
  }

  // --- unmatched full chunk → UnmatchedSegment ---

  @Test
  void unmatchedChunkBecomesUnmatchedSegmentWithRawChunkText() {
    List<Segment> segments = reader10A()
        .readAsSegments(new StringReader("AAAAAAAAAA" + "BBBBBBBBBB"));

    assertEquals(2, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(0));
    assertInstanceOf(UnmatchedSegment.class, segments.get(1));
    assertEquals("BBBBBBBBBB", ((UnmatchedSegment) segments.get(1)).getRawLine());
  }

  // --- partial trailing chunk ---

  @Test
  void partialChunkWithSkipStrategySilentlyDropped() {
    List<Segment> segments = FixedFormatSegmentReader.builder()
        .lineSlicing(LineSlicingStrategy.packed(10))
        .addMapping(TenCharRecord.class, A_PATTERN)
        .partialChunkStrategy(PartialChunkStrategy.skip())
        .build()
        .readAsSegments(new StringReader("AAAAAAAAAA" + "AAAAA"));

    assertEquals(1, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(0));
  }

  @Test
  void partialChunkWithPadStrategyBecomesAParsedSegment() {
    List<Segment> segments = FixedFormatSegmentReader.builder()
        .lineSlicing(LineSlicingStrategy.packed(10))
        .addMapping(TenCharRecord.class, A_PATTERN)
        .partialChunkStrategy(PartialChunkStrategy.pad())
        .build()
        .readAsSegments(new StringReader("AAAAAAAAAA" + "AAAAA"));

    assertEquals(2, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(1));
    assertEquals("AAAAA", tenCharOf(segments.get(1)));
  }

  // --- filtered physical line → single UnmatchedSegment ---

  @Test
  void filteredPhysicalLineBecomesASingleUnmatchedSegment() {
    List<Segment> segments = FixedFormatSegmentReader.builder()
        .lineSlicing(LineSlicingStrategy.packed(10))
        .addMapping(TenCharRecord.class, A_PATTERN)
        .includeLines(line -> !line.startsWith("#"))
        .build()
        .readAsSegments(new StringReader("#comment...\n" + "AAAAAAAAAA"));

    assertEquals(2, segments.size());
    assertInstanceOf(UnmatchedSegment.class, segments.get(0));
    assertEquals("#comment...", ((UnmatchedSegment) segments.get(0)).getRawLine());
    assertInstanceOf(ParsedSegment.class, segments.get(1));
  }

  // --- mixed: parsed + unmatched + filtered → all in correct order ---

  @Test
  void mixedInputPreservesExactOrder() {
    List<Segment> segments = FixedFormatSegmentReader.builder()
        .lineSlicing(LineSlicingStrategy.packed(10))
        .addMapping(TenCharRecord.class, A_PATTERN)
        .includeLines(line -> !line.startsWith("#"))
        .build()
        .readAsSegments(new StringReader(
            "AAAAAA1234" + "BBBBBBBBBB" + "\n"
            + "#filtered\n"
            + "AAAAAA5678"));

    assertEquals(4, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(0));
    assertInstanceOf(UnmatchedSegment.class, segments.get(1));
    assertInstanceOf(UnmatchedSegment.class, segments.get(2));
    assertInstanceOf(ParsedSegment.class, segments.get(3));
    assertEquals("AAAAAA1234", tenCharOf(segments.get(0)));
    assertEquals("BBBBBBBBBB", ((UnmatchedSegment) segments.get(1)).getRawLine());
    assertEquals("#filtered", ((UnmatchedSegment) segments.get(2)).getRawLine());
    assertEquals("AAAAAA5678", tenCharOf(segments.get(3)));
  }

  // --- round-trip: edit a record and write back via FixedFormatWriter ---

  @Test
  void roundTripEditsRecordAndWritesBackUnpacked() {
    List<Segment> segments = reader10A()
        .readAsSegments(new StringReader("AAAAAAAAAA" + "AAAAAA5678"));

    segments.stream()
        .filter(s -> s instanceof ParsedSegment && ((ParsedSegment<?>) s).isOf(TenCharRecord.class))
        .map(s -> (ParsedSegment<TenCharRecord>) s)
        .filter(ps -> ps.getRecord().getValue().endsWith("5678"))
        .findFirst()
        .ifPresent(ps -> ps.getRecord().setValue("AAAAAA9999"));

    StringWriter out = new StringWriter();
    new FixedFormatWriter(new FixedFormatManagerImpl()).write(segments, out);

    List<Segment> reread = reader10A()
        .readAsSegments(new StringReader(out.toString()));
    assertEquals(2, reread.size());
    assertEquals("AAAAAAAAAA", tenCharOf(reread.get(0)));
    assertEquals("AAAAAA9999", tenCharOf(reread.get(1)));
  }

  // --- mixed-file round-trip: header (single) + packed details + trailer (single) ---

  @Test
  void mixedFileRoundTripPreservesAllSegments() {
    String input = "HDR 20260424\n"
        + "AAAAAAAAAA" + "AAAAAA5678" + "\n"
        + "TRL000002\n";

    FixedFormatSegmentReader reader = FixedFormatSegmentReader.builder()
        .lineSlicing(LineSlicingStrategy.mixed(line -> line.startsWith("A"), 10))
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build();

    List<Segment> segments = reader.readAsSegments(new StringReader(input));

    // HDR line → UnmatchedSegment, 2 packed A-records, TRL → UnmatchedSegment
    assertEquals(4, segments.size());
    assertInstanceOf(UnmatchedSegment.class, segments.get(0));
    assertInstanceOf(ParsedSegment.class, segments.get(1));
    assertInstanceOf(ParsedSegment.class, segments.get(2));
    assertInstanceOf(UnmatchedSegment.class, segments.get(3));

    assertEquals("HDR 20260424", ((UnmatchedSegment) segments.get(0)).getRawLine());
    assertEquals("AAAAAAAAAA", tenCharOf(segments.get(1)));
    assertEquals("AAAAAA5678", tenCharOf(segments.get(2)));
    assertEquals("TRL000002", ((UnmatchedSegment) segments.get(3)).getRawLine());
  }

  // --- input-source overloads ---

  @Test
  void worksWithInputStream() {
    byte[] bytes = ("AAAAAAAAAA" + "BBBBBBBBBB").getBytes(StandardCharsets.UTF_8);
    List<Segment> segments = reader10A().readAsSegments(new ByteArrayInputStream(bytes));

    assertEquals(2, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(0));
    assertInstanceOf(UnmatchedSegment.class, segments.get(1));
  }

  @Test
  void worksWithFile() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA" + "AAAAAAAAAA");

    List<Segment> segments = reader10A().readAsSegments(file.toFile());
    assertEquals(2, segments.size());
  }

  @Test
  void worksWithPath() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA");

    List<Segment> segments = reader10A().readAsSegments(file);
    assertEquals(1, segments.size());
  }

  // --- helpers ---

  private FixedFormatSegmentReader reader10A() {
    return FixedFormatSegmentReader.builder()
        .lineSlicing(LineSlicingStrategy.packed(10))
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build();
  }

  @SuppressWarnings("unchecked")
  private String tenCharOf(Segment segment) {
    return ((ParsedSegment<TenCharRecord>) segment).getRecord().getValue();
  }
}
