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
 * <p>Matches each line against registered {@link RecordMapping}s, applies multi-match
 * and unmatched-line strategies, parses the line into a record, and emits the result via a
 * callback. Also handles typed handler dispatch for {@link FixedFormatReader#processAll}.</p>
 */
class FixedFormatLineProcessor {

  private final List<RecordMapping<?>> mappings;
  private final MultiMatchStrategy multiMatchStrategy;
  private final UnmatchStrategy unmatchStrategy;
  private final ParseErrorStrategy parseErrorStrategy;
  private final Predicate<String> lineFilter;
  private final FixedFormatManager manager;

  FixedFormatLineProcessor(
      List<RecordMapping<?>> mappings,
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

  void processLine(String line, long lineNumber, BiConsumer<RecordMapping<?>, Object> emit) {
    if (!lineFilter.test(line)) {
      return;
    }
    List<RecordMapping<?>> matched = findMatches(line);
    if (matched.isEmpty()) {
      unmatchStrategy.handle(lineNumber, line);
      return;
    }
    List<RecordMapping<?>> toProcess = matched.size() == 1
        ? matched
        : multiMatchStrategy.resolve(matched, lineNumber);
    for (RecordMapping<?> mapping : toProcess) {
      Object record = parseRecord(mapping, line, lineNumber);
      if (record != null) {
        emit.accept(mapping, record);
      }
    }
  }

  // Wildcard capture: RecordMapping<?> → RecordMapping<R>, enabling type-safe load.
  private <R> Object parseRecord(RecordMapping<R> mapping, String line, long lineNumber) {
    try {
      return manager.load(mapping.getRecordClass(), line);
    } catch (FixedFormatException e) {
      FixedFormatException wrapped = new FixedFormatException(
          "Parse error on line " + lineNumber + ": " + e.getMessage(), e);
      parseErrorStrategy.handle(wrapped, line, lineNumber);
      return null;
    }
  }

  private List<RecordMapping<?>> findMatches(String line) {
    return mappings.stream()
        .filter(m -> m.getPattern().matches(line))
        .collect(Collectors.toList());
  }

  // Called from FixedFormatReader.processAll via method reference (processor::fireHandler).
  // Safe: records dispatched under key K were loaded via manager.load(K, line), i.e. are K instances.
  void fireHandler(RecordMapping<?> mapping, Object record) {
    doFireHandler(mapping, record);
  }

  // Wildcard capture allows the typed Consumer<R> cast to be verified by the compiler.
  @SuppressWarnings("unchecked")
  private <R> void doFireHandler(RecordMapping<R> mapping, Object record) {
    if (mapping.getHandler() != null) {
      mapping.getHandler().accept((R) record);
    }
  }
}
