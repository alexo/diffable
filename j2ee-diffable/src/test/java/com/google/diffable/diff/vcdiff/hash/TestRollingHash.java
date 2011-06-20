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
package com.google.diffable.diff.vcdiff.hash;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.google.diffable.config.DiffableModule;
import com.google.diffable.diff.vcdiff.hash.RollingHash;
import com.google.inject.Guice;

import static org.junit.Assert.*;

public class TestRollingHash {
	private RollingHash hash;
	private RollingHash check;
	private Properties props;
	
	@Before
	public void setUp() {
		this.props = new Properties();
		hash = 
			Guice.createInjector(new DiffableModule())
				.getInstance(RollingHash.class);
		check = 
			Guice.createInjector(new DiffableModule())
				.getInstance(RollingHash.class);
	}
	
	private void createHashes() {
		hash = 
			Guice.createInjector(new DiffableModule(this.props))
				.getInstance(RollingHash.class);
		check = 
			Guice.createInjector(new DiffableModule(this.props))
				.getInstance(RollingHash.class);
	}

	@Test
	public void testHashFunctionSmall() {
		props.put("PrimeBase", "3");
		createHashes();
		assertEquals(1266L, hash.hash("abc"));
	}
	
	@Test
	public void testHashFunctionLarge() {
		assertEquals(232878305L, hash.hash("abcabc"));
	}
	
	@Test
	public void testRolling() {
		hash.hash("abcg");
		assertEquals(check.hash("bcgr"), hash.nextHash('r'));
		assertEquals(check.hash("cgrz"), hash.nextHash('z'));
		assertEquals(check.hash("grzQ"), hash.nextHash('Q'));
	}
	
	@Test
	public void testLargeRolling() {
		hash.hash("abcdefghijklmnopqrstuvwxyz");
		assertEquals(check.hash("bcdefghijklmnopqrstuvwxyza"), hash.nextHash('a'));
		assertEquals(check.hash("cdefghijklmnopqrstuvwxyzab"), hash.nextHash('b'));
		assertEquals(check.hash("defghijklmnopqrstuvwxyzabQ"), hash.nextHash('Q'));
	}
}
