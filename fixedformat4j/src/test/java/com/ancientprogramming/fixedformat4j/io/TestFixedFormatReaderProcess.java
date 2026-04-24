package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.read.HandlerRegistry;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.read.MultiMatchStrategy;
import com.ancientprogramming.fixedformat4j.io.read.RegexLinePattern;
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
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;

class TestFixedFormatReaderProcess {

  @TempDir
  Path tempDir;

  private static final LinePattern A_PATTERN = new RegexLinePattern("^A");
  private static final LinePattern B_PATTERN = new RegexLinePattern("^B");

  @Test
  void processFiresTypedHandlerForEachRecord() {
    List<TenCharRecord> tens = new ArrayList<>();
    List<FiveCharRecord> fives = new ArrayList<>();

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build()
        .process(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"), new HandlerRegistry()
            .on(TenCharRecord.class, tens::add)
            .on(FiveCharRecord.class, fives::add));

    assertEquals(1, tens.size());
    assertEquals(1, fives.size());
    assertEquals("AAAAAAAAAA", tens.get(0).getValue());
    assertEquals("BBBBB", fives.get(0).getCode());
  }

  @Test
  void processFiresHandlersInEncounterOrder() {
    List<String> order = new ArrayList<>();

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build()
        .process(new StringReader("BBBBBBBBBB\nAAAAAAAAAAAA"), new HandlerRegistry()
            .on(TenCharRecord.class, r -> order.add("ten:" + r.getValue().trim()))
            .on(FiveCharRecord.class, r -> order.add("five:" + r.getCode().trim())));

    assertEquals(List.of("five:BBBBB", "ten:AAAAAAAAAA"), order);
  }

  @Test
  void processSilentlyIgnoresClassesNotInRegistry() {
    List<FiveCharRecord> fives = new ArrayList<>();

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build()
        .process(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"), new HandlerRegistry()
            .on(FiveCharRecord.class, fives::add));  // TenCharRecord not registered

    assertEquals(1, fives.size());
  }

  @Test
  void processWorksWithInputStream() {
    List<TenCharRecord> results = new ArrayList<>();
    ByteArrayInputStream is = new ByteArrayInputStream(
        "AAAAAAAAAA".getBytes(StandardCharsets.UTF_8));

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build()
        .process(is, new HandlerRegistry().on(TenCharRecord.class, results::add));

    assertEquals(1, results.size());
  }

  @Test
  void processWorksWithInputStreamAndCharset() {
    List<TenCharRecord> results = new ArrayList<>();
    ByteArrayInputStream is = new ByteArrayInputStream(
        "AAAAAAAAAA".getBytes(StandardCharsets.ISO_8859_1));

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build()
        .process(is, StandardCharsets.ISO_8859_1,
            new HandlerRegistry().on(TenCharRecord.class, results::add));

    assertEquals(1, results.size());
  }

  @Test
  void processWorksWithFile() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nBBBBBBBBBB", StandardCharsets.UTF_8);
    List<TenCharRecord> tens = new ArrayList<>();
    List<FiveCharRecord> fives = new ArrayList<>();

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build()
        .process(file.toFile(), new HandlerRegistry()
            .on(TenCharRecord.class, tens::add)
            .on(FiveCharRecord.class, fives::add));

    assertEquals(1, tens.size());
    assertEquals(1, fives.size());
  }

  @Test
  void processWorksWithPath() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nBBBBBBBBBB", StandardCharsets.UTF_8);
    List<TenCharRecord> tens = new ArrayList<>();
    List<FiveCharRecord> fives = new ArrayList<>();

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build()
        .process(file, new HandlerRegistry()
            .on(TenCharRecord.class, tens::add)
            .on(FiveCharRecord.class, fives::add));

    assertEquals(1, tens.size());
    assertEquals(1, fives.size());
  }

  @Test
  void processFiresHandlerOnceWhenFirstMatchStrategyIsActive() {
    List<TenCharRecord> fired = new ArrayList<>();

    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(TenCharRecord.class, new RegexLinePattern(".*"))
        .multiMatchStrategy(MultiMatchStrategy.firstMatch())
        .build()
        .process(new StringReader("AAAAAAAAAA"),
            new HandlerRegistry().on(TenCharRecord.class, fired::add));

    assertEquals(1, fired.size(), "firstMatch should emit exactly one record");
  }

  @Test
  void throwsIllegalArgumentWhenRegistryIsNull() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build();

    assertThrows(IllegalArgumentException.class, () ->
        reader.process(new StringReader("AAAAAAAAAA"), null));
  }

  @Test
  void processAndReadAsResultAreIndependent() {
    List<TenCharRecord> fromHandler = new ArrayList<>();

    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .build();

    reader.process(new StringReader("AAAAAAAAAA"),
        new HandlerRegistry().on(TenCharRecord.class, fromHandler::add));
    List<Object> fromResult = reader.readAsResult(new StringReader("AAAAAAAAAA")).getAll();

    assertEquals(1, fromHandler.size());
    assertEquals(1, fromResult.size());
  }
}
