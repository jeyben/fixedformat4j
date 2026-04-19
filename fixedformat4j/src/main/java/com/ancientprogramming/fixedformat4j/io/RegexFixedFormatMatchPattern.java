package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexFixedFormatMatchPattern implements FixedFormatMatchPattern {

  private final Pattern pattern;

  public RegexFixedFormatMatchPattern(String regex) {
    try {
      this.pattern = Pattern.compile(regex);
    } catch (PatternSyntaxException e) {
      throw new FixedFormatException("Invalid regex pattern: " + regex, e);
    }
  }

  @Override
  public boolean matches(String line) {
    if (line == null) {
      return false;
    }
    return pattern.matcher(line).find();
  }
}
