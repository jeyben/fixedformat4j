package com.ancientprogramming.fixedformat4j.bench.fixtures;

import com.ancientprogramming.fixedformat4j.annotation.*;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

@Record
public class RandomLargeRecord {

    public static final String SAMPLE_DATA =
        "X" + "1" + "BENCH   " + "0017" + "20240601" + "0000000500075" +
        "00000042" + "Fixed Format Test   " + "0000000950" + "001234567890" +
        "F" + "000000000031250" + "103000" + "  CATEGORY" + "M" +
        "0000000001234567" + "00999" + "Perf Benchmark           " +
        "000012345" + "00000987654321" + "00000550" + "15012024" +
        "Y" + "0000009999" + "REF-001     " + "00000000001250000" +
        "0000077" + "Z" + "0000000456789" + "Benchmark Notes";

    private Character typeCode;
    private Boolean flag1;
    private String label;
    private Short smallCount;
    private Date date1;
    private BigDecimal amount1;
    private Integer count1;
    private String description1;
    private Float float1;
    private Long long1;
    private Boolean flag2;
    private Double double1;
    private Date time1;
    private String category;
    private Character marker;
    private BigDecimal amount2;
    private Short smallCount2;
    private String description2;
    private Integer count2;
    private Long long2;
    private Float float2;
    private Date date2;
    private Boolean flag3;
    private BigDecimal amount3;
    private String reference;
    private Double double2;
    private Integer count3;
    private Character suffix;
    private BigDecimal amount4;
    private String notes;

