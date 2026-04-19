package com.ancientprogramming.fixedformat4j.exception;

import java.io.IOException;

public class FixedFormatIOException extends FixedFormatException {

  public FixedFormatIOException(String message, IOException cause) {
    super(message, cause);
  }
}
