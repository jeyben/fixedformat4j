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

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.io.segment.UnmatchedSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Fluent builder for {@link FixedFormatSegmentReader}.
 *
 * <p>Obtain an instance via {@link FixedFormatSegmentReader#builder()}.
 * At minimum, one mapping must be added via {@link #addMapping} before calling
 * {@link #build()}.</p>
 *
 * <p>Unlike {@link FixedFormatReaderBuilder}, this builder deliberately omits
 * {@code unmatchStrategy} — {@link FixedFormatSegmentReader} always captures every line,
 * so the concept of an "unmatched line strategy" does not apply.</p>
 */
public class FixedFormatSegmentReaderBuilder {

  final List<ClassPatternMapping<?>> mappings = new ArrayList<>();
  MultiMatchStrategy multiMatchStrategy = MultiMatchStrategy.firstMatch();
  ParseErrorStrategy parseErrorStrategy = ParseErrorStrategy.throwException();
  Predicate<String> lineFilter = line -> true;
  LineSlicingStrategy lineSlicingStrategy = LineSlicingStrategy.singleRecord();
  PartialChunkStrategy partialChunkStrategy = PartialChunkStrategy.skip();
  FixedFormatManager manager = FixedFormatManagerImpl.create();

  FixedFormatSegmentReaderBuilder() {}

  /**
   * Registers a mapping that routes lines matching {@code pattern} to {@code clazz}.
   * Mappings are evaluated in registration order.
   *
   * @param clazz   the {@code @Record}-annotated class to instantiate when {@code pattern} matches
   * @param pattern the pattern that decides which lines are parsed as {@code clazz}
   * @param <R>     the record type
   * @return this builder
   * @throws IllegalArgumentException if {@code clazz} is not annotated with {@code @Record}
   */
  public <R> FixedFormatSegmentReaderBuilder addMapping(Class<R> clazz, FixedFormatMatchPattern pattern) {
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
  public FixedFormatSegmentReaderBuilder multiMatchStrategy(MultiMatchStrategy strategy) {
    this.multiMatchStrategy = strategy;
    return this;
  }

  /**
   * Sets the strategy applied when a matched line fails to parse.
   * Defaults to {@link ParseErrorStrategy#throwException()}.
   *
   * @param strategy the parse-error strategy to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatSegmentReaderBuilder parseErrorStrategy(ParseErrorStrategy strategy) {
    this.parseErrorStrategy = strategy;
    return this;
  }

  /**
   * Registers a pre-match line inclusion predicate. Lines for which the predicate returns
   * {@code false} are treated as unmatched and become {@link UnmatchedSegment} entries in the
   * result.
   *
   * @param predicate returns {@code true} for lines that should be pattern-matched
   * @return this builder
   */
  public FixedFormatSegmentReaderBuilder includeLines(Predicate<String> predicate) {
    this.lineFilter = predicate;
    return this;
  }

  /**
   * Sets the per-line slicing strategy. Defaults to {@link LineSlicingStrategy#singleRecord()},
   * which treats each physical line as one record. Use {@link LineSlicingStrategy#packed(int)} for
   * files where multiple records are packed end-to-end within each line, or
   * {@link LineSlicingStrategy#mixed(java.util.function.Predicate, int)} for files that mix both
   * formats.
   *
   * @param strategy the slicing strategy to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatSegmentReaderBuilder lineSlicing(LineSlicingStrategy strategy) {
    this.lineSlicingStrategy = strategy;
    return this;
  }

  /**
   * Sets the strategy applied when the last chunk on a physical line is shorter than the declared
   * record width. Only relevant when using a slicing strategy other than
   * {@link LineSlicingStrategy#singleRecord()}. Defaults to {@link PartialChunkStrategy#skip()}.
   *
   * @param strategy the partial-chunk strategy to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatSegmentReaderBuilder partialChunkStrategy(PartialChunkStrategy strategy) {
    this.partialChunkStrategy = strategy;
    return this;
  }

  /**
   * Overrides the {@link FixedFormatManager} used to parse each line into a record object.
   *
   * @param manager the manager to use; must not be {@code null}
   * @return this builder
   */
  public FixedFormatSegmentReaderBuilder manager(FixedFormatManager manager) {
    this.manager = manager;
    return this;
  }

  /**
   * Builds and returns a configured {@link FixedFormatSegmentReader}.
   *
   * @return a new reader instance
   * @throws IllegalArgumentException if no mappings have been added
   */
  public FixedFormatSegmentReader build() {
    if (mappings.isEmpty()) {
      throw new IllegalArgumentException("At least one mapping must be provided");
    }
    return new FixedFormatSegmentReader(this);
  }
}
