package com.ancientprogramming.fixedformat4j.micrometer;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;

/**
 * Fixture record used by the instrumentation tests.
 */
@Record
public class BasicRecord {

  private String text;
  private Integer amount;

  @Field(offset = 1, length = 10)
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  @Field(offset = 11, length = 5, align = Align.RIGHT, paddingChar = '0')
  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }
}
