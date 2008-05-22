package com.ancientprogramming.fixedformat4j.record;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Fields;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;

import java.util.Date;

@Record
public class MultibleFieldsRecord {

  private String stringData;
  private Date dateData;
  private Integer integerdata;

  @Field(offset = 1, length = 10, align = Align.RIGHT, paddingChar = ' ')
  public String getStringData() {
    return stringData;
  }

  public void setStringData(String stringData) {
    this.stringData = stringData;
  }

  @Fields({@Field(offset = 11, length = 8), @Field(offset = 19, length = 8)})
  @FixedFormatPattern("yyyyMMdd")
  public Date getDateData() {
    return dateData;
  }

  public void setDateData(Date dateData) {
    this.dateData = dateData;
  }

  @Field(offset = 50, length = 4)
  public Integer getIntegerdata() {
    return integerdata;
  }

  public void setIntegerdata(Integer integerdata) {
    this.integerdata = integerdata;
  }
}
