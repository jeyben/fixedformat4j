package com.ancientprogramming.fixedformat4j.bench.fixtures;

import com.ancientprogramming.fixedformat4j.annotation.*;

import java.math.BigDecimal;

@Record
public class WideRecord {

    public static final String SAMPLE_DATA =
        "fieldone  " + "fieldtwo  " + "fieldthree" + "fieldfour " + "fieldfive " +
        "fieldsix  " + "fieldsevn " + "fieldeght " + "fieldnine " + "fieldten  " +
        "0000000001" + "0000000002" + "0000000003" + "0000000004" + "0000000005" +
        "000000010000" + "000000020000" + "000000030000" +
        "TF";

    private String str1, str2, str3, str4, str5, str6, str7, str8, str9, str10;
    private Integer int1, int2, int3, int4, int5;
    private BigDecimal dec1, dec2, dec3;
    private Boolean bool1, bool2;

    public static WideRecord sampleInstance() {
        WideRecord r = new WideRecord();
        r.setStr1("fieldone");  r.setStr2("fieldtwo");  r.setStr3("fieldthree");
        r.setStr4("fieldfour"); r.setStr5("fieldfive"); r.setStr6("fieldsix");
        r.setStr7("fieldsevn"); r.setStr8("fieldeght"); r.setStr9("fieldnine");
        r.setStr10("fieldten");
        r.setInt1(1); r.setInt2(2); r.setInt3(3); r.setInt4(4); r.setInt5(5);
        r.setDec1(new BigDecimal("100.00"));
        r.setDec2(new BigDecimal("200.00"));
        r.setDec3(new BigDecimal("300.00"));
        r.setBool1(true);
        r.setBool2(false);
        return r;
    }

    @Field(offset = 1,   length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getStr1()  { return str1; }
    public void setStr1(String v)  { str1 = v; }

    @Field(offset = 11,  length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getStr2()  { return str2; }
    public void setStr2(String v)  { str2 = v; }

    @Field(offset = 21,  length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getStr3()  { return str3; }
    public void setStr3(String v)  { str3 = v; }

    @Field(offset = 31,  length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getStr4()  { return str4; }
    public void setStr4(String v)  { str4 = v; }

    @Field(offset = 41,  length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getStr5()  { return str5; }
    public void setStr5(String v)  { str5 = v; }

    @Field(offset = 51,  length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getStr6()  { return str6; }
    public void setStr6(String v)  { str6 = v; }

    @Field(offset = 61,  length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getStr7()  { return str7; }
    public void setStr7(String v)  { str7 = v; }

    @Field(offset = 71,  length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getStr8()  { return str8; }
    public void setStr8(String v)  { str8 = v; }

    @Field(offset = 81,  length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getStr9()  { return str9; }
    public void setStr9(String v)  { str9 = v; }

    @Field(offset = 91,  length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getStr10() { return str10; }
    public void setStr10(String v) { str10 = v; }

    @Field(offset = 101, length = 10, align = Align.RIGHT, paddingChar = '0')
    public Integer getInt1() { return int1; }
    public void setInt1(Integer v) { int1 = v; }

    @Field(offset = 111, length = 10, align = Align.RIGHT, paddingChar = '0')
    public Integer getInt2() { return int2; }
    public void setInt2(Integer v) { int2 = v; }

    @Field(offset = 121, length = 10, align = Align.RIGHT, paddingChar = '0')
    public Integer getInt3() { return int3; }
    public void setInt3(Integer v) { int3 = v; }

    @Field(offset = 131, length = 10, align = Align.RIGHT, paddingChar = '0')
    public Integer getInt4() { return int4; }
    public void setInt4(Integer v) { int4 = v; }

    @Field(offset = 141, length = 10, align = Align.RIGHT, paddingChar = '0')
    public Integer getInt5() { return int5; }
    public void setInt5(Integer v) { int5 = v; }

    @Field(offset = 151, length = 12, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
    public BigDecimal getDec1() { return dec1; }
    public void setDec1(BigDecimal v) { dec1 = v; }

    @Field(offset = 163, length = 12, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
    public BigDecimal getDec2() { return dec2; }
    public void setDec2(BigDecimal v) { dec2 = v; }

    @Field(offset = 175, length = 12, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
    public BigDecimal getDec3() { return dec3; }
    public void setDec3(BigDecimal v) { dec3 = v; }

    @Field(offset = 187, length = 1)
    @FixedFormatBoolean(trueValue = "T", falseValue = "F")
    public Boolean getBool1() { return bool1; }
    public void setBool1(Boolean v) { bool1 = v; }

    @Field(offset = 188, length = 1)
    @FixedFormatBoolean(trueValue = "T", falseValue = "F")
    public Boolean getBool2() { return bool2; }
    public void setBool2(Boolean v) { bool2 = v; }
}
