/*
 * Copyright 2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatIOException;
import com.ancientprogramming.fixedformat4j.io.strategy.PartialChunkStrategy;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Reads fixed-format files where multiple records are packed end-to-end within a single physical
 * line. Each record occupies exactly {@code recordWidth} characters; the last record on a line may
 * be shorter (handled by a configurable {@link PartialChunkStrategy}).
 *
 * <p>Quick start:</p>
 * <pre>{@code
 * PackedRecordReader reader = PackedRecordReader.builder()
 *     .recordWidth(128)
 *     .addMapping(HeaderRecord.class, new RegexFixedFormatMatchPattern("^HDR"))
 *     .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"))
 *     .partialChunkStrategy(PartialChunkStrategy.pad())
 *     .build();
 *
 * TypedReadResult result = reader.readAsTypedResult(new File("data.txt"));
 * List<HeaderRecord> headers = result.get(HeaderRecord.class);
 * List<DetailRecord> details = result.get(DetailRecord.class);
 * }</pre>
 *
 * <p>For read-edit-write round trips that preserve all chunks and unmatched content,
 * use {@link PackedRecordRowReader} instead.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 * @see PackedRecordReaderBuilder
 * @see PackedRecordRowReader
 * @see PartialChunkStrategy
 */
public class PackedRecordReader {

  private final int recordWidth;
  private final FixedFormatLineProcessor processor;
  private final PartialChunkStrategy partialChunkStrategy;
  private final Predicate<String> lineFilter;

  PackedRecordReader(PackedRecordReaderBuilder builder) {
    this.recordWidth = builder.recordWidth;
    this.partialChunkStrategy = builder.partialChunkStrategy;
    this.lineFilter = builder.lineFilter;
    this.processor = new FixedFormatLineProcessor(
        List.copyOf(builder.mappings),
        builder.multiMatchStrategy,
        builder.unmatchStrategy,
        builder.parseErrorStrategy,
        line -> true,  // line filter applied in sliceLine; processor sees full chunks
        builder.manager);
  }

  // --- readAsList ---

