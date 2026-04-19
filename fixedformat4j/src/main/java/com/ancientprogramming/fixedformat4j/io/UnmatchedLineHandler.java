package com.ancientprogramming.fixedformat4j.io;

@FunctionalInterface
public interface UnmatchedLineHandler {
  void handle(long lineNumber, String line);
}
