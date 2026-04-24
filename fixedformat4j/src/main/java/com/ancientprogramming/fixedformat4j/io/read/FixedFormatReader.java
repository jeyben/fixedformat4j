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
package com.ancientprogramming.fixedformat4j.io.read;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatIOException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Reads a fixed-format file or stream line by line, routes each line to one or more
 * {@link com.ancientprogramming.fixedformat4j.annotation.Record}-annotated classes via
 * {@link LinePattern} discriminators, and produces parsed record objects.
 *
 * <p>Each physical line is treated as exactly one record (or unmatched). If a file contains
 * multiple records packed within a single line, split the line before passing it to this reader.</p>
 *
 * <p>Records are collected eagerly via {@link #readAsResult} or dispatched via
 * {@link #processAll}. All input-source overloads default to
 * {@link java.nio.charset.StandardCharsets#UTF_8}; explicit {@link Charset} overloads are
 * provided for every source type.</p>
 *
 * <p>Quick start — single record type:</p>
 * <pre>{@code
 * FixedFormatReader reader = FixedFormatReader.builder()
 *     .addMapping(MyRecord.class, new RegexLinePattern(".*"))
 *     .build();
 *
 * List<MyRecord> records = reader.readAsResult(new File("data.txt")).get(MyRecord.class);
 * }</pre>
 *
 * <p>Quick start — heterogeneous file:</p>
 * <pre>{@code
 * FixedFormatReader reader = FixedFormatReader.builder()
 *     .addMapping(HeaderRecord.class, new RegexLinePattern("^HDR"))
 *     .addMapping(DetailRecord.class, new RegexLinePattern("^DTL"))
 *     .unmatchStrategy(UnmatchStrategy.skip())
 *     .build();
 *
 * ReadResult result = reader.readAsResult(Path.of("data.txt"));
 * List<HeaderRecord> headers = result.get(HeaderRecord.class);
 * List<DetailRecord> details = result.get(DetailRecord.class);
 * }</pre>
 *
 * <p>Quick start — typed handlers (no casts anywhere):</p>
 * <pre>{@code
 * FixedFormatReader reader = FixedFormatReader.builder()
 *     .addMapping(HeaderRecord.class, new RegexLinePattern("^HDR"))
 *     .addMapping(DetailRecord.class, new RegexLinePattern("^DTL"))
 *     .build();
 *
 * reader.process(Path.of("data.txt"), new HandlerRegistry()
 *     .on(HeaderRecord.class, this::onHeader)
 *     .on(DetailRecord.class, this::onDetail));
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public class FixedFormatReader {

  private final List<RecordMapping<?>> mappings;
  private final FixedFormatLineProcessor processor;

  FixedFormatReader(FixedFormatReaderBuilder builder) {
    this.mappings = List.copyOf(builder.mappings);
    this.processor = new FixedFormatLineProcessor(
        this.mappings,
        builder.multiMatchStrategy,
        builder.unmatchStrategy,
        builder.parseErrorStrategy,
        builder.lineFilter,
        builder.manager);
  }

  // --- readAsResult ---

  /**
   * Eagerly reads all records from {@code reader} and returns a {@link ReadResult} that
   * provides type-safe, class-keyed access without casts.
   *
   * <pre>{@code
   * ReadResult result = reader.readAsResult(source);
   * List<HeaderRecord> headers = result.get(HeaderRecord.class); // no cast
   * }</pre>
   *
   * @param reader the source of lines; closed when this method returns
   * @return a {@link ReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public ReadResult readAsResult(Reader reader) {
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    for (RecordMapping<?> mapping : mappings) {
      data.put(mapping.getRecordClass(), new ArrayList<>());
    }
    List<Object> all = new ArrayList<>();
    readWithMappingCallback(reader, (mapping, record) -> {
      data.get(mapping.getRecordClass()).add(record);
      all.add(record);
    });
    data.entrySet().removeIf(e -> e.getValue().isEmpty());
    return new ReadResult(data, all);
  }

  /**
   * Eagerly reads all records from {@code inputStream} using UTF-8 encoding and returns
   * a {@link ReadResult}.
   *
   * @param inputStream the source stream; closed when this method returns
   * @return a {@link ReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public ReadResult readAsResult(InputStream inputStream) {
    return readAsResult(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all records from {@code inputStream} using the given charset and returns
   * a {@link ReadResult}.
   *
   * @param inputStream the source stream; closed when this method returns
   * @param charset     the character encoding to apply
   * @return a {@link ReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public ReadResult readAsResult(InputStream inputStream, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(inputStream, charset)) {
      return readAsResult(r);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading input stream", e);
    }
  }

  /**
   * Eagerly reads all records from {@code file} using UTF-8 encoding and returns
   * a {@link ReadResult}.
   *
   * @param file the file to read
   * @return a {@link ReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public ReadResult readAsResult(File file) {
    return readAsResult(file, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all records from {@code file} using the given charset and returns
   * a {@link ReadResult}.
   *
   * @param file    the file to read
   * @param charset the character encoding to apply
   * @return a {@link ReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public ReadResult readAsResult(File file, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), charset)) {
      return readAsResult(r);
    } catch (FileNotFoundException e) {
      throw new FixedFormatIOException("File not found: " + file, e);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading file: " + file, e);
    }
  }

  /**
   * Eagerly reads all records from {@code path} using UTF-8 encoding and returns
   * a {@link ReadResult}.
   *
   * @param path the path to read
   * @return a {@link ReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public ReadResult readAsResult(Path path) {
    return readAsResult(path, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all records from {@code path} using the given charset and returns
   * a {@link ReadResult}.
   *
   * @param path    the path to read
   * @param charset the character encoding to apply
   * @return a {@link ReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public ReadResult readAsResult(Path path, Charset charset) {
    try (InputStreamReader r = new InputStreamReader(Files.newInputStream(path), charset)) {
      return readAsResult(r);
    } catch (IOException e) {
      throw new FixedFormatIOException("Cannot open path: " + path, e);
    }
  }

  private void readWithMappingCallback(Reader reader,
                                       BiConsumer<RecordMapping<?>, Object> callback) {
    BufferedReader buffered = toBuffered(reader);
    long[] lineCounter = {0L};
    try (buffered) {
      String line;
      while ((line = buffered.readLine()) != null) {
        processor.processLine(line, ++lineCounter[0], callback);
      }
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading line " + (lineCounter[0] + 1), e);
    }
  }

  // --- process ---

  /**
   * Reads all records from {@code reader} and dispatches each to the handler registered
   * for its class in {@code registry}.
   *
   * <p>Classes not present in the registry are silently ignored — they are still parsed,
   * but no handler is invoked. Because the registry is supplied per call rather than stored
   * in the reader, the same {@link FixedFormatReader} instance is safe to use from multiple
   * threads with independent registries.</p>
   *
   * <pre>{@code
   * reader.process(source, new HandlerRegistry()
   *     .on(HeaderRecord.class, this::onHeader)
   *     .on(DetailRecord.class, this::onDetail));
   * }</pre>
   *
   * @param reader   the source of lines; closed when this method returns
   * @param registry the typed handlers to invoke per matched class; must not be {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void process(Reader reader, HandlerRegistry registry) {
    if (registry == null) throw new IllegalArgumentException("registry must not be null");
    readWithMappingCallback(reader,
        (mapping, record) -> registry.dispatch(mapping.getRecordClass(), record));
  }

  /**
   * Reads all records from {@code inputStream} using UTF-8 encoding and dispatches each to
   * its handler in {@code registry}.
   *
   * @param inputStream the source stream; closed when this method returns
   * @param registry    the typed handlers to invoke per matched class; must not be {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void process(InputStream inputStream, HandlerRegistry registry) {
    process(inputStream, StandardCharsets.UTF_8, registry);
  }

  /**
   * Reads all records from {@code inputStream} using the given charset and dispatches each to
   * its handler in {@code registry}.
   *
   * @param inputStream the source stream; closed when this method returns
   * @param charset     the character encoding to apply
   * @param registry    the typed handlers to invoke per matched class; must not be {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void process(InputStream inputStream, Charset charset, HandlerRegistry registry) {
    try (InputStreamReader r = new InputStreamReader(inputStream, charset)) {
      process(r, registry);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading input stream", e);
    }
  }

  /**
   * Reads all records from {@code file} using UTF-8 encoding and dispatches each to
   * its handler in {@code registry}.
   *
   * @param file     the file to read
   * @param registry the typed handlers to invoke per matched class; must not be {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public void process(File file, HandlerRegistry registry) {
    process(file, StandardCharsets.UTF_8, registry);
  }

  /**
   * Reads all records from {@code file} using the given charset and dispatches each to
   * its handler in {@code registry}.
   *
   * @param file     the file to read
   * @param charset  the character encoding to apply
   * @param registry the typed handlers to invoke per matched class; must not be {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public void process(File file, Charset charset, HandlerRegistry registry) {
    try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), charset)) {
      process(r, registry);
    } catch (FileNotFoundException e) {
      throw new FixedFormatIOException("File not found: " + file, e);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading file: " + file, e);
    }
  }

  /**
   * Reads all records from {@code path} using UTF-8 encoding and dispatches each to
   * its handler in {@code registry}.
   *
   * @param path     the path to read
   * @param registry the typed handlers to invoke per matched class; must not be {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public void process(Path path, HandlerRegistry registry) {
    process(path, StandardCharsets.UTF_8, registry);
  }

  /**
   * Reads all records from {@code path} using the given charset and dispatches each to
   * its handler in {@code registry}.
   *
   * @param path     the path to read
   * @param charset  the character encoding to apply
   * @param registry the typed handlers to invoke per matched class; must not be {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public void process(Path path, Charset charset, HandlerRegistry registry) {
    try (InputStreamReader r = new InputStreamReader(Files.newInputStream(path), charset)) {
      process(r, registry);
    } catch (IOException e) {
      throw new FixedFormatIOException("Cannot open path: " + path, e);
    }
  }

  // --- factory ---

  /**
   * Returns a new builder for constructing a {@link FixedFormatReader}.
   *
   * @return a fresh builder instance
   */
  public static FixedFormatReaderBuilder builder() {
    return new FixedFormatReaderBuilder();
  }

  private static BufferedReader toBuffered(Reader reader) {
    return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
  }
}
