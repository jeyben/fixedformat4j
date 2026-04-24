package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.read.FixedFormatMatchPattern;
import com.ancientprogramming.fixedformat4j.io.read.RegexFixedFormatMatchPattern;
import com.ancientprogramming.fixedformat4j.io.segment.ParsedSegment;
import com.ancientprogramming.fixedformat4j.io.segment.Segment;
import com.ancientprogramming.fixedformat4j.io.segment.UnmatchedSegment;
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
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatSegmentReader;

class TestFixedFormatSegmentReader {

  @TempDir
  Path tempDir;

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");
  private static final FixedFormatMatchPattern B_PATTERN = new RegexFixedFormatMatchPattern("^B");

  // --- builder ---

  @Test
  void builderRequiresAtLeastOneMapping() {
    assertThrows(IllegalArgumentException.class,
        () -> FixedFormatSegmentReader.builder().build());
  }

  @Test
  void builderReturnsSegmentReader() {
    assertNotNull(FixedFormatSegmentReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build());
  }

  // --- readAsSegments: empty input ---

  @Test
  void emptyInputProducesEmptyList() {
    List<Segment> segments = readerA().readAsSegments(new StringReader(""));

    assertTrue(segments.isEmpty());
  }

  // --- readAsSegments: all matched ---

  @Test
  void allMatchedLinesProduceParsedSegmentsInOrder() {
    List<Segment> segments = readerA().readAsSegments(new StringReader("AAAAAAAAAA\nAAAAAAAAAAAA"));

    assertEquals(2, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(0));
    assertInstanceOf(ParsedSegment.class, segments.get(1));
    assertEquals("AAAAAAAAAA", tenCharOf(segments.get(0)));
    assertEquals("AAAAAAAAAA", tenCharOf(segments.get(1)));
  }

  // --- readAsSegments: all unmatched ---

  @Test
  void allUnmatchedLinesProduceUnmatchedSegmentsInOrder() {
    List<Segment> segments = readerA().readAsSegments(new StringReader("COMMENT1\nCOMMENT2"));

    assertEquals(2, segments.size());
    assertInstanceOf(UnmatchedSegment.class, segments.get(0));
    assertInstanceOf(UnmatchedSegment.class, segments.get(1));
    assertEquals("COMMENT1", ((UnmatchedSegment) segments.get(0)).getRawLine());
    assertEquals("COMMENT2", ((UnmatchedSegment) segments.get(1)).getRawLine());
  }

  // --- readAsSegments: mixed ---

  @Test
  void mixedFilePreservesExactLineOrder() {
    List<Segment> segments = readerAB().readAsSegments(
        new StringReader("AAAAAAAAAA\nCOMMENT\nBBBBB     "));

    assertEquals(3, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(0));
    assertInstanceOf(UnmatchedSegment.class, segments.get(1));
    assertInstanceOf(ParsedSegment.class, segments.get(2));
    assertEquals("AAAAAAAAAA", tenCharOf(segments.get(0)));
    assertEquals("COMMENT", ((UnmatchedSegment) segments.get(1)).getRawLine());
    assertEquals("BBBBB", fiveCharOf(segments.get(2)));
  }

  @Test
  void unmatchedLinesAlwaysCapturedRegardlessOfBuilderConfig() {
    // FixedFormatSegmentReader has no unmatchStrategy — all unmatched become UnmatchedSegment
    FixedFormatSegmentReader reader = FixedFormatSegmentReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build();

    List<Segment> segments = reader.readAsSegments(new StringReader("AAAAAAAAAA\nCOMMENT"));

    assertEquals(2, segments.size());
    assertInstanceOf(UnmatchedSegment.class, segments.get(1));
    assertEquals("COMMENT", ((UnmatchedSegment) segments.get(1)).getRawLine());
  }

  // --- readAsSegments: input-source overloads ---

  @Test
  void worksWithInputStream() {
    byte[] bytes = "AAAAAAAAAA\nCOMMENT".getBytes(StandardCharsets.UTF_8);

    List<Segment> segments = readerA().readAsSegments(new ByteArrayInputStream(bytes));

    assertEquals(2, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(0));
    assertInstanceOf(UnmatchedSegment.class, segments.get(1));
  }

  @Test
  void worksWithInputStreamAndCharset() {
    byte[] bytes = "AAAAAAAAAA\nCOMMENT".getBytes(StandardCharsets.ISO_8859_1);

    List<Segment> segments = readerA().readAsSegments(new ByteArrayInputStream(bytes), StandardCharsets.ISO_8859_1);

    assertEquals(2, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(0));
    assertInstanceOf(UnmatchedSegment.class, segments.get(1));
  }

  @Test
  void worksWithFile() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nCOMMENT\nBBBBB     ");

    List<Segment> segments = readerAB().readAsSegments(file.toFile());

    assertEquals(3, segments.size());
    assertInstanceOf(ParsedSegment.class, segments.get(0));
    assertInstanceOf(UnmatchedSegment.class, segments.get(1));
    assertInstanceOf(ParsedSegment.class, segments.get(2));
  }

  @Test
  void worksWithFileAndCharset() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.write(file, "AAAAAAAAAA\nCOMMENT".getBytes(StandardCharsets.ISO_8859_1));

    List<Segment> segments = readerA().readAsSegments(file.toFile(), StandardCharsets.ISO_8859_1);

    assertEquals(2, segments.size());
  }

  @Test
  void worksWithPath() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nCOMMENT");

    List<Segment> segments = readerA().readAsSegments(file);

    assertEquals(2, segments.size());
  }

  @Test
  void worksWithPathAndCharset() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.write(file, "AAAAAAAAAA\nCOMMENT".getBytes(StandardCharsets.ISO_8859_1));

    List<Segment> segments = readerA().readAsSegments(file, StandardCharsets.ISO_8859_1);

    assertEquals(2, segments.size());
  }

  // --- helpers ---

  private FixedFormatSegmentReader readerA() {
    return FixedFormatSegmentReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build();
  }

  private FixedFormatSegmentReader readerAB() {
    return FixedFormatSegmentReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();
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
