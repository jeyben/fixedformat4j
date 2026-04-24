package com.ancientprogramming.fixedformat4j.io.read;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;

import static com.ancientprogramming.fixedformat4j.io.read.LinePredicates.regex;
import static org.junit.jupiter.api.Assertions.*;

class TestLinePredicates {

  @Test
  void matchesLineStartingWithPattern() {
    assertTrue(regex("^A").test("ALPHA record"));
  }

  @Test
  void doesNotMatchLineFailingPattern() {
    assertFalse(regex("^A").test("BETA record"));
  }

  @Test
  void supportsDigitQuantifierPattern() {
    Predicate<String> p = regex("^TXN\\d{3}");
    assertTrue(p.test("TXN042 some data"));
    assertFalse(p.test("TXN data"));
  }

  @Test
  void throwsPatternSyntaxExceptionForInvalidRegex() {
    assertThrows(PatternSyntaxException.class, () -> regex("[[invalid"));
  }

  @Test
  void supportsPredicateComposition() {
    Predicate<String> aOrB = regex("^A").or(regex("^B"));
    assertTrue(aOrB.test("ALPHA"));
    assertTrue(aOrB.test("BETA"));
    assertFalse(aOrB.test("GAMMA"));
  }
}
