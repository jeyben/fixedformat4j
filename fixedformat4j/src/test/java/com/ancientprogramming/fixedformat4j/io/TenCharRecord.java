package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;

@Record(length = 10)
public class TenCharRecord {
  private String value;

  @Field(offset = 1, length = 10)
  public String getValue() { return value; }
  public void setValue(String value) { this.value = value; }
}
