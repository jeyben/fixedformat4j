package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

@FunctionalInterface
public interface ParseErrorHandler {
  void handle(long lineNumber, String line, FixedFormatException cause);
}
