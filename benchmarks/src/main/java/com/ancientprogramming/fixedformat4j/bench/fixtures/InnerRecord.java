package com.ancientprogramming.fixedformat4j.bench.fixtures;

import com.ancientprogramming.fixedformat4j.annotation.*;

@Record
public class InnerRecord {

    public static final String SAMPLE_DATA = "Hello     000042";

    private String name;
    private Integer value;

    public static InnerRecord sampleInstance() {
        InnerRecord r = new InnerRecord();
        r.setName("Hello");
        r.setValue(42);
        return r;
    }

    @Field(offset = 1, length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Field(offset = 11, length = 6, align = Align.RIGHT, paddingChar = '0')
    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
