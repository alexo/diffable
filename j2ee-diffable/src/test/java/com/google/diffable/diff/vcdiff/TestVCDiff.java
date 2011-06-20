/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.diffable.diff.vcdiff;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.google.diffable.config.DiffableModule;
import com.google.diffable.diff.Differ;
import com.google.inject.Guice;

import static org.junit.Assert.*;

public class TestVCDiff {
	private Differ vcdiff;
	private Properties props;
	
	@Before
	public void setUp() {
		vcdiff = 
			Guice.createInjector(new DiffableModule())
				.getInstance(VCDiff.class);
		this.props = new Properties();
	}
		
	private void createDiffer() {
		vcdiff = 
			Guice.createInjector(new DiffableModule(this.props))
				.getInstance(VCDiff.class);
	}
		
	@Test
	public void testNoMatchOnlyAddSmallTarget() {
		String diff = vcdiff.getDiffAsString("abc", "d");
		assertEquals("[\"d\",]", diff);
	}
	
	@Test
	public void testNoMatchOnlyAdd() {
		String diff = vcdiff.getDiffAsString("abc", "defghijk");
		assertEquals("[\"defghijk\",]", diff);
	}
	
	@Test
	public void testOnlyCopy() {
		props.put("BlockSize", "3");
		createDiffer();
		String diff = vcdiff.getDiffAsString("abcdef", "abcdef");
		assertNull(diff);
	}
	
	@Test
	public void testAddThenCopy() {
		props.put("BlockSize", "3");
		createDiffer();
		String diff = vcdiff.getDiffAsString("abc", "defabc");
		assertEquals("[\"def\",0,3,]", diff);
	}
	
	@Test
	public void testCopyAddCopy() {
		props.put("BlockSize", "3");
		createDiffer();
		String diff = vcdiff.getDiffAsString("abcdef", "defghiabc");
		assertEquals("[3,3,\"ghi\",0,3,]", diff);
	}
}
