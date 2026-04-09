package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Validates date/time pattern strings for the types supported by this library.
 *
 * <p>Validation is performed eagerly — before any data is parsed or formatted — so that
 * configuration errors surface immediately when a record class is first used, rather than
 * at the point a specific field is processed.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.6.0
 */
class PatternValidator {

  /**
   * Validates that {@code pattern} is a legal format pattern for {@code datatype}.
   *
   * <p>Validation is performed only for date/time types ({@link Date}, {@link LocalDate},
   * {@link LocalDateTime}). For all other types the method returns immediately.
   *
   * @param datatype the field's Java type
   * @param pattern  the pattern string from {@link com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern}
   * @throws FixedFormatException if the pattern is invalid for the given type
   */
  static void validate(Class<?> datatype, String pattern) {
    if (Date.class.equals(datatype)) {
      validateSimpleDateFormat(pattern);
    } else if (LocalDate.class.equals(datatype) || LocalDateTime.class.equals(datatype)) {
      validateDateTimeFormatter(pattern);
    }
  }

  private static void validateSimpleDateFormat(String pattern) {
    try {
      new SimpleDateFormat(pattern);
    } catch (IllegalArgumentException e) {
      throw new FixedFormatException(String.format("Invalid date pattern '%s': %s", pattern, e.getMessage()), e);
    }
  }

  private static void validateDateTimeFormatter(String pattern) {
    try {
      DateTimeFormatter.ofPattern(pattern);
    } catch (IllegalArgumentException e) {
      throw new FixedFormatException(String.format("Invalid date/time pattern '%s': %s", pattern, e.getMessage()), e);
    }
  }
}
