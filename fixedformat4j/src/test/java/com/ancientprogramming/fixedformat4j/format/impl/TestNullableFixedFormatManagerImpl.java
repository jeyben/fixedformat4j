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

import java.math.BigDecimal;
import java.util.Calendar;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;

/**
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestNullableFixedFormatManagerImpl extends TestCase {
	private static final Log LOG = LogFactory.getLog(TestNullableFixedFormatManagerImpl.class);
	

	public static final String MY_NULLABLE_RECORD_DATA = "               20080514CT001100000010350000002056-0012 01200000002056";
	public static final String MY_NONNULL_RECORD_DATA = "**********0000020080514CT001100000010350000002056-0012 01200000002056";
	
	FixedFormatManager manager = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		manager = new FixedFormatManagerImpl();
	}

	public void testLoadNullableRecord() {
		MyNullableRecord loadedRecord = manager.load(MyNullableRecord.class, MY_NULLABLE_RECORD_DATA);
		Assert.assertNotNull(loadedRecord);
		Assert.assertEquals(null, loadedRecord.getStringData());
		Assert.assertTrue(loadedRecord.isBooleanData());
	}

	public void testLoadNonNullRecord() {
		MyNullableRecord loadedRecord = manager.load(MyNullableRecord.class, MY_NONNULL_RECORD_DATA);
		Assert.assertNotNull(loadedRecord);
		Assert.assertEquals("", loadedRecord.getStringData());
		Assert.assertTrue(loadedRecord.isBooleanData());
	}

	
	public void testExportNullableRecordObject() {
		MyNullableRecord myRecord = createMyNullableRecord();
		Assert.assertEquals(MY_NULLABLE_RECORD_DATA,manager.export(myRecord));
		myRecord = createMyNonNullRecord();
		Assert.assertEquals(MY_NONNULL_RECORD_DATA, manager.export(myRecord));
	}

	private MyNullableRecord createMyNullableRecord() {
		Calendar someDay = Calendar.getInstance();
		someDay.set(2008, 4, 14, 0, 0, 0);
		someDay.set(Calendar.MILLISECOND, 0);

		MyNullableRecord myRecord = new MyNullableRecord();
		myRecord.setBooleanData(true);
		myRecord.setCharData('C');
		myRecord.setDateData(someDay.getTime());
		myRecord.setDoubleData(10.35);
		myRecord.setFloatData(20.56F);
		myRecord.setLongData(11L);
		myRecord.setIntegerData(null);
		myRecord.setStringData(null);
		myRecord.setBigDecimalData(new BigDecimal(-12.012));
		myRecord.setSimpleFloatData(20.56F);
		return myRecord;
	}
	
	private MyNullableRecord createMyNonNullRecord() {
		Calendar someDay = Calendar.getInstance();
		someDay.set(2008, 4, 14, 0, 0, 0);
		someDay.set(Calendar.MILLISECOND, 0);

		MyNullableRecord myRecord = new MyNullableRecord();
		myRecord.setBooleanData(true);
		myRecord.setCharData('C');
		myRecord.setDateData(someDay.getTime());
		myRecord.setDoubleData(10.35);
		myRecord.setFloatData(20.56F);
		myRecord.setLongData(11L);
		myRecord.setIntegerData(0);
		myRecord.setStringData("");
		myRecord.setBigDecimalData(new BigDecimal(-12.012));
		myRecord.setSimpleFloatData(20.56F);
		return myRecord;
	}

}
