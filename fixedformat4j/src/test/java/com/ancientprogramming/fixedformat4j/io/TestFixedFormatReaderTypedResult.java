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

class TestFixedFormatReaderTypedResult {

  @TempDir
  Path tempDir;

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");
  private static final FixedFormatMatchPattern B_PATTERN = new RegexFixedFormatMatchPattern("^B");

  private FixedFormatReader multiTypeReader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();
  }

  @Test
  void getReturnsCastFreeTypedListForEachClass() {
    TypedReadResult result = multiTypeReader().readAsTypedResult(
        new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    List<TenCharRecord> tens = result.get(TenCharRecord.class);
    List<FiveCharRecord> fives = result.get(FiveCharRecord.class);

    assertEquals(1, tens.size());
    assertEquals(1, fives.size());
    assertEquals("AAAAAAAAAA", tens.get(0).getValue());
    assertEquals("BBBBB", fives.get(0).getCode());
  }

  @Test
  void getAllReturnsFlatListInEncounterOrder() {
    TypedReadResult result = multiTypeReader().readAsTypedResult(
        new StringReader("AAAAAAAAAA\nBBBBBBBBBB\nAAAAAAAAAAAA"));

    List<Object> all = result.getAll();
    assertEquals(3, all.size());
    assertInstanceOf(TenCharRecord.class, all.get(0));
    assertInstanceOf(FiveCharRecord.class, all.get(1));
    assertInstanceOf(TenCharRecord.class, all.get(2));
  }

  @Test
  void containsChecksPresenceByClass() {
    TypedReadResult result = multiTypeReader().readAsTypedResult(
        new StringReader("AAAAAAAAAA"));

    assertTrue(result.contains(TenCharRecord.class));
    assertFalse(result.contains(FiveCharRecord.class));
  }

  @Test
  void classesReturnsRegisteredClassesWithRecords() {
    TypedReadResult result = multiTypeReader().readAsTypedResult(
        new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    assertTrue(result.classes().contains(TenCharRecord.class));
    assertTrue(result.classes().contains(FiveCharRecord.class));
  }

  @Test
  void readAsTypedResultExcludesClassesWithNoMatches() {
    TypedReadResult result = multiTypeReader().readAsTypedResult(
        new StringReader("AAAAAAAAAA"));

    assertEquals(1, result.classes().size());
    assertTrue(result.classes().contains(TenCharRecord.class));
    assertFalse(result.classes().contains(FiveCharRecord.class));
  }

  @Test
  void readAsTypedResultWorksWithInputStream() {
    ByteArrayInputStream is = new ByteArrayInputStream(
        "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.UTF_8));

    TypedReadResult result = multiTypeReader().readAsTypedResult(is);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsTypedResultWorksWithInputStreamAndCharset() {
    ByteArrayInputStream is = new ByteArrayInputStream(
        "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.ISO_8859_1));

    TypedReadResult result = multiTypeReader().readAsTypedResult(is, StandardCharsets.ISO_8859_1);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsTypedResultWorksWithFile() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nBBBBBBBBBB", StandardCharsets.UTF_8);

    TypedReadResult result = multiTypeReader().readAsTypedResult(file.toFile());

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsTypedResultWorksWithFileAndCharset() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.write(file, "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.ISO_8859_1));

    TypedReadResult result = multiTypeReader().readAsTypedResult(file.toFile(), StandardCharsets.ISO_8859_1);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsTypedResultWorksWithPath() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nBBBBBBBBBB", StandardCharsets.UTF_8);

    TypedReadResult result = multiTypeReader().readAsTypedResult(file);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsTypedResultWorksWithPathAndCharset() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.write(file, "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.ISO_8859_1));

    TypedReadResult result = multiTypeReader().readAsTypedResult(file, StandardCharsets.ISO_8859_1);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }
}
