package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;

public class NoDefaultConstructorClass {

  private String someData;

  public NoDefaultConstructorClass(String someData) {
    this.someData = someData;
  }

  @Record
  static class MyStaticNestedClass {

    private String stringData;

    @Field(offset = 1, length = 10)
    public String getStringData() {
      return stringData;
    }

    public void setStringData(String stringData) {
      this.stringData = stringData;
    }
  }

  @Record
  class MyInnerClass {

    private String stringData;

    @Field(offset = 1, length = 10)
    public String getStringData() {
      return stringData;
    }

    public void setStringData(String stringData) {
      this.stringData = stringData;
    }
  }
}
