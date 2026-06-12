package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

import static com.ancientprogramming.fixedformat4j.annotation.Field.UNSET_NULL_CHAR;

/**
 * Shared null-sentinel helpers used by both {@link FixedFormatManagerImpl} and
 * {@link RepeatingFieldSupport}. Covers the single-character {@code nullChar}
 * convention (since 1.7.1) and the literal-string {@code nullValue} convention
 * (since 1.9.0).
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.7.1
 */
final class NullSupport {

  private NullSupport() {}

  /**
   * Returns {@code true} when {@code @Field.nullChar()} is explicitly configured (non-sentinel).
   * The sentinel {@link com.ancientprogramming.fixedformat4j.annotation.Field#UNSET_NULL_CHAR}
   * marks "not configured" and is never treated as a real null character. Setting
   * {@code nullChar} equal to {@code paddingChar} activates the "blank-is-null" convention
   * (Issue 84).
   */
  static boolean isNullCharActive(FormatInstructions instructions) {
    return instructions.getNullChar() != UNSET_NULL_CHAR;
  }

  /**
   * Returns {@code true} when the raw slice consists entirely of the configured
   * {@code nullChar}. An empty slice is never treated as null.
   */
  static boolean isNullSlice(String slice, FormatInstructions instructions) {
    if (!isNullCharActive(instructions) || slice == null || slice.isEmpty()) {
      return false;
    }
    char nullChar = instructions.getNullChar();
    for (int i = 0; i < slice.length(); i++) {
      if (slice.charAt(i) != nullChar) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} when {@code @Field.nullValue()} is explicitly configured.
   * The default empty string marks "not configured" (Issue 130).
   *
   * @since 1.9.0
   */
  static boolean isNullValueActive(FormatInstructions instructions) {
    return !instructions.getNullValue().isEmpty();
  }

  /**
   * Returns {@code true} when the raw slice equals the configured {@code nullValue} literal.
   *
   * @since 1.9.0
   */
  static boolean isNullValueSlice(String slice, FormatInstructions instructions) {
    return isNullValueActive(instructions) && instructions.getNullValue().equals(slice);
  }

  /**
   * Unified load-side check: {@code true} when the slice matches either the
   * {@code nullChar} or the {@code nullValue} convention. The two are mutually
   * exclusive per field, enforced by {@link FieldValidator}.
   *
   * @since 1.9.0
   */
  static boolean isNullSliceOrValue(String slice, FormatInstructions instructions) {
    return isNullSlice(slice, instructions) || isNullValueSlice(slice, instructions);
  }
}
