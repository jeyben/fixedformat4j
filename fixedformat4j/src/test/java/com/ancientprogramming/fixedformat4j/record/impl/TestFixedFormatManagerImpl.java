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
package com.ancientprogramming.fixedformat4j.record.impl;

import junit.framework.TestCase;
import junit.framework.Assert;
import com.ancientprogramming.fixedformat4j.record.MyRecord;
import com.ancientprogramming.fixedformat4j.record.FixedFormatManager;

/**
 * todo: comment needed
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestFixedFormatManagerImpl extends TestCase {

  private static String STR = "some text ";

  public static final String MY_RECORD_DATA = "some text 0012320080514CT001100000010350000002056";

  FixedFormatManager manager = null;

  public void testLoadRecord() {
    manager = new FixedFormatManagerImpl();
    MyRecord loadedRecord = manager.load(MyRecord.class, MY_RECORD_DATA);
    Assert.assertNotNull(loadedRecord);
    Assert.assertEquals(STR, loadedRecord.getStringData());
    Assert.assertTrue(loadedRecord.getBooleanData());
  }
}
