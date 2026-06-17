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
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.AbstractFixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for Issue 161 — custom formatters on enum fields.
 *
 * <p>Before the fix, {@code validateEnumFieldLength} measured the longest enum
 * {@code name()} (LITERAL) or the ordinal digit count (NUMERIC) and rejected the field
 * when that exceeded {@code @Field(length)}, regardless of any custom {@code formatter=}.
 * That premise only holds for the built-in {@code EnumFormatter}; a custom formatter emits
 * its own representation, so the length check is meaningless and must be skipped.
 *
 * <p>The fixtures reproduce the reporter's exact scenario: a 12-constant enum whose names are
 * far longer than one character, each mapped to a single-character code, declared with
 * {@code length = 1}. Neither built-in {@code EnumFormat} mode can satisfy length 1 (LITERAL
 * overflows on the names, NUMERIC needs two digits for 12 constants), so only a custom
 * formatter works.
 */
public class TestIssue161CustomEnumFormatter {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  // -------------------------------------------------------------------------
  // Reporter's enum + custom formatter (Guava BiMap replaced with plain maps)
  // -------------------------------------------------------------------------

  enum NsccTransactionType {
    SINGLE_PURCHASE("0"),
    LETTER_OF_INTENT("1"),
    RIGHTS_OF_ACCUMULATION("2"),
    NET_ASSET_VALUE("3"),
    GROUP_PURCHASE("4"),
    CDSC_LIQUIDATION("5"),
    PARTICIPANT_DISTRIBUTION_529("8"),
    BENEFICIARY_DISTRIBUTION_529("9"),
    SCHOOL_DISTRIBUTION_529("A"),
    ROLLOVER_DISTRIBUTION_529("B"),
    IN_STATE_PLAN_TRANSFER_DISTRIBUTION("E"),
    REGISTRATION_CHANGE_529("F");

    private final String value;

    NsccTransactionType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static class Formatter extends EnumFixedFormatter<NsccTransactionType> {
      public Formatter() {
        super(NsccTransactionType.class, NsccTransactionType::getValue);
      }
    }
  }

  abstract static class EnumFixedFormatter<T extends Enum<T>> extends AbstractFixedFormatter<T> {

    private final Map<T, String> enumToValue = new HashMap<>();
    private final Map<String, T> valueToEnum = new HashMap<>();

    protected EnumFixedFormatter(Class<T> enumClass, Function<T, String> valueFunction) {
      for (T constant : enumClass.getEnumConstants()) {
        String code = valueFunction.apply(constant);
        if (valueToEnum.put(code, constant) != null) {
          throw new IllegalArgumentException("Duplicate code: " + code);
        }
        enumToValue.put(constant, code);
      }
    }

    @Override
    public T asObject(String value, FormatInstructions instructions) {
      return valueToEnum.get(value);
    }

    @Override
    public String asString(T obj, FormatInstructions instructions) {
      return enumToValue.get(obj);
    }
  }

  @Record(length = 1)
  public static class CustomFormatterRecord {
    private NsccTransactionType nsccTransactionType;

    @Field(offset = 1, length = 1, formatter = NsccTransactionType.Formatter.class)
    public NsccTransactionType getNsccTransactionType() {
      return nsccTransactionType;
    }

    public void setNsccTransactionType(NsccTransactionType nsccTransactionType) {
      this.nsccTransactionType = nsccTransactionType;
    }
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  @Test
  public void customFormatterEnumField_loadsSingleCharCode() {
    CustomFormatterRecord record = manager.load(CustomFormatterRecord.class, "A");
    assertEquals(NsccTransactionType.SCHOOL_DISTRIBUTION_529, record.getNsccTransactionType());
  }

  @Test
  public void customFormatterEnumField_exportsSingleCharCode() {
    CustomFormatterRecord record = new CustomFormatterRecord();
    record.setNsccTransactionType(NsccTransactionType.REGISTRATION_CHANGE_529);
    assertEquals("F", manager.export(record));
  }

  @Test
  public void customFormatterEnumField_roundTripsConstantBeyondNinthOrdinal() {
    CustomFormatterRecord record = new CustomFormatterRecord();
    record.setNsccTransactionType(NsccTransactionType.IN_STATE_PLAN_TRANSFER_DISTRIBUTION);
    String exported = manager.export(record);
    assertEquals("E", exported);
    assertEquals(
        NsccTransactionType.IN_STATE_PLAN_TRANSFER_DISTRIBUTION,
        manager.load(CustomFormatterRecord.class, exported).getNsccTransactionType());
  }

  // -------------------------------------------------------------------------
  // Guard: the default (no custom formatter) enum path still validates length
  // -------------------------------------------------------------------------

  enum TooLongForField {
    VERY_LONG_ENUM_VALUE_NAME, ANOTHER_VALUE
  }

  @Record(length = 1)
  public static class DefaultFormatterTooShortRecord {
    private TooLongForField value;

    @Field(offset = 1, length = 1)
    public TooLongForField getValue() {
      return value;
    }

    public void setValue(TooLongForField value) {
      this.value = value;
    }
  }

  @Test
  public void defaultFormatterEnumNameTooLong_stillThrows() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(DefaultFormatterTooShortRecord.class, "X"));
    assertTrue(ex.getMessage().contains("max length"),
        "exception should mention max length: " + ex.getMessage());
  }

  // Sanity: the reporter's enum genuinely defeats both built-in EnumFormat modes at length 1.
  @Test
  public void reporterEnumDefeatsBothBuiltinModes() {
    int longestName = Arrays.stream(NsccTransactionType.values())
        .mapToInt(e -> e.name().length()).max().orElse(0);
    int numericWidth = String.valueOf(NsccTransactionType.values().length - 1).length();
    assertTrue(longestName > 1, "LITERAL would overflow length 1");
    assertTrue(numericWidth > 1, "NUMERIC would overflow length 1");
  }
}
