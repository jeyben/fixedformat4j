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
package com.ancientprogramming.fixedformat4j.processor;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Validates a single {@code @Record}-annotated type and reports diagnostics via the
 * processing environment's {@code Messager}. Per-field rules live in {@link FieldChecker};
 * this class owns the rules that span multiple fields. The cross-field rules are the
 * mirror-API twins of {@code FieldValidator.doValidateRestOfLineIsLastField} and
 * {@code doValidateRestOfLineRecordLength}; when a rule changes there, it must change here too.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
class RecordValidator {

  private final Messager messager;
  private final FieldChecker fieldChecker;

  RecordValidator(ProcessingEnvironment env) {
    this.messager = env.getMessager();
    this.fieldChecker = new FieldChecker(env.getMessager());
  }

  void validate(TypeElement recordType) {
    List<AnnotatedFixedFormatField> targets = FieldScanner.scan(recordType);
    List<FieldEntry> entries = new ArrayList<>();
    for (AnnotatedFixedFormatField target : targets) {
      fieldChecker.checkPattern(target);
      for (Field fieldAnnotation : target.fieldAnnotations) {
        fieldChecker.checkEnumLength(target, fieldAnnotation);
        fieldChecker.checkNullChar(target, fieldAnnotation);
        fieldChecker.checkNullValue(target, fieldAnnotation);
        fieldChecker.checkRestOfLineField(target, fieldAnnotation);
        entries.add(new FieldEntry(target, fieldAnnotation));
      }
    }
    checkRestOfLineIsLastField(recordType, entries);
    checkRestOfLineRecordLength(recordType, entries);
    checkRecordLengthOverflow(recordType, entries);
    checkOverlaps(entries);
  }

  /**
   * No runtime counterpart (stricter than the 1.8.x runtime, which silently pads or truncates):
   * a field may not run past a fixed {@code @Record(length)}.
   */
  private void checkRecordLengthOverflow(TypeElement recordType, List<FieldEntry> entries) {
    Record record = recordType.getAnnotation(Record.class);
    if (record == null || record.length() == -1) {
      return;
    }
    for (FieldEntry entry : entries) {
      if (entry.annotation.length() == Field.REST_OF_LINE || entry.endOffset() <= record.length()) {
        continue;
      }
      String width = entry.annotation.count() > 1
          ? format("%d x count %d", entry.annotation.length(), entry.annotation.count())
          : String.valueOf(entry.annotation.length());
      messager.printMessage(Diagnostic.Kind.ERROR,
          format("@Field length %s exceeds @Record length %d on %s (field ends at position %d)",
              width, record.length(), entry.target.label(), entry.endOffset()),
          entry.target.element);
    }
  }

  /**
   * No runtime counterpart (stricter than the 1.8.x runtime, where a later field silently
   * overwrites the overlapped characters on export): two fields may not occupy the same
   * character positions.
   */
  private void checkOverlaps(List<FieldEntry> entries) {
    List<FieldEntry> sorted = entries.stream()
        .filter(entry -> entry.annotation.length() != Field.REST_OF_LINE)
        .sorted(Comparator.comparingInt(entry -> entry.annotation.offset()))
        .collect(Collectors.toList());
    FieldEntry farthestReaching = null;
    for (FieldEntry current : sorted) {
      if (farthestReaching != null && current.annotation.offset() <= farthestReaching.endOffset()) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            format("@Field [offset=%d, length=%d] on %s overlaps @Field [offset=%d, length=%d] on %s",
                farthestReaching.annotation.offset(), farthestReaching.annotation.length(), farthestReaching.target.label(),
                current.annotation.offset(), current.annotation.length(), current.target.label()),
            current.target.element);
      }
      if (farthestReaching == null || current.endOffset() > farthestReaching.endOffset()) {
        farthestReaching = current;
      }
    }
  }

  /** Twin of {@code FieldValidator.doValidateRestOfLineIsLastField}. */
  private void checkRestOfLineIsLastField(TypeElement recordType, List<FieldEntry> entries) {
    FieldEntry restOfLine = null;
    int maxOtherOffset = Integer.MIN_VALUE;

    for (FieldEntry entry : entries) {
      if (entry.annotation.length() == Field.REST_OF_LINE) {
        if (restOfLine != null) {
          messager.printMessage(Diagnostic.Kind.ERROR,
              format("Only one @Field(length = -1) is allowed per record class %s, but found multiple",
                  recordType.getQualifiedName()),
              entry.target.element);
          return;
        }
        restOfLine = entry;
      } else {
        maxOtherOffset = Math.max(maxOtherOffset, entry.endOffset());
      }
    }

    if (restOfLine == null) {
      return;
    }
    if (maxOtherOffset >= restOfLine.annotation.offset()) {
      messager.printMessage(Diagnostic.Kind.ERROR,
          format("@Field(length = -1) on %s must be the last field (highest offset) in the record,"
                  + " but another field extends to position %d, at or after this field's start offset %d",
              restOfLine.target.label(), maxOtherOffset, restOfLine.annotation.offset()),
          restOfLine.target.element);
    }
  }

  /** Twin of {@code FieldValidator.doValidateRestOfLineRecordLength}. */
  private void checkRestOfLineRecordLength(TypeElement recordType, List<FieldEntry> entries) {
    Record record = recordType.getAnnotation(Record.class);
    if (record == null || record.length() == -1) {
      return;
    }
    for (FieldEntry entry : entries) {
      if (entry.annotation.length() == Field.REST_OF_LINE) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            format("@Field(length = -1) is not compatible with @Record(length = %d) on %s "
                    + "because record-level padding would corrupt the verbatim round-trip",
                record.length(), recordType.getQualifiedName()),
            entry.target.element);
        return;
      }
    }
  }

  /** One {@code @Field} annotation occurrence — a {@code @Fields} getter contributes several. */
  static final class FieldEntry {

    final AnnotatedFixedFormatField target;
    final Field annotation;

    FieldEntry(AnnotatedFixedFormatField target, Field annotation) {
      this.target = target;
      this.annotation = annotation;
    }

    /** Last occupied 1-based position, expanding {@code count > 1} repetitions. */
    int endOffset() {
      return annotation.offset() + Math.max(annotation.count(), 1) * annotation.length() - 1;
    }
  }
}
