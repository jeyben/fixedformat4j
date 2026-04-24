package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.read.HandlerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;

class TestFixedFormatReaderInputSources {

  @TempDir
  Path tempDir;

  private FixedFormatReader reader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, Pattern.compile(".*").asPredicate())
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
    List<TenCharRecord> results = reader().readAsResult(path.toFile()).get(TenCharRecord.class);
    assertEquals(2, results.size());
    assertEquals("hello", results.get(0).getValue());
  }

  @Test
  void readsFromFileWithExplicitCharset() throws IOException {
    Path path = tempDir.resolve("latin.txt");
    Files.write(path, "hello     \nworld     ".getBytes(StandardCharsets.ISO_8859_1));

    List<TenCharRecord> results = reader()
        .readAsResult(path.toFile(), StandardCharsets.ISO_8859_1)
        .get(TenCharRecord.class);
    assertEquals(2, results.size());
  }

  @Test
  void readsFromPath() throws IOException {
    Path path = writeTemp("hello     \nworld     ");
    List<TenCharRecord> results = reader().readAsResult(path).get(TenCharRecord.class);
    assertEquals(2, results.size());
    assertEquals("world", results.get(1).getValue());
  }

  @Test
  void readsFromPathWithExplicitCharset() throws IOException {
    Path path = tempDir.resolve("latin.txt");
    Files.write(path, "hello     \nworld     ".getBytes(StandardCharsets.ISO_8859_1));

    List<TenCharRecord> results = reader()
        .readAsResult(path, StandardCharsets.ISO_8859_1)
        .get(TenCharRecord.class);
    assertEquals(2, results.size());
  }

  @Test
  void readsFromInputStream() {
    InputStream is = new ByteArrayInputStream("hello     \nworld     ".getBytes(StandardCharsets.UTF_8));
    List<TenCharRecord> results = reader().readAsResult(is).get(TenCharRecord.class);
    assertEquals(2, results.size());
  }

  @Test
  void readsFromInputStreamWithExplicitCharset() {
    InputStream is = new ByteArrayInputStream("hello     \nworld     ".getBytes(StandardCharsets.ISO_8859_1));
    List<TenCharRecord> results = reader()
        .readAsResult(is, StandardCharsets.ISO_8859_1)
        .get(TenCharRecord.class);
    assertEquals(2, results.size());
  }

  @Test
  void defaultCharsetIsUtf8() throws IOException {
    String content = "hello     \nworld     ";
    Path path = tempDir.resolve("utf8.txt");
    Files.writeString(path, content, StandardCharsets.UTF_8);

    List<TenCharRecord> withDefault = reader().readAsResult(path.toFile()).get(TenCharRecord.class);
    List<TenCharRecord> withExplicit = reader()
        .readAsResult(path.toFile(), StandardCharsets.UTF_8)
        .get(TenCharRecord.class);
    assertEquals(withExplicit.size(), withDefault.size());
  }

  @Test
  void inputStreamIsClosedAfterReadAsResult() {
    TrackingInputStream is = new TrackingInputStream("hello     \nworld     ".getBytes(StandardCharsets.UTF_8));
    reader().readAsResult(is);
    assertTrue(is.closed, "InputStream should be closed after readAsResult");
  }

  @Test
  void throwsNullPointerWhenReaderIsNull() {
    assertThrows(NullPointerException.class, () ->
        reader().readAsResult((java.io.Reader) null));
  }

  @Test
  void throwsNullPointerWhenInputStreamIsNull() {
    assertThrows(NullPointerException.class, () ->
        reader().readAsResult((java.io.InputStream) null));
  }

  @Test
  void throwsNullPointerWhenInputStreamCharsetIsNull() {
    assertThrows(NullPointerException.class, () ->
        reader().readAsResult(new ByteArrayInputStream("data".getBytes()), null));
  }

  @Test
  void throwsNullPointerWhenFileIsNull() {
    assertThrows(NullPointerException.class, () ->
        reader().readAsResult((java.io.File) null));
  }

  @Test
  void throwsNullPointerWhenFileCharsetIsNull() throws IOException {
    Path path = writeTemp("hello     ");
    assertThrows(NullPointerException.class, () ->
        reader().readAsResult(path.toFile(), null));
  }

  @Test
  void throwsNullPointerWhenPathIsNull() {
    assertThrows(NullPointerException.class, () ->
        reader().readAsResult((java.nio.file.Path) null));
  }

  @Test
  void throwsNullPointerWhenPathCharsetIsNull() throws IOException {
    Path path = writeTemp("hello     ");
    assertThrows(NullPointerException.class, () ->
        reader().readAsResult(path, null));
  }

  @Test
  void inputStreamIsClosedAfterProcess() {
    TrackingInputStream is = new TrackingInputStream("hello     \nworld     ".getBytes(StandardCharsets.UTF_8));
    List<String> values = new ArrayList<>();
    FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, Pattern.compile(".*").asPredicate())
        .build()
        .process(is, new HandlerRegistry().on(TenCharRecord.class, r -> values.add(r.getValue())));
    assertTrue(is.closed, "InputStream should be closed after process");
  }

}
