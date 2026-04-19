package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FixedFormatReader<T> {

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
