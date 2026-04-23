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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Reads a fixed-format file or stream line by line, routes each line to one or more
 * {@link com.ancientprogramming.fixedformat4j.annotation.Record}-annotated classes via
 * {@link FixedFormatMatchPattern} discriminators, and produces parsed record objects.
 *
 * <p>Records are produced lazily (via {@link #readAsStream}) or eagerly (via
 * {@link #readAsList} / {@link #readAsTypedResult}). All input-source overloads default to
 * {@link java.nio.charset.StandardCharsets#UTF_8}; explicit {@link Charset} overloads are
 * provided for every source type.</p>
 *
 * <p>Quick start — single record type:</p>
 * <pre>{@code
 * FixedFormatReader reader = FixedFormatReader.builder()
 *     .addMapping(MyRecord.class, new RegexFixedFormatMatchPattern(".*"))
 *     .build();
 *
 * List<MyRecord> records = reader.readAsTypedResult(new File("data.txt")).get(MyRecord.class);
 * }</pre>
 *
 * <p>Quick start — heterogeneous file:</p>
 * <pre>{@code
 * FixedFormatReader reader = FixedFormatReader.builder()
 *     .addMapping(HeaderRecord.class, new RegexFixedFormatMatchPattern("^HDR"))
 *     .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"))
 *     .unmatchedLineStrategy(UnmatchedLineStrategy.SKIP)
 *     .build();
 *
 * TypedReadResult result = reader.readAsTypedResult(Path.of("data.txt"));
 * List<HeaderRecord> headers = result.get(HeaderRecord.class);
 * List<DetailRecord> details = result.get(DetailRecord.class);
 * }</pre>
 *
 * <p>Quick start — typed handlers (no casts anywhere):</p>
 * <pre>{@code
 * FixedFormatReader reader = FixedFormatReader.builder()
 *     .addMapping(HeaderRecord.class, new RegexFixedFormatMatchPattern("^HDR"), this::onHeader)
 *     .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"), this::onDetail)
 *     .build();
 *
 * reader.processAll(Path.of("data.txt"));
 * }</pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public class FixedFormatReader {

  private final List<ClassPatternMapping<?>> mappings;
  private final FixedFormatLineProcessor processor;

  FixedFormatReader(FixedFormatReaderBuilder builder) {
    this.mappings = List.copyOf(builder.mappings);
    this.processor = new FixedFormatLineProcessor(
        this.mappings,
        builder.multiMatchStrategy,
        builder.unmatchedLineStrategy,
        builder.parseErrorStrategy,
        builder.lineFilter,
        builder.manager);
  }

  // --- readAsStream ---

  /**
   * Returns a lazy {@link Stream} of records read from {@code reader}.
   *
   * <p>The stream is <em>not</em> parallel. The underlying reader is closed automatically
   * when the stream is closed, so callers should use try-with-resources:</p>
   * <pre>{@code
   * try (Stream<Object> stream = reader.readAsStream(new FileReader("data.txt"))) {
   *     stream.forEach(this::process);
   * }
   * }</pre>
   *
   * <p>For typed record access, prefer {@link #readAsTypedResult} instead.</p>
   *
   * @param reader the source of lines; wrapped in a {@link BufferedReader} if not already one
   * @return a lazily-evaluated, ordered, sequential stream of parsed records
   * @throws FixedFormatIOException if an IO error occurs while reading a line or closing
   *                                the reader
   */
  public Stream<Object> readAsStream(Reader reader) {
    BufferedReader buffered = toBuffered(reader);
    long[] lineCounter = {0L};

    Spliterator<Object> spliterator = new Spliterators.AbstractSpliterator<Object>(Long.MAX_VALUE, Spliterator.ORDERED) {
      @Override
      public boolean tryAdvance(Consumer<? super Object> action) {
        String line;
        try {
          line = buffered.readLine();
        } catch (IOException e) {
          throw new FixedFormatIOException("IO error reading line " + (lineCounter[0] + 1), e);
        }
        if (line == null) {
          return false;
        }
        processor.processLine(line, ++lineCounter[0], (clazz, record) -> action.accept(record));
        return true;
      }
    };

    return StreamSupport.stream(spliterator, false).onClose(() -> {
      try {
        buffered.close();
      } catch (IOException e) {
        throw new FixedFormatIOException("IO error closing reader", e);
      }
    });
  }

  /**
   * Returns a lazy stream of records read from {@code inputStream} using UTF-8 encoding.
   *
   * @param inputStream the source stream
   * @return a lazily-evaluated, ordered, sequential stream of parsed records
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public Stream<Object> readAsStream(InputStream inputStream) {
    return readAsStream(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Returns a lazy stream of records read from {@code inputStream} using the given charset.
   *
   * @param inputStream the source stream
   * @param charset     the character encoding to apply
   * @return a lazily-evaluated, ordered, sequential stream of parsed records
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public Stream<Object> readAsStream(InputStream inputStream, Charset charset) {
    return readAsStream(new InputStreamReader(inputStream, charset));
  }

  /**
   * Returns a lazy stream of records read from {@code file} using UTF-8 encoding.
   *
   * @param file the file to read
   * @return a lazily-evaluated, ordered, sequential stream of parsed records
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public Stream<Object> readAsStream(File file) {
    return readAsStream(file, StandardCharsets.UTF_8);
  }

  /**
   * Returns a lazy stream of records read from {@code file} using the given charset.
   *
   * @param file    the file to read
   * @param charset the character encoding to apply
   * @return a lazily-evaluated, ordered, sequential stream of parsed records
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public Stream<Object> readAsStream(File file, Charset charset) {
    try {
      return readAsStream(new InputStreamReader(new FileInputStream(file), charset));
    } catch (FileNotFoundException e) {
      throw new FixedFormatIOException("File not found: " + file, e);
    }
  }

  /**
   * Returns a lazy stream of records read from {@code path} using UTF-8 encoding.
   *
   * @param path the path to read
   * @return a lazily-evaluated, ordered, sequential stream of parsed records
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public Stream<Object> readAsStream(Path path) {
    return readAsStream(path, StandardCharsets.UTF_8);
  }

  /**
   * Returns a lazy stream of records read from {@code path} using the given charset.
   *
   * @param path    the path to read
   * @param charset the character encoding to apply
   * @return a lazily-evaluated, ordered, sequential stream of parsed records
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public Stream<Object> readAsStream(Path path, Charset charset) {
    try {
      return readAsStream(new InputStreamReader(Files.newInputStream(path), charset));
    } catch (IOException e) {
      throw new FixedFormatIOException("Cannot open path: " + path, e);
    }
  }

  // --- readAsList ---

  /**
   * Eagerly reads all records from {@code reader} and returns them as a list in encounter order.
   *
   * <p>For typed record access, prefer {@link #readAsTypedResult} instead.</p>
   *
   * @param reader the source of lines
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Object> readAsList(Reader reader) {
    try (Stream<Object> stream = readAsStream(reader)) {
      return stream.collect(Collectors.toList());
    }
  }

  /**
   * Eagerly reads all records from {@code inputStream} using UTF-8 encoding.
   *
   * @param inputStream the source stream
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Object> readAsList(InputStream inputStream) {
    return readAsList(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all records from {@code inputStream} using the given charset.
   *
   * @param inputStream the source stream
   * @param charset     the character encoding to apply
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<Object> readAsList(InputStream inputStream, Charset charset) {
    try (Stream<Object> stream = readAsStream(inputStream, charset)) {
      return stream.collect(Collectors.toList());
    }
  }

  /**
   * Eagerly reads all records from {@code file} using UTF-8 encoding.
   *
   * @param file the file to read
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public List<Object> readAsList(File file) {
    return readAsList(file, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all records from {@code file} using the given charset.
   *
   * @param file    the file to read
   * @param charset the character encoding to apply
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public List<Object> readAsList(File file, Charset charset) {
    try (Stream<Object> stream = readAsStream(file, charset)) {
      return stream.collect(Collectors.toList());
    }
  }

  /**
   * Eagerly reads all records from {@code path} using UTF-8 encoding.
   *
   * @param path the path to read
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public List<Object> readAsList(Path path) {
    return readAsList(path, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all records from {@code path} using the given charset.
   *
   * @param path    the path to read
   * @param charset the character encoding to apply
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public List<Object> readAsList(Path path, Charset charset) {
    try (Stream<Object> stream = readAsStream(path, charset)) {
      return stream.collect(Collectors.toList());
    }
  }

  // --- readAsTypedResult ---

  /**
   * Eagerly reads all records from {@code reader} and returns a {@link TypedReadResult} that
   * provides type-safe, class-keyed access without casts.
   *
   * <pre>{@code
   * TypedReadResult result = reader.readAsTypedResult(source);
   * List<HeaderRecord> headers = result.get(HeaderRecord.class); // no cast
   * }</pre>
   *
   * @param reader the source of lines
   * @return a {@link TypedReadResult} grouping records by their matched class; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public TypedReadResult readAsTypedResult(Reader reader) {
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    for (ClassPatternMapping<?> mapping : mappings) {
      data.put(mapping.getRecordClass(), new ArrayList<>());
    }
    List<Object> all = new ArrayList<>();
    readWithCallback(reader, (clazz, record) -> {
      data.get(clazz).add(record);
      all.add(record);
    });
    data.entrySet().removeIf(e -> e.getValue().isEmpty());
    return new TypedReadResult(data, all);
  }

  /**
   * Eagerly reads all records from {@code inputStream} using UTF-8 encoding and returns
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
   * Eagerly reads all records from {@code inputStream} using the given charset and returns
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
   * Eagerly reads all records from {@code file} using UTF-8 encoding and returns
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
   * Eagerly reads all records from {@code file} using the given charset and returns
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
   * Eagerly reads all records from {@code path} using UTF-8 encoding and returns
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
   * Eagerly reads all records from {@code path} using the given charset and returns
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

  // --- readWithCallback ---

  /**
   * Reads all records from {@code reader} and invokes {@code callback} once per record
   * in encounter order.
   *
   * @param reader   the source of lines
   * @param callback invoked with each parsed record; must not be {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void readWithCallback(Reader reader, Consumer<Object> callback) {
    readWithCallback(reader, (clazz, record) -> callback.accept(record));
  }

  /**
   * Reads all records from {@code reader} and invokes {@code callback} once per record,
   * passing both the matched class and the record instance.
   *
   * <p>This overload is the basis for {@link #readAsTypedResult(Reader)} and is the preferred
   * low-level approach when the caller needs to know which class each record belongs to.
   * For typed dispatch without casts, prefer {@link #processAll(Reader)} with per-mapping
   * handlers registered via
   * {@link FixedFormatReaderBuilder#addMapping(Class, FixedFormatMatchPattern, Consumer)}.</p>
   *
   * @param reader   the source of lines
   * @param callback invoked with the matched {@link Class} and the parsed record instance;
   *                 must not be {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void readWithCallback(Reader reader, BiConsumer<Class<?>, Object> callback) {
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

  /**
   * Reads all records from {@code inputStream} using UTF-8 encoding and invokes
   * {@code callback} once per record.
   *
   * @param inputStream the source stream
   * @param callback    invoked with each parsed record
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void readWithCallback(InputStream inputStream, Consumer<Object> callback) {
    readWithCallback(inputStream, StandardCharsets.UTF_8, callback);
  }

  /**
   * Reads all records from {@code inputStream} using the given charset and invokes
   * {@code callback} once per record.
   *
   * @param inputStream the source stream
   * @param charset     the character encoding to apply
   * @param callback    invoked with each parsed record
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void readWithCallback(InputStream inputStream, Charset charset, Consumer<Object> callback) {
    readWithCallback(new InputStreamReader(inputStream, charset), callback);
  }

  /**
   * Reads all records from {@code file} using UTF-8 encoding and invokes
   * {@code callback} once per record.
   *
   * @param file     the file to read
   * @param callback invoked with each parsed record
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public void readWithCallback(File file, Consumer<Object> callback) {
    readWithCallback(file, StandardCharsets.UTF_8, callback);
  }

  /**
   * Reads all records from {@code file} using the given charset and invokes
   * {@code callback} once per record.
   *
   * @param file     the file to read
   * @param charset  the character encoding to apply
   * @param callback invoked with each parsed record
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public void readWithCallback(File file, Charset charset, Consumer<Object> callback) {
    try (Stream<Object> stream = readAsStream(file, charset)) {
      stream.forEach(callback);
    }
  }

  /**
   * Reads all records from {@code path} using UTF-8 encoding and invokes
   * {@code callback} once per record.
   *
   * @param path     the path to read
   * @param callback invoked with each parsed record
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public void readWithCallback(Path path, Consumer<Object> callback) {
    readWithCallback(path, StandardCharsets.UTF_8, callback);
  }

  /**
   * Reads all records from {@code path} using the given charset and invokes
   * {@code callback} once per record.
   *
   * @param path     the path to read
   * @param charset  the character encoding to apply
   * @param callback invoked with each parsed record
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public void readWithCallback(Path path, Charset charset, Consumer<Object> callback) {
    try (Stream<Object> stream = readAsStream(path, charset)) {
      stream.forEach(callback);
    }
  }

  /**
   * Reads all records from {@code inputStream} using UTF-8 encoding and invokes
   * {@code callback} once per record, passing both the matched class and the record instance.
   *
   * @param inputStream the source stream
   * @param callback    invoked with the matched {@link Class} and the parsed record instance
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void readWithCallback(InputStream inputStream, BiConsumer<Class<?>, Object> callback) {
    readWithCallback(inputStream, StandardCharsets.UTF_8, callback);
  }

  /**
   * Reads all records from {@code inputStream} using the given charset and invokes
   * {@code callback} once per record, passing both the matched class and the record instance.
   *
   * @param inputStream the source stream
   * @param charset     the character encoding to apply
   * @param callback    invoked with the matched {@link Class} and the parsed record instance
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void readWithCallback(InputStream inputStream, Charset charset,
      BiConsumer<Class<?>, Object> callback) {
    readWithCallback(new InputStreamReader(inputStream, charset), callback);
  }

  /**
   * Reads all records from {@code file} using UTF-8 encoding and invokes {@code callback}
   * once per record, passing both the matched class and the record instance.
   *
   * @param file     the file to read
   * @param callback invoked with the matched {@link Class} and the parsed record instance
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public void readWithCallback(File file, BiConsumer<Class<?>, Object> callback) {
    readWithCallback(file, StandardCharsets.UTF_8, callback);
  }

  /**
   * Reads all records from {@code file} using the given charset and invokes {@code callback}
   * once per record, passing both the matched class and the record instance.
   *
   * @param file     the file to read
   * @param charset  the character encoding to apply
   * @param callback invoked with the matched {@link Class} and the parsed record instance
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public void readWithCallback(File file, Charset charset,
      BiConsumer<Class<?>, Object> callback) {
    try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), charset)) {
      readWithCallback(r, callback);
    } catch (FileNotFoundException e) {
      throw new FixedFormatIOException("File not found: " + file, e);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading file: " + file, e);
    }
  }

  /**
   * Reads all records from {@code path} using UTF-8 encoding and invokes {@code callback}
   * once per record, passing both the matched class and the record instance.
   *
   * @param path     the path to read
   * @param callback invoked with the matched {@link Class} and the parsed record instance
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public void readWithCallback(Path path, BiConsumer<Class<?>, Object> callback) {
    readWithCallback(path, StandardCharsets.UTF_8, callback);
  }

  /**
   * Reads all records from {@code path} using the given charset and invokes {@code callback}
   * once per record, passing both the matched class and the record instance.
   *
   * @param path     the path to read
   * @param charset  the character encoding to apply
   * @param callback invoked with the matched {@link Class} and the parsed record instance
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public void readWithCallback(Path path, Charset charset,
      BiConsumer<Class<?>, Object> callback) {
    try (InputStreamReader r = new InputStreamReader(Files.newInputStream(path), charset)) {
      readWithCallback(r, callback);
    } catch (IOException e) {
      throw new FixedFormatIOException("Cannot open path: " + path, e);
    }
  }

  // --- processAll ---

  /**
   * Reads all records from {@code reader} and dispatches each to the typed handler registered
   * for its class via
   * {@link FixedFormatReaderBuilder#addMapping(Class, FixedFormatMatchPattern, Consumer)}.
   *
   * <p>Mappings added without a handler are silently skipped. This is the preferred method
   * when type-safe dispatch without casts is desired:</p>
   * <pre>{@code
   * FixedFormatReader reader = FixedFormatReader.builder()
   *     .addMapping(HeaderRecord.class, hdrPattern, this::onHeader)
   *     .addMapping(DetailRecord.class, dtlPattern, this::onDetail)
   *     .build();
   *
   * reader.processAll(source); // handlers receive HeaderRecord / DetailRecord directly
   * }</pre>
   *
   * @param reader the source of lines
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void processAll(Reader reader) {
    readWithCallback(reader, processor::fireHandler);
  }

  /**
   * Reads all records from {@code inputStream} using UTF-8 encoding and dispatches each to
   * its registered typed handler.
   *
   * @param inputStream the source stream
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void processAll(InputStream inputStream) {
    processAll(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Reads all records from {@code inputStream} using the given charset and dispatches each to
   * its registered typed handler.
   *
   * @param inputStream the source stream
   * @param charset     the character encoding to apply
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void processAll(InputStream inputStream, Charset charset) {
    readWithCallback(inputStream, charset, processor::fireHandler);
  }

  /**
   * Reads all records from {@code file} using UTF-8 encoding and dispatches each to
   * its registered typed handler.
   *
   * @param file the file to read
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public void processAll(File file) {
    processAll(file, StandardCharsets.UTF_8);
  }

  /**
   * Reads all records from {@code file} using the given charset and dispatches each to
   * its registered typed handler.
   *
   * @param file    the file to read
   * @param charset the character encoding to apply
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public void processAll(File file, Charset charset) {
    readWithCallback(file, charset, processor::fireHandler);
  }

  /**
   * Reads all records from {@code path} using UTF-8 encoding and dispatches each to
   * its registered typed handler.
   *
   * @param path the path to read
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public void processAll(Path path) {
    processAll(path, StandardCharsets.UTF_8);
  }

  /**
   * Reads all records from {@code path} using the given charset and dispatches each to
   * its registered typed handler.
   *
   * @param path    the path to read
   * @param charset the character encoding to apply
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public void processAll(Path path, Charset charset) {
    readWithCallback(path, charset, processor::fireHandler);
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
