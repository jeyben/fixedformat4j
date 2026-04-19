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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.concurrent.atomic.AtomicLong;
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

  private static final Logger LOG = LoggerFactory.getLogger(FixedFormatReader.class);

  private final List<ClassPatternMapping<? extends T>> mappings;
  private final MultiMatchStrategy multiMatchStrategy;
  private final UnmatchedLineStrategy unmatchedLineStrategy;
  private final UnmatchedLineHandler unmatchedLineHandler;
  private final ParseErrorStrategy parseErrorStrategy;
  private final ParseErrorHandler parseErrorHandler;
  private final Predicate<String> lineFilter;
  private final FixedFormatManager manager;

  private FixedFormatReader(Builder<T> builder) {
    this.mappings = List.copyOf(builder.mappings);
    this.multiMatchStrategy = builder.multiMatchStrategy;
    this.unmatchedLineStrategy = builder.unmatchedLineStrategy;
    this.unmatchedLineHandler = builder.unmatchedLineHandler;
    this.parseErrorStrategy = builder.parseErrorStrategy;
    this.parseErrorHandler = builder.parseErrorHandler;
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
    BufferedReader buffered = reader instanceof BufferedReader
        ? (BufferedReader) reader
        : new BufferedReader(reader);
    AtomicLong lineCounter = new AtomicLong(0);

    Spliterator<T> spliterator = new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
        String line;
        try {
          line = buffered.readLine();
        } catch (IOException e) {
          throw new FixedFormatIOException("IO error reading line " + (lineCounter.get() + 1), e);
        }
        if (line == null) {
          return false;
        }
        long lineNumber = lineCounter.incrementAndGet();

        if (!lineFilter.test(line)) {
          return true;
        }

        List<ClassPatternMapping<? extends T>> matched = findMatches(line);

        if (matched.isEmpty()) {
          handleUnmatched(lineNumber, line);
          return true;
        }

        emitMatches(matched, line, lineNumber, action);
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
  public Stream<T> readAsStream(File file) throws FixedFormatIOException {
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
  public Stream<T> readAsStream(File file, Charset charset) throws FixedFormatIOException {
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
  public Stream<T> readAsStream(Path path) throws FixedFormatIOException {
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
  public Stream<T> readAsStream(Path path, Charset charset) throws FixedFormatIOException {
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
    readWithCallback(reader, (clazz, record) ->
        result.computeIfAbsent(clazz, k -> new ArrayList<>()).add(record));
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
    return readAsMap(new InputStreamReader(inputStream, charset));
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
    try (Stream<T> stream = readAsStream(reader)) {
      stream.forEach(callback);
    }
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
    BufferedReader buffered = reader instanceof BufferedReader
        ? (BufferedReader) reader
        : new BufferedReader(reader);
    AtomicLong lineCounter = new AtomicLong(0);

    try {
      String line;
      while ((line = buffered.readLine()) != null) {
        long lineNumber = lineCounter.incrementAndGet();

        if (!lineFilter.test(line)) {
          continue;
        }

        List<ClassPatternMapping<? extends T>> matched = findMatches(line);

        if (matched.isEmpty()) {
          handleUnmatched(lineNumber, line);
          continue;
        }

        for (ClassPatternMapping<? extends T> mapping : resolveMatches(matched, lineNumber)) {
          T record = parseRecord(mapping, line, lineNumber);
          if (record != null) {
            callback.accept(mapping.getRecordClass(), record);
          }
        }
      }
    } catch (IOException e) {
      throw new FixedFormatIOException("IO error reading line " + (lineCounter.get() + 1), e);
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

  // --- internal helpers ---

  private List<ClassPatternMapping<? extends T>> findMatches(String line) {
    List<ClassPatternMapping<? extends T>> matched = new ArrayList<>();
    for (ClassPatternMapping<? extends T> mapping : mappings) {
      if (mapping.getPattern().matches(line)) {
        matched.add(mapping);
        if (multiMatchStrategy == MultiMatchStrategy.FIRST_MATCH) {
          break;
        }
      }
    }
    return matched;
  }

  private List<ClassPatternMapping<? extends T>> resolveMatches(
      List<ClassPatternMapping<? extends T>> matched, long lineNumber) {
    if (multiMatchStrategy == MultiMatchStrategy.THROW_ON_AMBIGUITY && matched.size() > 1) {
      String classes = matched.stream()
          .map(m -> m.getRecordClass().getSimpleName())
          .collect(Collectors.joining(", "));
      throw new FixedFormatException(
          "Line " + lineNumber + " matched multiple patterns: " + classes);
    }
    return matched;
  }

  private void emitMatches(List<ClassPatternMapping<? extends T>> matched, String line,
      long lineNumber, Consumer<? super T> action) {
    for (ClassPatternMapping<? extends T> mapping : resolveMatches(matched, lineNumber)) {
      T record = parseRecord(mapping, line, lineNumber);
      if (record != null) {
        action.accept(record);
      }
    }
  }

  private <R extends T> R parseRecord(ClassPatternMapping<R> mapping, String line, long lineNumber) {
    try {
      return manager.load(mapping.getRecordClass(), line);
    } catch (FixedFormatException e) {
      return handleParseError(e, line, lineNumber);
    }
  }

  private <R extends T> R handleParseError(FixedFormatException e, String line, long lineNumber) {
    FixedFormatException wrapped = new FixedFormatException(
        "Parse error on line " + lineNumber + ": " + e.getMessage(), e);
    switch (parseErrorStrategy) {
      case THROW:
        throw wrapped;
      case SKIP_AND_LOG:
        LOG.warn("Skipping line {}: {} — {}", lineNumber, line, e.getMessage());
        return null;
      case FORWARD_TO_HANDLER:
        parseErrorHandler.handle(lineNumber, line, wrapped);
        return null;
      default:
        throw wrapped;
    }
  }

  private void handleUnmatched(long lineNumber, String line) {
    switch (unmatchedLineStrategy) {
      case SKIP:
        break;
      case THROW:
        throw new FixedFormatException(
            "No pattern matched line " + lineNumber + ": " + line);
      case FORWARD_TO_HANDLER:
        unmatchedLineHandler.handle(lineNumber, line);
        break;
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
   * {@link #build()}. Strategy-specific handlers must be registered before {@code build()}
   * when the corresponding strategy is set to {@code FORWARD_TO_HANDLER}.</p>
   *
   * @param <T> the common supertype of all record classes registered with this builder
   */
  public static class Builder<T> {
    private final List<ClassPatternMapping<? extends T>> mappings = new ArrayList<>();
    private MultiMatchStrategy multiMatchStrategy = MultiMatchStrategy.FIRST_MATCH;
    private UnmatchedLineStrategy unmatchedLineStrategy = UnmatchedLineStrategy.SKIP;
    private UnmatchedLineHandler unmatchedLineHandler;
    private ParseErrorStrategy parseErrorStrategy = ParseErrorStrategy.THROW;
    private ParseErrorHandler parseErrorHandler;
    private Predicate<String> lineFilter = line -> true;
    private FixedFormatManager manager = new FixedFormatManagerImpl();

    /**
     * Registers a mapping from a {@code @Record}-annotated class to a line-match pattern.
     * Mappings are evaluated in registration order.
     *
     * @param clazz   the record class to instantiate when {@code pattern} matches
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
     * Defaults to {@link MultiMatchStrategy#FIRST_MATCH}.
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
     * Defaults to {@link UnmatchedLineStrategy#SKIP}.
     *
     * @param strategy the unmatched-line strategy to use; must not be {@code null}
     * @return this builder
     */
    public Builder<T> unmatchedLineStrategy(UnmatchedLineStrategy strategy) {
      this.unmatchedLineStrategy = strategy;
      return this;
    }

    /**
     * Registers the handler invoked when {@link UnmatchedLineStrategy#FORWARD_TO_HANDLER}
     * is active and a line matches no pattern. Must be set before calling {@link #build()}
     * when the strategy is {@code FORWARD_TO_HANDLER}.
     *
     * @param handler the handler to invoke; must not be {@code null}
     * @return this builder
     */
    public Builder<T> unmatchedLineHandler(UnmatchedLineHandler handler) {
      this.unmatchedLineHandler = handler;
      return this;
    }

    /**
     * Sets the strategy applied when a matched line fails to parse.
     * Defaults to {@link ParseErrorStrategy#THROW}.
     *
     * @param strategy the parse-error strategy to use; must not be {@code null}
     * @return this builder
     */
    public Builder<T> parseErrorStrategy(ParseErrorStrategy strategy) {
      this.parseErrorStrategy = strategy;
      return this;
    }

    /**
     * Registers the handler invoked when {@link ParseErrorStrategy#FORWARD_TO_HANDLER}
     * is active and a line fails to parse. Must be set before calling {@link #build()}
     * when the strategy is {@code FORWARD_TO_HANDLER}.
     *
     * @param handler the handler to invoke; must not be {@code null}
     * @return this builder
     */
    public Builder<T> parseErrorHandler(ParseErrorHandler handler) {
      this.parseErrorHandler = handler;
      return this;
    }

    /**
     * Registers a pre-match line filter. Lines that do <em>not</em> satisfy the predicate
     * are skipped entirely before pattern matching, bypassing the
     * {@link UnmatchedLineStrategy}.
     *
     * <p>The default filter accepts every line.</p>
     *
     * @param filter a predicate that returns {@code true} for lines that should be processed
     * @return this builder
     */
    public Builder<T> lineFilter(Predicate<String> filter) {
      this.lineFilter = filter;
      return this;
    }

    /**
     * Overrides the {@link FixedFormatManager} used to parse each line into a record object.
     * Primarily useful for testing. Defaults to {@link FixedFormatManagerImpl}.
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
     * @throws IllegalStateException    if a {@code FORWARD_TO_HANDLER} strategy is active
     *                                  but the corresponding handler has not been registered
     */
    public FixedFormatReader<T> build() {
      if (mappings.isEmpty()) {
        throw new IllegalArgumentException("At least one mapping must be provided");
      }
      if (unmatchedLineStrategy == UnmatchedLineStrategy.FORWARD_TO_HANDLER && unmatchedLineHandler == null) {
        throw new IllegalStateException("unmatchedLineHandler must be set when strategy is FORWARD_TO_HANDLER");
      }
      if (parseErrorStrategy == ParseErrorStrategy.FORWARD_TO_HANDLER && parseErrorHandler == null) {
        throw new IllegalStateException("parseErrorHandler must be set when strategy is FORWARD_TO_HANDLER");
      }
      return new FixedFormatReader<>(this);
    }
  }
}
