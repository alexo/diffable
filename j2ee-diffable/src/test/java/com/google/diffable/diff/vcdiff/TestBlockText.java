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

import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestBlockText {
	
	@Test
	public void testBlocksizeLargerThanText() {
		BlockText bt = new BlockText("abc", 5);
		assertEquals(1, bt.getBlocks().size());
		assertEquals("abc", bt.getBlocks().get(0).getText());
		assertEquals(0, bt.getBlocks().get(0).getOffset());
	}
	
	@Test
	public void testThreeBlocks() {
		BlockText bt = new BlockText("abcdefghi", 3);
		assertEquals(3, bt.getBlocks().size());
	}
	
	@Test
	public void testThreeBlocksIterator() {
		BlockText bt = new BlockText("abcdefgh", 3);
		Iterator<Block> itr = bt.iterator();
		
		assertTrue(itr.hasNext());
		Block first = itr.next();
		assertEquals("abc", first.getText());
		assertEquals(0, first.getOffset());
		
		assertTrue(itr.hasNext());
		Block second = itr.next();
		assertEquals("def", second.getText());
		assertEquals(3, second.getOffset());
		
		assertTrue(itr.hasNext());
		Block third = itr.next();
		assertEquals("gh", third.getText());
		assertEquals(6, third.getOffset());
		
		assertFalse(itr.hasNext());
	}
}
