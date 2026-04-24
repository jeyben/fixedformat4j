package com.ancientprogramming.fixedformat4j.io.pattern;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestRegexFixedFormatMatchPattern {

  @Test
  void matchesLineStartingWithPattern() {
    FixedFormatMatchPattern pattern = new RegexFixedFormatMatchPattern("^A");
    assertTrue(pattern.matches("ALPHA record"));
  }

  @Test
  void doesNotMatchLineThatFailsPattern() {
    FixedFormatMatchPattern pattern = new RegexFixedFormatMatchPattern("^A");
    assertFalse(pattern.matches("BETA record"));
  }

  @Test
  void matchesExactPattern() {
    FixedFormatMatchPattern pattern = new RegexFixedFormatMatchPattern("^TXN\\d{3}");
    assertTrue(pattern.matches("TXN042 some data"));
    assertFalse(pattern.matches("TXN data"));
  }

  @Test
  void throwsFixedFormatExceptionForInvalidRegex() {
    assertThrows(FixedFormatException.class, () -> new RegexFixedFormatMatchPattern("[[invalid"));
  }

  @Test
  void returnsFalseForNullLine() {
    FixedFormatMatchPattern pattern = new RegexFixedFormatMatchPattern("^A");
    assertFalse(pattern.matches(null));
  }
}
