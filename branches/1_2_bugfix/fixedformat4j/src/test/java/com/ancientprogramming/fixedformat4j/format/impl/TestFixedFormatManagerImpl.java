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
package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.ParseException;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.format.impl.MultibleFieldsRecord;
import com.ancientprogramming.fixedformat4j.format.impl.MyRecord;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Calendar;
import java.math.BigDecimal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestFixedFormatManagerImpl extends TestCase {

  private static final Log LOG = LogFactory.getLog(TestFixedFormatManagerImpl.class);

  private static String STR = "some text ";

  public static final String MY_RECORD_DATA = "some text 0012320080514CT001100000010350000002056-0012 0120";
  public static final String MULTIBLE_RECORD_DATA = "some      2008101320081013                       0100";
  public static final String MULTIBLE_RECORD_DATA_X_PADDED = "some      2008101320081013xxxxxxxxxxxxxxxxxxxxxxx0100";

  FixedFormatManager manager = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    manager = new FixedFormatManagerImpl();
  }

  public void testLoadRecord() {
    MyRecord loadedRecord = manager.load(MyRecord.class, MY_RECORD_DATA);
    Assert.assertNotNull(loadedRecord);
    Assert.assertEquals(STR, loadedRecord.getStringData());
    Assert.assertTrue(loadedRecord.isBooleanData());
  }

  public void testLoadMultibleFieldsRecord() {
    //when reading data having multible field annotations the first field will decide what data to return
    Calendar someDay = Calendar.getInstance();
    someDay.set(2008, 9, 13, 0, 0, 0);
    someDay.set(Calendar.MILLISECOND, 0);
    MultibleFieldsRecord loadedRecord = manager.load(MultibleFieldsRecord.class, MULTIBLE_RECORD_DATA);
    Assert.assertNotNull(loadedRecord);
    Assert.assertEquals("some      ", loadedRecord.getStringData());
    Assert.assertEquals(someDay.getTime(), loadedRecord.getDateData());
  }

  public void testExportRecordObject() {
    Calendar someDay = Calendar.getInstance();
    someDay.set(2008, 4, 14, 0, 0, 0);
    someDay.set(Calendar.MILLISECOND, 0);

    MyRecord myRecord = new MyRecord();
    myRecord.setBooleanData(true);
    myRecord.setCharData('C');
    myRecord.setDateData(someDay.getTime());
    myRecord.setDoubleData(10.35);
    myRecord.setFloatData(20.56F);
    myRecord.setLongData(11L);
    myRecord.setIntegerData(123);
    myRecord.setStringData("some text ");
    myRecord.setBigDecimalData(new BigDecimal(-12.012));
    Assert.assertEquals("wrong record exported", MY_RECORD_DATA, manager.export(myRecord));
  }

  public void testExportMultibleFieldRecordObject() {
    Calendar someDay = Calendar.getInstance();
    someDay.set(2008, 9, 13, 0, 0, 0);
    someDay.set(Calendar.MILLISECOND, 0);

    MultibleFieldsRecord multibleFieldsRecord = new MultibleFieldsRecord();
    multibleFieldsRecord.setDateData(someDay.getTime());
    multibleFieldsRecord.setStringData("some      ");
    multibleFieldsRecord.setIntegerdata(100);
    manager.export(multibleFieldsRecord);
    Assert.assertEquals("wrong record exported", MULTIBLE_RECORD_DATA, manager.export(multibleFieldsRecord));
  }

  public void testExportIntoExistingString() {
    Calendar someDay = Calendar.getInstance();
    someDay.set(2008, 9, 13, 0, 0, 0);
    someDay.set(Calendar.MILLISECOND, 0);

    MultibleFieldsRecord multibleFieldsRecord = new MultibleFieldsRecord();
    multibleFieldsRecord.setDateData(someDay.getTime());
    multibleFieldsRecord.setStringData("some      ");
    multibleFieldsRecord.setIntegerdata(100);
    String exportedString = manager.export("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", multibleFieldsRecord);
    Assert.assertEquals("wrong record exported", MULTIBLE_RECORD_DATA_X_PADDED, exportedString);
  }

  public void testLoadNonRecordAnnotatedClass() {
    try {
      manager.load(String.class, "some");
    } catch (FixedFormatException e) {
      //expected
    }
  }

  public void testParseFail() {
    try {
      manager.load(MyRecord.class, "foobarfoobarfoobarfoobar");
      fail("expected parse exception");
    } catch (ParseException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("bu", e);
      }
      //expected
    }
  }
}