    public static RandomLargeRecord sampleInstance() {
        RandomLargeRecord r = new RandomLargeRecord();
        r.setTypeCode('X');
        r.setFlag1(true);
        r.setLabel("BENCH");
        r.setSmallCount((short) 17);
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2024, Calendar.JUNE, 1, 0, 0, 0);
        cal1.set(Calendar.MILLISECOND, 0);
        r.setDate1(cal1.getTime());
        r.setAmount1(new BigDecimal("5000.75"));
        r.setCount1(42);
        r.setDescription1("Fixed Format Test");
        r.setFloat1(9.5f);
        r.setLong1(1234567890L);
        r.setFlag2(false);
        r.setDouble1(3.125);
        Calendar timeCal = Calendar.getInstance();
        timeCal.set(Calendar.HOUR_OF_DAY, 10);
        timeCal.set(Calendar.MINUTE, 30);
        timeCal.set(Calendar.SECOND, 0);
        timeCal.set(Calendar.MILLISECOND, 0);
        r.setTime1(timeCal.getTime());
        r.setCategory("CATEGORY");
        r.setMarker('M');
        r.setAmount2(new BigDecimal("12.34567"));
        r.setSmallCount2((short) 999);
        r.setDescription2("Perf Benchmark");
        r.setCount2(12345);
        r.setLong2(987654321L);
        r.setFloat2(5.5f);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2024, Calendar.JANUARY, 15, 0, 0, 0);
        cal2.set(Calendar.MILLISECOND, 0);
        r.setDate2(cal2.getTime());
        r.setFlag3(true);
        r.setAmount3(new BigDecimal("99.99"));
        r.setReference("REF-001");
        r.setDouble2(1.25);
        r.setCount3(77);
        r.setSuffix('Z');
        r.setAmount4(new BigDecimal("456.789"));
        r.setNotes("Benchmark Notes");
        return r;
    }

    @Field(offset = 1, length = 1, align = Align.LEFT, paddingChar = ' ')
    public Character getTypeCode() { return typeCode; }
    public void setTypeCode(Character v) { typeCode = v; }

    @Field(offset = 2, length = 1)
    @FixedFormatBoolean(trueValue = "1", falseValue = "0")
    public Boolean getFlag1() { return flag1; }
    public void setFlag1(Boolean v) { flag1 = v; }

    @Field(offset = 3, length = 8, align = Align.LEFT, paddingChar = ' ')
    public String getLabel() { return label; }
    public void setLabel(String v) { label = v; }

    @Field(offset = 11, length = 4, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public Short getSmallCount() { return smallCount; }
    public void setSmallCount(Short v) { smallCount = v; }

    @Field(offset = 15, length = 8)
    @FixedFormatPattern("yyyyMMdd")
    public Date getDate1() { return date1; }
    public void setDate1(Date v) { date1 = v; }

    @Field(offset = 23, length = 13, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
    public BigDecimal getAmount1() { return amount1; }
    public void setAmount1(BigDecimal v) { amount1 = v; }

    @Field(offset = 36, length = 8, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public Integer getCount1() { return count1; }
    public void setCount1(Integer v) { count1 = v; }

    @Field(offset = 44, length = 20, align = Align.LEFT, paddingChar = ' ')
    public String getDescription1() { return description1; }
    public void setDescription1(String v) { description1 = v; }

    @Field(offset = 64, length = 10, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
    public Float getFloat1() { return float1; }
    public void setFloat1(Float v) { float1 = v; }

    @Field(offset = 74, length = 12, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public Long getLong1() { return long1; }
    public void setLong1(Long v) { long1 = v; }

    @Field(offset = 86, length = 1)
    @FixedFormatBoolean(trueValue = "T", falseValue = "F")
    public Boolean getFlag2() { return flag2; }
    public void setFlag2(Boolean v) { flag2 = v; }

    @Field(offset = 87, length = 15, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 4, useDecimalDelimiter = false)
    public Double getDouble1() { return double1; }
    public void setDouble1(Double v) { double1 = v; }

    @Field(offset = 102, length = 6)
    @FixedFormatPattern("HHmmss")
    public Date getTime1() { return time1; }
    public void setTime1(Date v) { time1 = v; }

    @Field(offset = 108, length = 10, align = Align.RIGHT, paddingChar = ' ')
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }

    @Field(offset = 118, length = 1, align = Align.LEFT, paddingChar = ' ')
    public Character getMarker() { return marker; }
    public void setMarker(Character v) { marker = v; }

    @Field(offset = 119, length = 16, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 5, useDecimalDelimiter = false)
    public BigDecimal getAmount2() { return amount2; }
    public void setAmount2(BigDecimal v) { amount2 = v; }

    @Field(offset = 135, length = 5, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public Short getSmallCount2() { return smallCount2; }
    public void setSmallCount2(Short v) { smallCount2 = v; }

    @Field(offset = 140, length = 25, align = Align.LEFT, paddingChar = ' ')
    public String getDescription2() { return description2; }
    public void setDescription2(String v) { description2 = v; }

    @Field(offset = 165, length = 9, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public Integer getCount2() { return count2; }
    public void setCount2(Integer v) { count2 = v; }

    @Field(offset = 174, length = 14, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public Long getLong2() { return long2; }
    public void setLong2(Long v) { long2 = v; }

    @Field(offset = 188, length = 8, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
    public Float getFloat2() { return float2; }
    public void setFloat2(Float v) { float2 = v; }

    @Field(offset = 196, length = 8)
    @FixedFormatPattern("ddMMyyyy")
    public Date getDate2() { return date2; }
    public void setDate2(Date v) { date2 = v; }

    @Field(offset = 204, length = 1)
    @FixedFormatBoolean(trueValue = "Y", falseValue = "N")
    public Boolean getFlag3() { return flag3; }
    public void setFlag3(Boolean v) { flag3 = v; }

    @Field(offset = 205, length = 10, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
    public BigDecimal getAmount3() { return amount3; }
    public void setAmount3(BigDecimal v) { amount3 = v; }

    @Field(offset = 215, length = 12, align = Align.LEFT, paddingChar = ' ')
    public String getReference() { return reference; }
    public void setReference(String v) { reference = v; }

    @Field(offset = 227, length = 17, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 6, useDecimalDelimiter = false)
    public Double getDouble2() { return double2; }
    public void setDouble2(Double v) { double2 = v; }

    @Field(offset = 244, length = 7, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public Integer getCount3() { return count3; }
    public void setCount3(Integer v) { count3 = v; }

    @Field(offset = 251, length = 1, align = Align.LEFT, paddingChar = ' ')
    public Character getSuffix() { return suffix; }
    public void setSuffix(Character v) { suffix = v; }

    @Field(offset = 252, length = 13, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 3, useDecimalDelimiter = false)
    public BigDecimal getAmount4() { return amount4; }
    public void setAmount4(BigDecimal v) { amount4 = v; }

    @Field(offset = 265, length = 15, align = Align.LEFT, paddingChar = ' ')
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
}
