package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;

@Record(length = 10)
public class FiveCharRecord {
  private String code;

  @Field(offset = 1, length = 5)
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
}
