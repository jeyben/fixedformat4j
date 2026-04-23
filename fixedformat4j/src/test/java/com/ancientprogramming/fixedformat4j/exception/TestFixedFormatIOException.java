package com.ancientprogramming.fixedformat4j.exception;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatIOException {

  @Test
  void isAFixedFormatException() {
    assertInstanceOf(FixedFormatException.class, new FixedFormatIOException("msg", new IOException()));
  }

  @Test
  void preservesCause() {
    IOException cause = new IOException("disk error");
    FixedFormatIOException ex = new FixedFormatIOException("could not read", cause);
    assertSame(cause, ex.getCause());
  }

  @Test
  void preservesMessage() {
    FixedFormatIOException ex = new FixedFormatIOException("could not read", new IOException());
    assertEquals("could not read", ex.getMessage());
  }
}
