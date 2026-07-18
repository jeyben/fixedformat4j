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
package com.ancientprogramming.fixedformat4j.issues;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Issue 182 (bug #3) - the eager enum-length and date/time-pattern validations resolve
 * a repeating field's ({@code count > 1}) raw {@code List}/array return type instead of its
 * element type, so both checks were silently skipped for repeating fields (they always saw
 * {@code List}, never {@code isEnum()} nor a date type). Both validations must now resolve the
 * element type the same way {@code doValidateFieldNullChar}/{@code doValidateNullValue} already
 * do, and fire on the first {@code load}/{@code export} call, exactly as they do for a
 * non-repeating field of the same element type.
 *
 * @since 2.0.0
 */
public class TestIssue182RepeatingFieldValidation {

  private final FixedFormatManager manager = FixedFormatManagerImpl.create();

  @Test
  public void testEnumLengthValidationFiresForRepeatingField() {
    // Status182.ONE ("ONE", length 3) does not fit in the @Field(length = 2) slot; for a
    // non-repeating field this is caught eagerly. It must be caught here too.
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(RepeatingEnumRecord182.class, "ONON"));
    assertTrue(ex.getMessage().contains("Status182"), "Message should name the enum: " + ex.getMessage());
  }

  @Test
  public void testEnumLengthValidationDoesNotThrowWhenValuesFit() {
    RepeatingEnumFitRecord182 loaded = manager.load(RepeatingEnumFitRecord182.class, "RED  GREEN");
    assertEquals(2, loaded.getColors().size());
    assertEquals(Color182.RED, loaded.getColors().get(0));
    assertEquals(Color182.GREEN, loaded.getColors().get(1));
  }

  @Test
  public void testPatternValidationFiresForRepeatingField() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(RepeatingInvalidPatternRecord182.class, "2024010120240102"));
    assertTrue(ex.getMessage().toLowerCase().contains("pattern"),
        "Message should mention the invalid pattern: " + ex.getMessage());
  }

  enum Status182 { ON, ONE }

  @Record
  public static class RepeatingEnumRecord182 {
    private List<Status182> statuses;

    @Field(offset = 1, length = 2, count = 2)
    public List<Status182> getStatuses() {
      return statuses;
    }

    public void setStatuses(List<Status182> statuses) {
      this.statuses = statuses;
    }
  }

  enum Color182 { RED, GREEN, BLUE }

  @Record
  public static class RepeatingEnumFitRecord182 {
    private List<Color182> colors;

    @Field(offset = 1, length = 5, count = 2)
    public List<Color182> getColors() {
      return colors;
    }

    public void setColors(List<Color182> colors) {
      this.colors = colors;
    }
  }

  @Record
  public static class RepeatingInvalidPatternRecord182 {
    private List<LocalDate> dates;

    @Field(offset = 1, length = 8, count = 2)
    @FixedFormatPattern("not a valid pattern[")
    public List<LocalDate> getDates() {
      return dates;
    }

    public void setDates(List<LocalDate> dates) {
      this.dates = dates;
    }
  }
}
