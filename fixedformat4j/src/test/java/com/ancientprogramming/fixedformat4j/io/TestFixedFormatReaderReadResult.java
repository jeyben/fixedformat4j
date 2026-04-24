package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.read.ReadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;

class TestFixedFormatReaderReadResult {

  @TempDir
  Path tempDir;

  private static final Predicate<String> A_PATTERN = Pattern.compile("^A").asPredicate();
  private static final Predicate<String> B_PATTERN = Pattern.compile("^B").asPredicate();

  private FixedFormatReader multiTypeReader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();
  }

  @Test
  void getReturnsCastFreeTypedListForEachClass() {
    ReadResult result = multiTypeReader().read(
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
    ReadResult result = multiTypeReader().read(
        new StringReader("AAAAAAAAAA\nBBBBBBBBBB\nAAAAAAAAAAAA"));

    List<Object> all = result.getAll();
    assertEquals(3, all.size());
    assertInstanceOf(TenCharRecord.class, all.get(0));
    assertInstanceOf(FiveCharRecord.class, all.get(1));
    assertInstanceOf(TenCharRecord.class, all.get(2));
  }

  @Test
  void containsChecksPresenceByClass() {
    ReadResult result = multiTypeReader().read(
        new StringReader("AAAAAAAAAA"));

    assertTrue(result.contains(TenCharRecord.class));
    assertFalse(result.contains(FiveCharRecord.class));
  }

  @Test
  void classesReturnsRegisteredClassesWithRecords() {
    ReadResult result = multiTypeReader().read(
        new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    assertTrue(result.classes().contains(TenCharRecord.class));
    assertTrue(result.classes().contains(FiveCharRecord.class));
  }

  @Test
  void readExcludesClassesWithNoMatches() {
    ReadResult result = multiTypeReader().read(
        new StringReader("AAAAAAAAAA"));

    assertEquals(1, result.classes().size());
    assertTrue(result.classes().contains(TenCharRecord.class));
    assertFalse(result.classes().contains(FiveCharRecord.class));
  }

  @Test
  void readWorksWithInputStream() {
    ByteArrayInputStream is = new ByteArrayInputStream(
        "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.UTF_8));

    ReadResult result = multiTypeReader().read(is);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readWorksWithInputStreamAndCharset() {
    ByteArrayInputStream is = new ByteArrayInputStream(
        "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.ISO_8859_1));

    ReadResult result = multiTypeReader().read(is, StandardCharsets.ISO_8859_1);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readWorksWithPath() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nBBBBBBBBBB", StandardCharsets.UTF_8);

    ReadResult result = multiTypeReader().read(file);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readWorksWithPathAndCharset() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.write(file, "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.ISO_8859_1));

    ReadResult result = multiTypeReader().read(file, StandardCharsets.ISO_8859_1);

    assertEquals(1, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }
}
