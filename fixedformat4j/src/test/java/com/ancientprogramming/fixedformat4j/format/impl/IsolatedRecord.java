package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;

/**
 * Minimal @Record fixture used in classloader-leak tests.
 * Loaded by an isolated child classloader so its lifecycle can be tracked.
 */
@Record
public class IsolatedRecord {

  private String data;

  @Field(offset = 1, length = 10)
  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }
}
