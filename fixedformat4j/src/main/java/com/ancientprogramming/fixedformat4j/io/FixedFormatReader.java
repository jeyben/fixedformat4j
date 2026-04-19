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

  public Stream<T> readAsStream(InputStream inputStream) {
    return readAsStream(inputStream, StandardCharsets.UTF_8);
  }

  public Stream<T> readAsStream(InputStream inputStream, Charset charset) {
    return readAsStream(new InputStreamReader(inputStream, charset));
  }

  public Stream<T> readAsStream(File file) throws FixedFormatIOException {
    return readAsStream(file, StandardCharsets.UTF_8);
  }

  public Stream<T> readAsStream(File file, Charset charset) throws FixedFormatIOException {
    try {
      return readAsStream(new InputStreamReader(new FileInputStream(file), charset));
    } catch (FileNotFoundException e) {
      throw new FixedFormatIOException("File not found: " + file, e);
    }
  }

  public Stream<T> readAsStream(Path path) throws FixedFormatIOException {
    return readAsStream(path, StandardCharsets.UTF_8);
  }

  public Stream<T> readAsStream(Path path, Charset charset) throws FixedFormatIOException {
    try {
      return readAsStream(new InputStreamReader(Files.newInputStream(path), charset));
    } catch (IOException e) {
      throw new FixedFormatIOException("Cannot open path: " + path, e);
    }
  }

  // --- readAsList ---

  public List<T> readAsList(Reader reader) {
    try (Stream<T> stream = readAsStream(reader)) {
      return stream.collect(Collectors.toList());
    }
  }

  public List<T> readAsList(InputStream inputStream) {
    return readAsList(inputStream, StandardCharsets.UTF_8);
  }

  public List<T> readAsList(InputStream inputStream, Charset charset) {
    try (Stream<T> stream = readAsStream(inputStream, charset)) {
      return stream.collect(Collectors.toList());
    }
  }

  public List<T> readAsList(File file) {
    return readAsList(file, StandardCharsets.UTF_8);
  }

  public List<T> readAsList(File file, Charset charset) {
    try (Stream<T> stream = readAsStream(file, charset)) {
      return stream.collect(Collectors.toList());
    }
  }

  public List<T> readAsList(Path path) {
    return readAsList(path, StandardCharsets.UTF_8);
  }

  public List<T> readAsList(Path path, Charset charset) {
    try (Stream<T> stream = readAsStream(path, charset)) {
      return stream.collect(Collectors.toList());
    }
  }

  // --- readAsMap ---

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

  public Map<Class<? extends T>, List<T>> readAsMap(InputStream inputStream) {
    return readAsMap(inputStream, StandardCharsets.UTF_8);
  }

  public Map<Class<? extends T>, List<T>> readAsMap(InputStream inputStream, Charset charset) {
    return readAsMap(new InputStreamReader(inputStream, charset));
  }

  public Map<Class<? extends T>, List<T>> readAsMap(File file) {
    return readAsMap(file, StandardCharsets.UTF_8);
  }

  public Map<Class<? extends T>, List<T>> readAsMap(File file, Charset charset) {
    try {
      return readAsMap(new InputStreamReader(new FileInputStream(file), charset));
    } catch (FileNotFoundException e) {
      throw new FixedFormatIOException("File not found: " + file, e);
    }
  }

  public Map<Class<? extends T>, List<T>> readAsMap(Path path) {
    return readAsMap(path, StandardCharsets.UTF_8);
  }

  public Map<Class<? extends T>, List<T>> readAsMap(Path path, Charset charset) {
    try {
      return readAsMap(new InputStreamReader(Files.newInputStream(path), charset));
    } catch (IOException e) {
      throw new FixedFormatIOException("Cannot open path: " + path, e);
    }
  }

  // --- readWithCallback ---

  public void readWithCallback(Reader reader, Consumer<T> callback) {
    try (Stream<T> stream = readAsStream(reader)) {
      stream.forEach(callback);
    }
  }

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

  public void readWithCallback(InputStream inputStream, Consumer<T> callback) {
    readWithCallback(inputStream, StandardCharsets.UTF_8, callback);
  }

  public void readWithCallback(InputStream inputStream, Charset charset, Consumer<T> callback) {
    readWithCallback(new InputStreamReader(inputStream, charset), callback);
  }

  public void readWithCallback(File file, Consumer<T> callback) {
    readWithCallback(file, StandardCharsets.UTF_8, callback);
  }

  public void readWithCallback(File file, Charset charset, Consumer<T> callback) {
    try (Stream<T> stream = readAsStream(file, charset)) {
      stream.forEach(callback);
    }
  }

  public void readWithCallback(Path path, Consumer<T> callback) {
    readWithCallback(path, StandardCharsets.UTF_8, callback);
  }

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

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static class Builder<T> {
    private final List<ClassPatternMapping<? extends T>> mappings = new ArrayList<>();
    private MultiMatchStrategy multiMatchStrategy = MultiMatchStrategy.FIRST_MATCH;
    private UnmatchedLineStrategy unmatchedLineStrategy = UnmatchedLineStrategy.SKIP;
    private UnmatchedLineHandler unmatchedLineHandler;
    private ParseErrorStrategy parseErrorStrategy = ParseErrorStrategy.THROW;
    private ParseErrorHandler parseErrorHandler;
    private Predicate<String> lineFilter = line -> true;
    private FixedFormatManager manager = new FixedFormatManagerImpl();

    public Builder<T> addMapping(Class<? extends T> clazz, FixedFormatMatchPattern pattern) {
      mappings.add(new ClassPatternMapping<>(clazz, pattern));
      return this;
    }

    public Builder<T> multiMatchStrategy(MultiMatchStrategy strategy) {
      this.multiMatchStrategy = strategy;
      return this;
    }

    public Builder<T> unmatchedLineStrategy(UnmatchedLineStrategy strategy) {
      this.unmatchedLineStrategy = strategy;
      return this;
    }

    public Builder<T> unmatchedLineHandler(UnmatchedLineHandler handler) {
      this.unmatchedLineHandler = handler;
      return this;
    }

    public Builder<T> parseErrorStrategy(ParseErrorStrategy strategy) {
      this.parseErrorStrategy = strategy;
      return this;
    }

    public Builder<T> parseErrorHandler(ParseErrorHandler handler) {
      this.parseErrorHandler = handler;
      return this;
    }

    public Builder<T> lineFilter(Predicate<String> filter) {
      this.lineFilter = filter;
      return this;
    }

    public Builder<T> manager(FixedFormatManager manager) {
      this.manager = manager;
      return this;
    }

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
