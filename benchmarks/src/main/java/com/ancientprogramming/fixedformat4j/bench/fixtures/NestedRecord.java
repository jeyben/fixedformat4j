package com.ancientprogramming.fixedformat4j.bench.fixtures;

import com.ancientprogramming.fixedformat4j.annotation.*;

@Record
public class NestedRecord {

    public static final String SAMPLE_DATA = "OUTERHello     000042";

    private String id;
    private InnerRecord inner;

    public static NestedRecord sampleInstance() {
        NestedRecord r = new NestedRecord();
        r.setId("OUTER");
        r.setInner(InnerRecord.sampleInstance());
        return r;
    }

    @Field(offset = 1, length = 5, align = Align.LEFT, paddingChar = ' ')
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Field(offset = 6, length = 16)
    public InnerRecord getInner() {
        return inner;
    }

    public void setInner(InnerRecord inner) {
        this.inner = inner;
    }
}
