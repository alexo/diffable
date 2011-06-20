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

import org.junit.Test;

import com.google.diffable.diff.vcdiff.hash.Hasher;

import static org.junit.Assert.*;

public class TestDictionary {

	private class FakeHasher extends Hasher {
		private int currentHash = 1;
		
		public long hash(String toHash) {
			return currentHash++;
		}
	}
	
	@Test
	public void testPut() {
		Dictionary dict = new Dictionary();
		dict.put(1L, new Block("abc", 0));
		Block b = dict.getMatch(0L, 3, "");
		assertNull(b);
		b = dict.getMatch(1L, 3, "abc");
		assertNotNull(b);
		assertEquals(0, b.getOffset());
		assertEquals("abc", b.getText());
	}
	
	@Test
	public void testLongestBlockTextMatch() {
		FakeHasher fake = new FakeHasher();
		Dictionary dict = new Dictionary();
		BlockText text = new BlockText("abcdef", 3);
		
		dict.populateDictionary(text, fake);
		Block b = dict.getMatch(2, 3, "def");
		assertNotNull(b);
		assertEquals("def", b.getText());
		assertEquals(3, b.getOffset());
		
		b = dict.getMatch(1, 3, "abcdef");
		assertNotNull(b);
		assertEquals("abcdef", b.getText());
		assertEquals(0, b.getOffset());
	}
}
