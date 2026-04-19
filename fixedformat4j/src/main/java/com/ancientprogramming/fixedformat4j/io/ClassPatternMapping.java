package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.annotation.Record;

public class ClassPatternMapping<T> {

  private final Class<T> recordClass;
  private final FixedFormatMatchPattern pattern;

  public ClassPatternMapping(Class<T> recordClass, FixedFormatMatchPattern pattern) {
    if (recordClass.getAnnotation(Record.class) == null) {
      throw new IllegalArgumentException(
          recordClass.getSimpleName() + " is not annotated with @Record");
    }
    this.recordClass = recordClass;
    this.pattern = pattern;
  }

  public Class<T> getRecordClass() {
    return recordClass;
  }

  public FixedFormatMatchPattern getPattern() {
    return pattern;
  }
}
