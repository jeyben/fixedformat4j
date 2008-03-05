/*
 * Copyright 2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancientprogramming.fixedformat4j;

import junit.framework.TestCase;
import junit.framework.Assert;
import com.ancientprogramming.fixedformat4j.record.RecordFactory;
import com.ancientprogramming.fixedformat4j.record.MyRecord;
import com.ancientprogramming.fixedformat4j.record.MyFieldRecord;

import java.util.Date;
import java.util.Calendar;

import org.apache.commons.lang.time.DateUtils;

/**
 * todo: comment needed
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestRecordFactory extends TestCase {

  public void testRecordFactory() throws Exception{
    MyRecord record = (MyRecord) RecordFactory.createInstance(MyRecord.class);
    record = (MyRecord) RecordFactory.createInstance(MyRecord.class);

    //test that null is returned in case we are out of bound
    Assert.assertNull(record.getStringData());
    Assert.assertNull(record.getIntegerData());
    Assert.assertNull(record.getDateData());
    Assert.assertNull(record.getCharacterData());
    Assert.assertNull(record.getBooleanData());
    Assert.assertNull(record.getLongData());
    Assert.assertNull(record.getDoubleData());
    Assert.assertNull(record.getFloatData());

    String stringValue = "string";
    record.setStringData(stringValue);

    Integer intValue = 10;
    record.setIntegerData(intValue);

    Date dateValue = DateUtils.round(new Date(), Calendar.DATE);
    record.setDateData(dateValue);

    Character characterValue = 'N';
    record.setCharacterData(characterValue);

    Boolean booleanValue = true;
    record.setBooleanData(booleanValue);

    Long longValue = 123L;
    record.setLongData(longValue);

    Double doubleValue = 123.0;
    record.setDoubleData(doubleValue);

    Float floatValue = 123.0F;
    record.setFloatData(floatValue);

    Assert.assertEquals(stringValue, record.getStringData());
    Assert.assertEquals(intValue, record.getIntegerData());
    Assert.assertEquals(dateValue, record.getDateData());
    Assert.assertEquals(characterValue, record.getCharacterData());
    Assert.assertEquals(booleanValue, record.getBooleanData());
    Assert.assertEquals(longValue, record.getLongData());
    Assert.assertEquals(doubleValue, record.getDoubleData());
    Assert.assertEquals(floatValue, record.getFloatData());

    System.out.println(record.export());
  }

  public void testFieldRecord() throws Exception {
    MyFieldRecord record = (MyFieldRecord) RecordFactory.createInstance(MyFieldRecord.class);
    record.setIntegerData(123);
    record.setStringData("something");
    Assert.assertEquals("0000000123something ", record.export());

  }
}