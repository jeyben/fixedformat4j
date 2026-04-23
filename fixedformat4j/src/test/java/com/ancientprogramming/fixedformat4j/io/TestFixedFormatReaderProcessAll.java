package com.ancientprogramming.fixedformat4j.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatReaderProcessAll {

  @TempDir
  Path tempDir;

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");
  private static final FixedFormatMatchPattern B_PATTERN = new RegexFixedFormatMatchPattern("^B");

  @Test
  void processAllFiresTypedHandlerForEachRecord() {
    List<TenCharRecord> tens = new ArrayList<>();
    List<FiveCharRecord> fives = new ArrayList<>();

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN, tens::add)
        .addMapping(FiveCharRecord.class, B_PATTERN, fives::add)
        .build()
        .processAll(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    assertEquals(1, tens.size());
    assertEquals(1, fives.size());
    assertEquals("AAAAAAAAAA", tens.get(0).getValue());
    assertEquals("BBBBB", fives.get(0).getCode());
  }

  @Test
  void processAllFiresHandlersInEncounterOrder() {
    List<String> order = new ArrayList<>();

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN, r -> order.add("ten:" + r.getValue().trim()))
        .addMapping(FiveCharRecord.class, B_PATTERN, r -> order.add("five:" + r.getCode().trim()))
        .build()
        .processAll(new StringReader("BBBBBBBBBB\nAAAAAAAAAAAA"));

    assertEquals(List.of("five:BBBBB", "ten:AAAAAAAAAA"), order);
  }

  @Test
  void processAllSilentlySkipsMappingsWithNoHandler() {
    List<FiveCharRecord> fives = new ArrayList<>();

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)           // no handler
        .addMapping(FiveCharRecord.class, B_PATTERN, fives::add)
        .build()
        .processAll(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    assertEquals(1, fives.size());
  }

  @Test
  void processAllWorksWithInputStream() {
    List<TenCharRecord> results = new ArrayList<>();
    ByteArrayInputStream is = new ByteArrayInputStream(
        "AAAAAAAAAA".getBytes(StandardCharsets.UTF_8));

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN, results::add)
        .build()
        .processAll(is);

    assertEquals(1, results.size());
  }

  @Test
  void processAllWorksWithInputStreamAndCharset() {
    List<TenCharRecord> results = new ArrayList<>();
    ByteArrayInputStream is = new ByteArrayInputStream(
        "AAAAAAAAAA".getBytes(StandardCharsets.ISO_8859_1));

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN, results::add)
        .build()
        .processAll(is, StandardCharsets.ISO_8859_1);

    assertEquals(1, results.size());
  }

  @Test
  void processAllWorksWithFile() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nBBBBBBBBBB", StandardCharsets.UTF_8);
    List<TenCharRecord> tens = new ArrayList<>();
    List<FiveCharRecord> fives = new ArrayList<>();

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN, tens::add)
        .addMapping(FiveCharRecord.class, B_PATTERN, fives::add)
        .build()
        .processAll(file.toFile());

    assertEquals(1, tens.size());
    assertEquals(1, fives.size());
  }

  @Test
  void processAllWorksWithPath() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nBBBBBBBBBB", StandardCharsets.UTF_8);
    List<TenCharRecord> tens = new ArrayList<>();
    List<FiveCharRecord> fives = new ArrayList<>();

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN, tens::add)
        .addMapping(FiveCharRecord.class, B_PATTERN, fives::add)
        .build()
        .processAll(file);

    assertEquals(1, tens.size());
    assertEquals(1, fives.size());
  }

  @Test
  void processAllDoesNotInterfereWithReadWithCallback() {
    List<TenCharRecord> fromHandler = new ArrayList<>();
    List<Class<?>> fromCallback = new ArrayList<>();

    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN, fromHandler::add)
        .build();

    reader.processAll(new StringReader("AAAAAAAAAA"));
    reader.readWithCallback(new StringReader("AAAAAAAAAA"),
        (clazz, record) -> fromCallback.add(clazz));

    assertEquals(1, fromHandler.size());
    assertEquals(1, fromCallback.size());
  }
}
