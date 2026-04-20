package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

import static com.ancientprogramming.fixedformat4j.annotation.Field.UNSET_NULL_CHAR;

/**
 * Shared null-char helpers used by both {@link FixedFormatManagerImpl} and
 * {@link RepeatingFieldSupport}.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.7.1
 */
final class NullCharSupport {

  private NullCharSupport() {}

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
}
