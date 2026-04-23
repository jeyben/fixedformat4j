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

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatIOException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;

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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Reads a fixed-format file or stream line by line, routes each line to one or more
 * {@link com.ancientprogramming.fixedformat4j.annotation.Record}-annotated classes via
 * {@link FixedFormatMatchPattern} discriminators, and produces typed record objects.
 *
 * <p>Records are produced lazily (via {@link #readAsStream}) or eagerly (via
 * {@link #readAsList} / {@link #readAsMap}). All input-source overloads default to
 * {@link java.nio.charset.StandardCharsets#UTF_8}; explicit {@link Charset} overloads are
 * provided for every source type.</p>
 *
 * <p>Quick start — single record type:</p>
 * <pre>{@code
 * FixedFormatReader<MyRecord> reader = FixedFormatReader.<MyRecord>builder()
 *     .addMapping(MyRecord.class, new RegexFixedFormatMatchPattern(".*"))
 *     .build();
 *
 * List<MyRecord> records = reader.readAsList(new File("data.txt"));
 * }</pre>
 *
 * <p>Quick start — heterogeneous file ({@code T=Object}):</p>
 * <pre>{@code
 * FixedFormatReader<Object> reader = FixedFormatReader.<Object>builder()
 *     .addMapping(HeaderRecord.class, new RegexFixedFormatMatchPattern("^HDR"))
 *     .addMapping(DetailRecord.class, new RegexFixedFormatMatchPattern("^DTL"))
 *     .unmatchedLineStrategy(UnmatchedLineStrategy.SKIP)
 *     .build();
 *
 * Map<Class<?>, List<Object>> byType = reader.readAsMap(Path.of("data.txt"));
 * List<Object> headers = byType.get(HeaderRecord.class);
 * }</pre>
 *
 * @param <T> the common supertype of all record classes registered with this reader;
 *            use {@code Object} when no meaningful supertype exists
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public class FixedFormatReader<T> {

  private final List<ClassPatternMapping<? extends T>> mappings;
  private final MultiMatchStrategy multiMatchStrategy;
  private final UnmatchedLineStrategy unmatchedLineStrategy;
  private final ParseErrorStrategy parseErrorStrategy;
  private final Predicate<String> lineFilter;
  private final FixedFormatManager manager;

  private FixedFormatReader(Builder<T> builder) {
    this.mappings = List.copyOf(builder.mappings);
    this.multiMatchStrategy = builder.multiMatchStrategy;
    this.unmatchedLineStrategy = builder.unmatchedLineStrategy;
    this.parseErrorStrategy = builder.parseErrorStrategy;
    this.lineFilter = builder.lineFilter;
    this.manager = builder.manager;
  }

  // --- readAsStream ---

  /**
   * Returns a lazy {@link Stream} of records read from {@code reader}.
   *
   * <p>The stream is <em>not</em> parallel. The underlying reader is closed automatically
   * when the stream is closed, so callers should use try-with-resources:</p>
   * <pre>{@code
   * try (Stream<MyRecord> stream = reader.readAsStream(new FileReader("data.txt"))) {
   *     stream.forEach(this::process);
   * }
   * }</pre>
   *
   * @param reader the source of lines; wrapped in a {@link BufferedReader} if not already one
   * @return a lazily-evaluated, ordered, sequential stream of parsed records
   * @throws FixedFormatIOException if an IO error occurs while reading a line or closing
   *                                the reader
   */
  public Stream<T> readAsStream(Reader reader) {
    BufferedReader buffered = toBuffered(reader);
    long[] lineCounter = {0L};

    Spliterator<T> spliterator = new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
        String line;
        try {
          line = buffered.readLine();
        } catch (IOException e) {
          throw new FixedFormatIOException("IO error reading line " + (lineCounter[0] + 1), e);
        }
        if (line == null) {
          return false;
        }
        processLine(line, ++lineCounter[0], (clazz, record) -> action.accept(record));
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
  public Stream<T> readAsStream(InputStream inputStream) {
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
  public Stream<T> readAsStream(InputStream inputStream, Charset charset) {
    return readAsStream(new InputStreamReader(inputStream, charset));
  }

  /**
   * Returns a lazy stream of records read from {@code file} using UTF-8 encoding.
   *
   * @param file the file to read
   * @return a lazily-evaluated, ordered, sequential stream of parsed records
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public Stream<T> readAsStream(File file) {
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
  public Stream<T> readAsStream(File file, Charset charset) {
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
  public Stream<T> readAsStream(Path path) {
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
  public Stream<T> readAsStream(Path path, Charset charset) {
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
   * @param reader the source of lines
   * @return an ordered list of all parsed records; never {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public List<T> readAsList(Reader reader) {
    try (Stream<T> stream = readAsStream(reader)) {
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
  public List<T> readAsList(InputStream inputStream) {
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
  public List<T> readAsList(InputStream inputStream, Charset charset) {
    try (Stream<T> stream = readAsStream(inputStream, charset)) {
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
  public List<T> readAsList(File file) {
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
  public List<T> readAsList(File file, Charset charset) {
    try (Stream<T> stream = readAsStream(file, charset)) {
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
  public List<T> readAsList(Path path) {
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
  public List<T> readAsList(Path path, Charset charset) {
    try (Stream<T> stream = readAsStream(path, charset)) {
      return stream.collect(Collectors.toList());
    }
  }

  // --- readAsMap ---

  /**
   * Eagerly reads all records from {@code reader} and groups them by their matched class.
   *
   * <p>The returned map is a {@link java.util.LinkedHashMap} whose key order follows
   * the registration order of {@link Builder#addMapping} calls. Classes with no matching
   * lines are excluded from the map.</p>
   *
   * @param reader the source of lines
   * @return a map from record class to all records of that class; never {@code null};
   *         excludes classes that produced no records
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public Map<Class<? extends T>, List<T>> readAsMap(Reader reader) {
    Map<Class<? extends T>, List<T>> result = new LinkedHashMap<>();
    for (ClassPatternMapping<? extends T> mapping : mappings) {
      result.put(mapping.getRecordClass(), new ArrayList<>());
    }
    readWithCallback(reader, (clazz, record) -> result.get(clazz).add(record));
    result.entrySet().removeIf(e -> e.getValue().isEmpty());
    return result;
  }

  /**
   * Eagerly reads all records from {@code inputStream} using UTF-8 encoding and groups
   * them by their matched class.
   *
   * @param inputStream the source stream
   * @return a map from record class to all records of that class; excludes empty classes
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public Map<Class<? extends T>, List<T>> readAsMap(InputStream inputStream) {
    return readAsMap(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all records from {@code inputStream} using the given charset and groups
   * them by their matched class.
   *
   * @param inputStream the source stream
   * @param charset     the character encoding to apply
   * @return a map from record class to all records of that class; excludes empty classes
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public Map<Class<? extends T>, List<T>> readAsMap(InputStream inputStream, Charset charset) {
    try (InputStreamReader reader = new InputStreamReader(inputStream, charset)) {
      return readAsMap(reader);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading input stream", e);
    }
  }

  /**
   * Eagerly reads all records from {@code file} using UTF-8 encoding and groups
   * them by their matched class.
   *
   * @param file the file to read
   * @return a map from record class to all records of that class; excludes empty classes
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public Map<Class<? extends T>, List<T>> readAsMap(File file) {
    return readAsMap(file, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all records from {@code file} using the given charset and groups
   * them by their matched class.
   *
   * @param file    the file to read
   * @param charset the character encoding to apply
   * @return a map from record class to all records of that class; excludes empty classes
   * @throws FixedFormatIOException if the file is not found or an IO error occurs
   */
  public Map<Class<? extends T>, List<T>> readAsMap(File file, Charset charset) {
    try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), charset)) {
      return readAsMap(reader);
    } catch (FileNotFoundException e) {
      throw new FixedFormatIOException("File not found: " + file, e);
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading file: " + file, e);
    }
  }

  /**
   * Eagerly reads all records from {@code path} using UTF-8 encoding and groups
   * them by their matched class.
   *
   * @param path the path to read
   * @return a map from record class to all records of that class; excludes empty classes
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public Map<Class<? extends T>, List<T>> readAsMap(Path path) {
    return readAsMap(path, StandardCharsets.UTF_8);
  }

  /**
   * Eagerly reads all records from {@code path} using the given charset and groups
   * them by their matched class.
   *
   * @param path    the path to read
   * @param charset the character encoding to apply
   * @return a map from record class to all records of that class; excludes empty classes
   * @throws FixedFormatIOException if the path cannot be opened or an IO error occurs
   */
  public Map<Class<? extends T>, List<T>> readAsMap(Path path, Charset charset) {
    try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), charset)) {
      return readAsMap(reader);
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
  public void readWithCallback(Reader reader, Consumer<T> callback) {
    readWithCallback(reader, (clazz, record) -> callback.accept(record));
  }

  /**
   * Reads all records from {@code reader} and invokes {@code callback} once per record,
   * passing both the matched class and the record instance.
   *
   * <p>This overload is the basis for {@link #readAsMap(Reader)} and is the preferred
   * approach when the caller needs to know which class each record belongs to.</p>
   *
   * @param reader   the source of lines
   * @param callback invoked with the matched {@link Class} and the parsed record instance;
   *                 must not be {@code null}
   * @throws FixedFormatIOException if an IO error occurs while reading
   */
  public void readWithCallback(Reader reader, BiConsumer<Class<? extends T>, T> callback) {
    BufferedReader buffered = toBuffered(reader);
    long[] lineCounter = {0L};

    try (buffered) {
      String line;
      while ((line = buffered.readLine()) != null) {
        processLine(line, ++lineCounter[0], callback);
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
  public void readWithCallback(InputStream inputStream, Consumer<T> callback) {
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
  public void readWithCallback(InputStream inputStream, Charset charset, Consumer<T> callback) {
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
  public void readWithCallback(File file, Consumer<T> callback) {
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
  public void readWithCallback(File file, Charset charset, Consumer<T> callback) {
    try (Stream<T> stream = readAsStream(file, charset)) {
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
  public void readWithCallback(Path path, Consumer<T> callback) {
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
  public void readWithCallback(Path path, Charset charset, Consumer<T> callback) {
    try (Stream<T> stream = readAsStream(path, charset)) {
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
  public void readWithCallback(InputStream inputStream, BiConsumer<Class<? extends T>, T> callback) {
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
      BiConsumer<Class<? extends T>, T> callback) {
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
  public void readWithCallback(File file, BiConsumer<Class<? extends T>, T> callback) {
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
      BiConsumer<Class<? extends T>, T> callback) {
    try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), charset)) {
      readWithCallback(reader, callback);
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
  public void readWithCallback(Path path, BiConsumer<Class<? extends T>, T> callback) {
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
      BiConsumer<Class<? extends T>, T> callback) {
    try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), charset)) {
      readWithCallback(reader, callback);
    } catch (IOException e) {
      throw new FixedFormatIOException("Cannot open path: " + path, e);
    }
  }

  // --- internal helpers ---

  private static BufferedReader toBuffered(Reader reader) {
    return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
  }

  private List<ClassPatternMapping<? extends T>> findMatches(String line) {
    return mappings.stream()
        .filter(m -> m.getPattern().matches(line))
        .collect(Collectors.toList());
  }

  private void processLine(String line, long lineNumber, BiConsumer<Class<? extends T>, T> emit) {
    if (!lineFilter.test(line)) {
      return;
    }
    List<ClassPatternMapping<? extends T>> matched = findMatches(line);
    if (matched.isEmpty()) {
      unmatchedLineStrategy.handle(lineNumber, line);
      return;
    }
    for (ClassPatternMapping<? extends T> mapping : multiMatchStrategy.resolve(matched, lineNumber)) {
      T record = parseRecord(mapping, line, lineNumber);
      if (record != null) {
        emit.accept(mapping.getRecordClass(), record);
      }
    }
  }

  private <R extends T> R parseRecord(ClassPatternMapping<R> mapping, String line, long lineNumber) {
    try {
      return manager.load(mapping.getRecordClass(), line);
    } catch (FixedFormatException e) {
      FixedFormatException wrapped = new FixedFormatException(
          "Parse error on line " + lineNumber + ": " + e.getMessage(), e);
      parseErrorStrategy.handle(wrapped, line, lineNumber);
      return null;
    }
  }

  /**
   * Returns a new builder for constructing a {@link FixedFormatReader}.
   *
   * @param <T> the common supertype of all record classes to be registered
   * @return a fresh builder instance
   */
  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Fluent builder for {@link FixedFormatReader}.
   *
   * <p>At minimum, one mapping must be added via {@link #addMapping} before calling
   * {@link #build()}.</p>
   *
   * @param <T> the common supertype of all record classes registered with this builder
   */
  public static class Builder<T> {
    private final List<ClassPatternMapping<? extends T>> mappings = new ArrayList<>();
    private MultiMatchStrategy multiMatchStrategy = MultiMatchStrategy.firstMatch();
    private UnmatchedLineStrategy unmatchedLineStrategy = UnmatchedLineStrategy.skip();
    private ParseErrorStrategy parseErrorStrategy = ParseErrorStrategy.throwException();
    private Predicate<String> lineFilter = line -> true;
    private FixedFormatManager manager = FixedFormatManagerImpl.create();

    /**
     * Registers a mapping that routes lines matching {@code pattern} to {@code clazz}.
     * Mappings are evaluated in registration order.
     *
     * @param clazz   the {@code @Record}-annotated class to instantiate when {@code pattern} matches
     * @param pattern the pattern that decides which lines are parsed as {@code clazz}
     * @return this builder
     * @throws IllegalArgumentException if {@code clazz} is not annotated with {@code @Record}
     */
    public Builder<T> addMapping(Class<? extends T> clazz, FixedFormatMatchPattern pattern) {
      mappings.add(new ClassPatternMapping<>(clazz, pattern));
      return this;
    }

    /**
     * Sets the strategy applied when more than one pattern matches a line.
     * Defaults to {@link MultiMatchStrategy#firstMatch()}.
     *
     * @param strategy the multi-match strategy to use; must not be {@code null}
     * @return this builder
     */
    public Builder<T> multiMatchStrategy(MultiMatchStrategy strategy) {
      this.multiMatchStrategy = strategy;
      return this;
    }

    /**
     * Sets the strategy applied when no pattern matches a line.
     * Defaults to {@link UnmatchedLineStrategy#skip()}.
     *
     * <p>Pass a lambda to define custom handling:</p>
     * <pre>{@code
     * .unmatchedLineStrategy((lineNumber, line) ->
     *     System.err.println("Unmatched line " + lineNumber + ": " + line))
     * }</pre>
     *
     * @param strategy the unmatched-line strategy to use; must not be {@code null}
     * @return this builder
     */
    public Builder<T> unmatchedLineStrategy(UnmatchedLineStrategy strategy) {
      this.unmatchedLineStrategy = strategy;
      return this;
    }

    /**
     * Sets the strategy applied when a matched line fails to parse.
     * Defaults to {@link ParseErrorStrategy#throwException()}.
     *
     * <p>Pass a lambda to define custom handling (throw to abort, return to skip):</p>
     * <pre>{@code
     * .parseErrorStrategy((wrapped, line, lineNumber) ->
     *     errors.add("Line " + lineNumber + ": " + wrapped.getMessage()))
     * }</pre>
     *
     * @param strategy the parse-error strategy to use; must not be {@code null}
     * @return this builder
     */
    public Builder<T> parseErrorStrategy(ParseErrorStrategy strategy) {
      this.parseErrorStrategy = strategy;
      return this;
    }

    /**
     * Registers a pre-match line inclusion predicate. Lines for which the predicate returns
     * {@code false} are skipped entirely before pattern matching, bypassing the
     * {@link UnmatchedLineStrategy}.
     *
     * <p>Defaults to accepting every line.</p>
     *
     * @param predicate returns {@code true} for lines that should be processed
     * @return this builder
     */
    public Builder<T> includeLines(Predicate<String> predicate) {
      this.lineFilter = predicate;
      return this;
    }

    /**
     * Overrides the {@link FixedFormatManager} used to parse each line into a record object.
     * Use to inject a custom manager — for example to add metrics, caching, or
     * field-level transformation. Defaults to a new {@code FixedFormatManagerImpl}.
     *
     * @param manager the manager to use; must not be {@code null}
     * @return this builder
     */
    public Builder<T> manager(FixedFormatManager manager) {
      this.manager = manager;
      return this;
    }

    /**
     * Builds and returns a configured {@link FixedFormatReader}.
     *
     * @return a new reader instance
     * @throws IllegalArgumentException if no mappings have been added
     */
    public FixedFormatReader<T> build() {
      if (mappings.isEmpty()) {
        throw new IllegalArgumentException("At least one mapping must be provided");
      }
      return new FixedFormatReader<>(this);
    }
  }
}
