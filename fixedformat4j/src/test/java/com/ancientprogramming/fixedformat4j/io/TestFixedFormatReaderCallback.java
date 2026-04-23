package com.ancientprogramming.fixedformat4j.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatReaderCallback {

  @TempDir
  Path tempDir;

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");
  private static final FixedFormatMatchPattern B_PATTERN = new RegexFixedFormatMatchPattern("^B");

  private FixedFormatReader heterogeneousReader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();
  }

  @Test
  void consumerCallbackInvokesForEachRecordInOrder() {
    List<String> values = new ArrayList<>();
    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"),
            record -> values.add(record.getValue()))
        .build()
        .processAll(new StringReader("hello     \nworld     "));

    assertEquals(List.of("hello", "world"), values);
  }

  @Test
  void biConsumerCallbackReceivesMatchedClassAndRecord() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();

    List<String> captured = new ArrayList<>();
    reader.readWithCallback(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"),
        (clazz, record) -> captured.add(clazz.getSimpleName()));

    assertEquals(List.of("TenCharRecord", "FiveCharRecord"), captured);
  }

  @Test
  void biConsumerCallbackPreservesEncounterOrder() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();

    List<Class<?>> classes = new ArrayList<>();
    reader.readWithCallback(new StringReader("BBBBBBBBBB\nAAAAAAAAAAAA"),
        (clazz, record) -> classes.add(clazz));

    assertEquals(FiveCharRecord.class, classes.get(0));
    assertEquals(TenCharRecord.class, classes.get(1));
  }

  @Test
  void consumerCallbackWorksWithInputStreamAndExplicitCharset() {
    List<String> values = new ArrayList<>();
    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"),
            record -> values.add(record.getValue()))
        .build()
        .processAll(
            new ByteArrayInputStream("hello     \nworld     ".getBytes(StandardCharsets.ISO_8859_1)),
            StandardCharsets.ISO_8859_1);
    assertEquals(List.of("hello", "world"), values);
  }

  @Test
  void biConsumerCallbackWorksWithInputStream() {
    InputStream is = new ByteArrayInputStream("AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.UTF_8));
    List<String> captured = new ArrayList<>();
    heterogeneousReader().readWithCallback(is, (clazz, record) -> captured.add(clazz.getSimpleName()));
    assertEquals(List.of("TenCharRecord", "FiveCharRecord"), captured);
  }

  @Test
  void biConsumerCallbackWorksWithInputStreamAndCharset() {
    InputStream is = new ByteArrayInputStream("AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.ISO_8859_1));
    List<String> captured = new ArrayList<>();
    heterogeneousReader().readWithCallback(is, StandardCharsets.ISO_8859_1, (clazz, record) -> captured.add(clazz.getSimpleName()));
    assertEquals(List.of("TenCharRecord", "FiveCharRecord"), captured);
  }

  @Test
  void biConsumerCallbackWorksWithFile() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nBBBBBBBBBB", StandardCharsets.UTF_8);
    List<String> captured = new ArrayList<>();
    heterogeneousReader().readWithCallback(file.toFile(), (clazz, record) -> captured.add(clazz.getSimpleName()));
    assertEquals(List.of("TenCharRecord", "FiveCharRecord"), captured);
  }

  @Test
  void biConsumerCallbackWorksWithFileAndCharset() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.write(file, "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.ISO_8859_1));
    List<String> captured = new ArrayList<>();
    heterogeneousReader().readWithCallback(file.toFile(), StandardCharsets.ISO_8859_1, (clazz, record) -> captured.add(clazz.getSimpleName()));
    assertEquals(List.of("TenCharRecord", "FiveCharRecord"), captured);
  }

  @Test
  void biConsumerCallbackWorksWithPath() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.writeString(file, "AAAAAAAAAA\nBBBBBBBBBB", StandardCharsets.UTF_8);
    List<String> captured = new ArrayList<>();
    heterogeneousReader().readWithCallback(file, (clazz, record) -> captured.add(clazz.getSimpleName()));
    assertEquals(List.of("TenCharRecord", "FiveCharRecord"), captured);
  }

  @Test
  void biConsumerCallbackWorksWithPathAndCharset() throws IOException {
    Path file = tempDir.resolve("data.txt");
    Files.write(file, "AAAAAAAAAA\nBBBBBBBBBB".getBytes(StandardCharsets.ISO_8859_1));
    List<String> captured = new ArrayList<>();
    heterogeneousReader().readWithCallback(file, StandardCharsets.ISO_8859_1, (clazz, record) -> captured.add(clazz.getSimpleName()));
    assertEquals(List.of("TenCharRecord", "FiveCharRecord"), captured);
  }
}