  /**
   * Eagerly reads all matched chunks from {@code reader} and returns them as a list in
   * encounter order.
   *
   * @param reader the source of lines
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Object> readAsList(Reader reader) {
    List<Object> results = new ArrayList<>();
    readWithCallback(reader, (clazz, record) -> results.add(record));
    return results;
  }

  /**
   * Eagerly reads all matched chunks from {@code inputStream} using UTF-8 encoding.
   *
   * @param inputStream the source stream
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Object> readAsList(InputStream inputStream) {
    return readAsList(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all matched chunks from {@code inputStream} using the given charset.
   *
   * @param inputStream the source stream
   * @param charset     the character encoding to apply
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Object> readAsList(InputStream inputStream, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(inputStream, charset)) {
      return readAsList(r);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading input stream", e);
    }
  }

  /**
   * Eagerly reads all matched chunks from {@code file} using UTF-8 encoding.
   *
   * @param file the file to read
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public List<Object> readAsList(File file) {
    return readAsList(file, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all matched chunks from {@code file} using the given charset.
   *
   * @param file    the file to read
   * @param charset the character encoding to apply
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public List<Object> readAsList(File file, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), charset)) {
      return readAsList(r);
    } catch (FileNotFoundException e) {
      throw new FixedFormatIOException("File not found: " + file, e);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading file: " + file, e);
    }
  }

  /**
   * Eagerly reads all matched chunks from {@code path} using UTF-8 encoding.
   *
   * @param path the path to read
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public List<Object> readAsList(Path path) {
    return readAsList(path, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all matched chunks from {@code path} using the given charset.
   *
   * @param path    the path to read
   * @param charset the character encoding to apply
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public List<Object> readAsList(Path path, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(Files.newInputStream(path), charset)) {
      return readAsList(r);
    } catch (IOException e) {
      throw new FixedFormatIOException("Cannot open path: " + path, e);
    }
  }

  // --- readAsStream ---

  /**
   * Returns a stream of all matched chunks from {@code reader}, in encounter order.
   * The underlying reader is consumed eagerly.
   *
   * @param reader the source of lines
   * @return an ordered stream of all parsed records; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public Stream<Object> readAsStream(Reader reader) {
    return readAsList(reader).stream();
  }

  /**
   * Returns a stream of all matched chunks from {@code inputStream} using UTF-8 encoding.
   *
   * @param inputStream the source stream
   * @return an ordered stream of all parsed records; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public Stream<Object> readAsStream(InputStream inputStream) {
    return readAsStream(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Returns a stream of all matched chunks from {@code inputStream} using the given charset.
   *
   * @param inputStream the source stream
   * @param charset     the character encoding to apply
   * @return an ordered stream of all parsed records; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public Stream<Object> readAsStream(InputStream inputStream, Charset charset) {
    return readAsList(inputStream, charset).stream();
  }

  /**
   * Returns a stream of all matched chunks from {@code file} using UTF-8 encoding.
   *
   * @param file the file to read
   * @return an ordered stream of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public Stream<Object> readAsStream(File file) {
    return readAsStream(file, StandardCharsets.UTF_8);
  }

  /**
   * Returns a stream of all matched chunks from {@code file} using the given charset.
   *
   * @param file    the file to read
   * @param charset the character encoding to apply
   * @return an ordered stream of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public Stream<Object> readAsStream(File file, Charset charset) {
    return readAsList(file, charset).stream();
  }

  /**
   * Returns a stream of all matched chunks from {@code path} using UTF-8 encoding.
   *
   * @param path the path to read
   * @return an ordered stream of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public Stream<Object> readAsStream(Path path) {
    return readAsStream(path, StandardCharsets.UTF_8);
  }

  /**
   * Returns a stream of all matched chunks from {@code path} using the given charset.
   *
   * @param path    the path to read
   * @param charset the character encoding to apply
   * @return an ordered stream of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public Stream<Object> readAsStream(Path path, Charset charset) {
    return readAsList(path, charset).stream();
  }

  // --- readAsTypedResult ---

  /**
   * Eagerly reads all matched chunks from {@code reader} and returns a {@link TypedReadResult}
   * that provides type-safe, class-keyed access without casts.
   *
   * @param reader the source of lines
   * @return a {@link TypedReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public TypedReadResult readAsTypedResult(Reader reader) {
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    List<Object> all = new ArrayList<>();
    readWithCallback(reader, (clazz, record) -> {
      data.computeIfAbsent(clazz, k -> new ArrayList<>()).add(record);
      all.add(record);
    });
    return new TypedReadResult(data, all);
  }

  /**
   * Eagerly reads all matched chunks from {@code inputStream} using UTF-8 encoding and returns
   * a {@link TypedReadResult}.
   *
   * @param inputStream the source stream
   * @return a {@link TypedReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public TypedReadResult readAsTypedResult(InputStream inputStream) {
    return readAsTypedResult(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all matched chunks from {@code inputStream} using the given charset and returns
   * a {@link TypedReadResult}.
   *
   * @param inputStream the source stream
   * @param charset     the character encoding to apply
   * @return a {@link TypedReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public TypedReadResult readAsTypedResult(InputStream inputStream, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(inputStream, charset)) {
      return readAsTypedResult(r);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading input stream", e);
    }
  }

  /**
   * Eagerly reads all matched chunks from {@code file} using UTF-8 encoding and returns
   * a {@link TypedReadResult}.
   *
   * @param file the file to read
   * @return a {@link TypedReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public TypedReadResult readAsTypedResult(File file) {
    return readAsTypedResult(file, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all matched chunks from {@code file} using the given charset and returns
   * a {@link TypedReadResult}.
   *
   * @param file    the file to read
   * @param charset the character encoding to apply
   * @return a {@link TypedReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public TypedReadResult readAsTypedResult(File file, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), charset)) {
      return readAsTypedResult(r);
    } catch (FileNotFoundException e) {
      throw new FixedFormatIOException("File not found: " + file, e);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading file: " + file, e);
    }
  }

  /**
   * Eagerly reads all matched chunks from {@code path} using UTF-8 encoding and returns
   * a {@link TypedReadResult}.
   *
   * @param path the path to read
   * @return a {@link TypedReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public TypedReadResult readAsTypedResult(Path path) {
    return readAsTypedResult(path, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all matched chunks from {@code path} using the given charset and returns
   * a {@link TypedReadResult}.
   *
   * @param path    the path to read
   * @param charset the character encoding to apply
   * @return a {@link TypedReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public TypedReadResult readAsTypedResult(Path path, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(Files.newInputStream(path), charset)) {
      return readAsTypedResult(r);
    } catch (IOException e) {
      throw new FixedFormatIOException("Cannot open path: " + path, e);
    }
  }

  /**
   * Returns a new builder for constructing a {@link PackedRecordReader}.
   *
   * @return a fresh builder instance
   */
  public static PackedRecordReaderBuilder builder() {
    return new PackedRecordReaderBuilder();
  }

  private void readWithCallback(Reader reader, BiConsumer<Class<?>, Object> emit) {
    BufferedReader buffered = toBuffered(reader);
    long[] lineCounter = {0L};
    try (buffered) {
      String line;
      while ((line = buffered.readLine()) != null) {
        sliceLine(line, ++lineCounter[0], emit);
      }
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading line " + (lineCounter[0] + 1), e);
    }
  }

  private void sliceLine(String line, long lineNumber, BiConsumer<Class<?>, Object> emit) {
    if (!lineFilter.test(line)) {
      return;
    }
    int offset = 0;
    int chunkIndex = 0;
    while (offset < line.length()) {
      chunkIndex++;
      int remaining = line.length() - offset;
      if (remaining >= recordWidth) {
        String chunk = line.substring(offset, offset + recordWidth);
        offset += recordWidth;
        processor.processLine(chunk, lineNumber, emit);
      } else {
        String partial = line.substring(offset);
        offset = line.length();
        Optional<String> resolved = partialChunkStrategy.resolve(lineNumber, chunkIndex, partial, recordWidth);
        resolved.ifPresent(padded -> processor.processLine(padded, lineNumber, emit));
      }
    }
  }

  private static BufferedReader toBuffered(Reader reader) {
    return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
  }
}
