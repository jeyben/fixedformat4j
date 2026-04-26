/*
 * Copyright 2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancientprogramming.fixedformat4j.format.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class TestCharacterFormatter {

  FixedFormatter formatter = new CharacterFormatter();

  @Test
  public void testParse() {
    assertEquals('J', formatter.parse("J", new FormatInstructions(1, Align.LEFT, ' ', null, null, null, null)));
    assertEquals('J', formatter.parse("JN", new FormatInstructions(2, Align.LEFT, ' ', null, null, null, null)));
    assertNull(formatter.parse("", new FormatInstructions(0, Align.LEFT, ' ', null, null, null, null)));
  }

  @Test
  public void testFormat() {
    assertEquals("J", formatter.format('J', new FormatInstructions(1, Align.LEFT, ' ', null, null, null, null)));
    assertEquals(" ", formatter.format(null, new FormatInstructions(1, Align.LEFT, ' ', null, null, null, null)));
  }

  @Test
  public void testWhitespaceCharRoundTrip() {
    // Use underscore as padding char so the space value is not stripped during parsing
    assertEquals(' ', formatter.parse(" ", new FormatInstructions(1, Align.LEFT, '_', null, null, null, null)));
    assertEquals(" ", formatter.format(' ', new FormatInstructions(1, Align.LEFT, '_', null, null, null, null)));
  }

  @Test
  public void testOnlyFirstCharUsedFromLongerString() {
    // When field contains several chars, only the first is returned
    assertEquals('A', formatter.parse("ABC", new FormatInstructions(3, Align.LEFT, ' ', null, null, null, null)));
  }

  @Test
  public void parseMultiCharInput_logsWarningWithOriginalStringAndTruncatedChar() {
    ListAppender<ILoggingEvent> appender = attachListAppender();
    try {
      formatter.parse("AB", new FormatInstructions(2, Align.LEFT, ' ', null, null, null, null));
      List<ILoggingEvent> events = appender.list;
      assertEquals(1, events.size(), "expected exactly one WARN log event");
      assertEquals(Level.WARN, events.get(0).getLevel());
      String message = events.get(0).getFormattedMessage();
      assertTrue(message.contains("AB"), "warn message should contain the original string");
      assertTrue(message.contains("A"), "warn message should contain the returned character");
    } finally {
      detachListAppender(appender);
    }
  }

  @Test
  public void parseSingleCharInput_doesNotLogWarning() {
    ListAppender<ILoggingEvent> appender = attachListAppender();
    try {
      formatter.parse("A", new FormatInstructions(1, Align.LEFT, ' ', null, null, null, null));
      assertTrue(appender.list.isEmpty(), "no WARN should be logged for single-char input");
    } finally {
      detachListAppender(appender);
    }
  }

  private ListAppender<ILoggingEvent> attachListAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(CharacterFormatter.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    return appender;
  }

  private void detachListAppender(ListAppender<ILoggingEvent> appender) {
    Logger logger = (Logger) LoggerFactory.getLogger(CharacterFormatter.class);
    logger.detachAppender(appender);
  }
}
