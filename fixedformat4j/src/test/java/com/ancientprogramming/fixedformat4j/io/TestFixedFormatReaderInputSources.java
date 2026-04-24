package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.pattern.RegexFixedFormatMatchPattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatReaderInputSources {

  @TempDir
  Path tempDir;

  private FixedFormatReader reader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .build();
  }

  private Path writeTemp(String content) throws IOException {
    Path file = tempDir.resolve("test.txt");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file;
  }

  private static class TrackingInputStream extends ByteArrayInputStream {
    boolean closed = false;

    TrackingInputStream(byte[] buf) { super(buf); }

    @Override
    public void close() throws IOException {
      closed = true;
      super.close();
    }
  }

  @Test
  void readsFromFile() throws IOException {
    Path path = writeTemp("hello     \nworld     ");
    List<TenCharRecord> results = reader().readAsTypedResult(path.toFile()).get(TenCharRecord.class);
    assertEquals(2, results.size());
    assertEquals("hello", results.get(0).getValue());
  }

  @Test
  void readsFromFileWithExplicitCharset() throws IOException {
    Path path = tempDir.resolve("latin.txt");
    Files.write(path, "hello     \nworld     ".getBytes(StandardCharsets.ISO_8859_1));

    List<TenCharRecord> results = reader()
        .readAsTypedResult(path.toFile(), StandardCharsets.ISO_8859_1)
        .get(TenCharRecord.class);
    assertEquals(2, results.size());
  }

  @Test
  void readsFromPath() throws IOException {
    Path path = writeTemp("hello     \nworld     ");
    List<TenCharRecord> results = reader().readAsTypedResult(path).get(TenCharRecord.class);
    assertEquals(2, results.size());
    assertEquals("world", results.get(1).getValue());
  }

  @Test
  void readsFromPathWithExplicitCharset() throws IOException {
    Path path = tempDir.resolve("latin.txt");
    Files.write(path, "hello     \nworld     ".getBytes(StandardCharsets.ISO_8859_1));

    List<TenCharRecord> results = reader()
        .readAsTypedResult(path, StandardCharsets.ISO_8859_1)
        .get(TenCharRecord.class);
    assertEquals(2, results.size());
  }

  @Test
  void readsFromInputStream() {
    InputStream is = new ByteArrayInputStream("hello     \nworld     ".getBytes(StandardCharsets.UTF_8));
    List<TenCharRecord> results = reader().readAsTypedResult(is).get(TenCharRecord.class);
    assertEquals(2, results.size());
  }

  @Test
  void readsFromInputStreamWithExplicitCharset() {
    InputStream is = new ByteArrayInputStream("hello     \nworld     ".getBytes(StandardCharsets.ISO_8859_1));
    List<TenCharRecord> results = reader()
        .readAsTypedResult(is, StandardCharsets.ISO_8859_1)
        .get(TenCharRecord.class);
    assertEquals(2, results.size());
  }

  @Test
  void defaultCharsetIsUtf8() throws IOException {
    String content = "hello     \nworld     ";
    Path path = tempDir.resolve("utf8.txt");
    Files.writeString(path, content, StandardCharsets.UTF_8);

    List<TenCharRecord> withDefault = reader().readAsTypedResult(path.toFile()).get(TenCharRecord.class);
    List<TenCharRecord> withExplicit = reader()
        .readAsTypedResult(path.toFile(), StandardCharsets.UTF_8)
        .get(TenCharRecord.class);
    assertEquals(withExplicit.size(), withDefault.size());
  }

  @Test
  void inputStreamIsClosedAfterReadAsList() {
    TrackingInputStream is = new TrackingInputStream("hello     \nworld     ".getBytes(StandardCharsets.UTF_8));
    reader().readAsList(is);
    assertTrue(is.closed, "InputStream should be closed after readAsList");
  }

  @Test
  void inputStreamIsClosedAfterReadAsTypedResult() {
    TrackingInputStream is = new TrackingInputStream("hello     \nworld     ".getBytes(StandardCharsets.UTF_8));
    reader().readAsTypedResult(is);
    assertTrue(is.closed, "InputStream should be closed after readAsTypedResult");
  }

  @Test
  void inputStreamIsClosedAfterReadWithCallbackConsumer() {
    TrackingInputStream is = new TrackingInputStream("hello     \nworld     ".getBytes(StandardCharsets.UTF_8));
    List<String> values = new ArrayList<>();
    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"),
            r -> values.add(r.getValue()))
        .build()
        .processAll(is);
    assertTrue(is.closed, "InputStream should be closed after processAll(Consumer)");
  }

  @Test
  void inputStreamIsClosedAfterReadWithCallbackBiConsumer() {
    FixedFormatReader r = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .build();
    TrackingInputStream is = new TrackingInputStream("hello     \nworld     ".getBytes(StandardCharsets.UTF_8));
    r.readWithCallback(is, (clazz, record) -> {});
    assertTrue(is.closed, "InputStream should be closed after readWithCallback(BiConsumer)");
  }
}
