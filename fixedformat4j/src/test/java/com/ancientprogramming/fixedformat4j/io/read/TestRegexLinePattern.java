package com.ancientprogramming.fixedformat4j.io.read;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestRegexLinePattern {

  @Test
  void matchesLineStartingWithPattern() {
    LinePattern pattern = new RegexLinePattern("^A");
    assertTrue(pattern.matches("ALPHA record"));
  }

  @Test
  void doesNotMatchLineThatFailsPattern() {
    LinePattern pattern = new RegexLinePattern("^A");
    assertFalse(pattern.matches("BETA record"));
  }

  @Test
  void matchesExactPattern() {
    LinePattern pattern = new RegexLinePattern("^TXN\\d{3}");
    assertTrue(pattern.matches("TXN042 some data"));
    assertFalse(pattern.matches("TXN data"));
  }

  @Test
  void throwsFixedFormatExceptionForInvalidRegex() {
    assertThrows(FixedFormatException.class, () -> new RegexLinePattern("[[invalid"));
  }

  @Test
  void returnsFalseForNullLine() {
    LinePattern pattern = new RegexLinePattern("^A");
    assertFalse(pattern.matches(null));
  }
}
