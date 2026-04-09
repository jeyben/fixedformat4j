package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.annotation.Align;

/**
 * Test fixture for repeating @Field with array return types.
 *
 * Layout (1-based offsets):
 *   positions  1-15 : 3 x String (length=5, LEFT aligned, space-padded)
 *   positions 16-25 : 2 x Integer (length=5, RIGHT aligned, zero-padded)
 */
@Record
public class RepeatingFieldRecord {

  private String[] codes;
  private Integer[] amounts;

  @Field(offset = 1, length = 5, count = 3)
  public String[] getCodes() {
    return codes;
  }

  public void setCodes(String[] codes) {
    this.codes = codes;
  }

  @Field(offset = 16, length = 5, count = 2, align = Align.RIGHT, paddingChar = '0')
  public Integer[] getAmounts() {
    return amounts;
  }

  public void setAmounts(Integer[] amounts) {
    this.amounts = amounts;
  }
}
