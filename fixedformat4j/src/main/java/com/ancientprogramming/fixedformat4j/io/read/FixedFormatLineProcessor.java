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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Core line-routing engine for {@link FixedFormatReader}.
 *
 * <p>Matches each line against a {@link RecordMappingIndex}, applies multi-match
 * and unmatched-line strategies, parses the line into a record, and emits the result via a
 * callback.</p>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
class FixedFormatLineProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(FixedFormatLineProcessor.class);

  private final RecordMappingIndex index;
  private final MultiMatchStrategy multiMatchStrategy;
  private final UnmatchStrategy unmatchStrategy;
  private final ParseErrorStrategy parseErrorStrategy;
  private final Predicate<String> exclusionFilter;
  private final FixedFormatManager manager;

  FixedFormatLineProcessor(
      RecordMappingIndex index,
      MultiMatchStrategy multiMatchStrategy,
      UnmatchStrategy unmatchStrategy,
      ParseErrorStrategy parseErrorStrategy,
      Predicate<String> exclusionFilter,
      FixedFormatManager manager) {
    this.index = index;
    this.multiMatchStrategy = multiMatchStrategy;
    this.unmatchStrategy = unmatchStrategy;
    this.parseErrorStrategy = parseErrorStrategy;
    this.exclusionFilter = exclusionFilter;
    this.manager = manager;
  }

  void processLine(String line, long lineNumber, BiConsumer<Class<?>, Object> emit) {
    if (exclusionFilter.test(line)) {
      LOG.debug("Excluding line {}: {}", lineNumber, line);
      return;
    }
    List<RecordMapping<?>> matched = index.findMatches(line);
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
        emit.accept(mapping.getRecordClass(), record);
      }
    }
  }

  private Object parseRecord(RecordMapping<?> mapping, String line, long lineNumber) {
    try {
      return manager.load(mapping.getRecordClass(), line);
    } catch (FixedFormatException e) {
      FixedFormatException wrapped = new FixedFormatException(
          format("Parse error on line %d: %s", lineNumber, e.getMessage()), e);
      parseErrorStrategy.handle(wrapped, line, lineNumber);
      return null;
    }
  }

}
