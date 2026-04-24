package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.read.ReadResult;
import com.ancientprogramming.fixedformat4j.io.read.RegexLinePattern;
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
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;

class TestFixedFormatReaderTypedResult {

  @TempDir
  Path tempDir;

  private static final LinePattern A_PATTERN = new RegexLinePattern("^A");
  private static final LinePattern B_PATTERN = new RegexLinePattern("^B");

  private FixedFormatReader multiTypeReader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();
  }

  @Test
  void getReturnsCastFreeTypedListForEachClass() {
    ReadResult result = multiTypeReader().readAsResult(
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
    ReadResult result = multiTypeReader().readAsResult(
        new StringReader("AAAAAAAAAA\nBBBBBBBBBB\nAAAAAAAAAAAA"));

    List<Object> all = result.getAll();
    assertEquals(3, all.size());
    assertInstanceOf(TenCharRecord.class, all.get(0));
    assertInstanceOf(FiveCharRecord.class, all.get(1));
    assertInstanceOf(TenCharRecord.class, all.get(2));
  }

  @Test
  void containsChecksPresenceByClass() {
    ReadResult result = multiTypeReader().readAsResult(
        new StringReader("AAAAAAAAAA"));

    assertTrue(result.contains(TenCharRecord.class));
    assertFalse(result.contains(FiveCharRecord.class));
  }

  @Test
  void classesReturnsRegisteredClassesWithRecords() {
    ReadResult result = multiTypeReader().readAsResult(
        new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    assertTrue(result.classes().contains(TenCharRecord.class));
    assertTrue(result.classes().contains(FiveCharRecord.class));
  }

  @Test
  void readAsResultExcludesClassesWithNoMatches() {
    ReadResult result = multiTypeReader().readAsResult(
        new StringReader("AAAAAAAAAA"));

    assertEquals(1, result.classes().size());
    assertTrue(result.classes().contains(TenCharRecord.class));
    assertFalse(result.classes().contains(FiveCharRecord.class));
  }

  @Test
  void readAsResultWorksWithInputStream() {
    ByteArrayInputStream is = new ByteArrayInputStream(
        "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.UTF_8));

    ReadResult result = multiTypeReader().readAsResult(is);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsResultWorksWithInputStreamAndCharset() {
    ByteArrayInputStream is = new ByteArrayInputStream(
        "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.ISO_8859_1));

    ReadResult result = multiTypeReader().readAsResult(is, StandardCharsets.ISO_8859_1);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsResultWorksWithFile() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nBBBBBBBBBB", StandardCharsets.UTF_8);

    ReadResult result = multiTypeReader().readAsResult(file.toFile());

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsResultWorksWithFileAndCharset() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.write(file, "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.ISO_8859_1));

    ReadResult result = multiTypeReader().readAsResult(file.toFile(), StandardCharsets.ISO_8859_1);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsResultWorksWithPath() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nBBBBBBBBBB", StandardCharsets.UTF_8);

    ReadResult result = multiTypeReader().readAsResult(file);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsResultWorksWithPathAndCharset() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.write(file, "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.ISO_8859_1));

    ReadResult result = multiTypeReader().readAsResult(file, StandardCharsets.ISO_8859_1);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }
}
