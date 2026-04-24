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
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Core line-routing engine for {@link FixedFormatReader}.
 *
 * <p>Matches each line against registered {@link ClassPatternMapping}s, applies multi-match
 * and unmatched-line strategies, parses the line into a record, and emits the result via a
 * callback. Also handles typed handler dispatch for {@link FixedFormatReader#processAll}.</p>
 */
class FixedFormatLineProcessor {

  private final List<ClassPatternMapping<?>> mappings;
  private final MultiMatchStrategy multiMatchStrategy;
  private final UnmatchStrategy unmatchStrategy;
  private final ParseErrorStrategy parseErrorStrategy;
  private final Predicate<String> lineFilter;
  private final FixedFormatManager manager;

  FixedFormatLineProcessor(
      List<ClassPatternMapping<?>> mappings,
      MultiMatchStrategy multiMatchStrategy,
      UnmatchStrategy unmatchStrategy,
      ParseErrorStrategy parseErrorStrategy,
      Predicate<String> lineFilter,
      FixedFormatManager manager) {
    this.mappings = mappings;
    this.multiMatchStrategy = multiMatchStrategy;
    this.unmatchStrategy = unmatchStrategy;
    this.parseErrorStrategy = parseErrorStrategy;
    this.lineFilter = lineFilter;
    this.manager = manager;
  }

  void processLine(String line, long lineNumber, BiConsumer<Class<?>, Object> emit) {
    if (!lineFilter.test(line)) {
      return;
    }
    List<ClassPatternMapping<?>> matched = findMatches(line);
    if (matched.isEmpty()) {
      unmatchStrategy.handle(lineNumber, line);
      return;
    }
    for (ClassPatternMapping<?> mapping : multiMatchStrategy.resolve(matched, lineNumber)) {
      Object record = parseRecord(mapping, line, lineNumber);
      if (record != null) {
        emit.accept(mapping.getRecordClass(), record);
      }
    }
  }

  /**
   * Variant used by {@link FixedFormatReader#readAsRows} for round-trip support.
   *
   * <p>Unlike the single-callback overload, this variant:</p>
   * <ul>
   *   <li>Calls {@code unmatchedCallback} instead of the configured {@link UnmatchStrategy}
   *       so that all unmatched lines are captured as {@link UnmatchedRow} entries.</li>
   *   <li>Calls {@code unmatchedCallback} also for lines rejected by the line filter, so
   *       no line is silently dropped and the original file can be reconstructed exactly.</li>
   * </ul>
   */
  void processLine(String line, long lineNumber,
                   BiConsumer<Class<?>, Object> matchedCallback,
                   Consumer<String> unmatchedCallback) {
    if (!lineFilter.test(line)) {
      unmatchedCallback.accept(line);
      return;
    }
    List<ClassPatternMapping<?>> matched = findMatches(line);
    if (matched.isEmpty()) {
      unmatchedCallback.accept(line);
      return;
    }
    for (ClassPatternMapping<?> mapping : multiMatchStrategy.resolve(matched, lineNumber)) {
      Object record = parseRecord(mapping, line, lineNumber);
      if (record != null) {
        matchedCallback.accept(mapping.getRecordClass(), record);
      }
    }
  }

  // Wildcard capture: ClassPatternMapping<?> → ClassPatternMapping<R>, enabling type-safe load.
  private <R> Object parseRecord(ClassPatternMapping<R> mapping, String line, long lineNumber) {
    try {
      return manager.load(mapping.getRecordClass(), line);
    } catch (FixedFormatException e) {
      FixedFormatException wrapped = new FixedFormatException(
          "Parse error on line " + lineNumber + ": " + e.getMessage(), e);
      parseErrorStrategy.handle(wrapped, line, lineNumber);
      return null;
    }
  }

  private List<ClassPatternMapping<?>> findMatches(String line) {
    return mappings.stream()
        .filter(m -> m.getPattern().matches(line))
        .collect(Collectors.toList());
  }

  // Called from FixedFormatReader.processAll via method reference (processor::fireHandler).
  // Safe: records dispatched under key K were loaded via manager.load(K, line), i.e. are K instances.
  void fireHandler(Class<?> clazz, Object record) {
    doFireHandler(clazz, record);
  }

  // Wildcard capture allows the typed Consumer<R> cast to be verified by the compiler.
  @SuppressWarnings("unchecked")
  private <R> void doFireHandler(Class<R> clazz, Object record) {
    for (ClassPatternMapping<?> mapping : mappings) {
      if (mapping.getRecordClass() == clazz && mapping.getHandler() != null) {
        ((Consumer<R>) mapping.getHandler()).accept((R) record);
      }
    }
  }
}
