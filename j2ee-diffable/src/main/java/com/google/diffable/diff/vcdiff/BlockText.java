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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Turns a string of text into blocks of text of size blockSize.  This class
 * is iterable over its blocks of text.
 * 
 * @author joshua Harrison
 *
 */
class BlockText implements Iterable<Block> {
	private final String originalText;
	private final int blockSize;
	private final ArrayList<Block> blocks =
		new ArrayList<Block>();
	
	/**
	 * The constructor accepts the text to turn into blocks, and the size each
	 * block should be.
	 * 
	 * @param originalText The text to divide into blocks of text.
	 * @param blockSize The size each text block should be.
	 */
	public BlockText(String originalText, int blockSize) {
		this.originalText = originalText;
		this.blockSize = blockSize;
		
		for(int i=0; i<originalText.length(); i += blockSize) {
			int endIndex = (i+blockSize) >= originalText.length() ?
	                       originalText.length() : i + blockSize;
			blocks.add(new Block(originalText.substring(i, endIndex), i));
		}
	}
	
	public Iterator<Block> iterator() {
		return blocks.iterator();
	}
	
	public List<Block> getBlocks() {
		return Collections.unmodifiableList(blocks);
	}
	
	public String getOriginalText() {
		return originalText;
	}
	
	public int getBlockSize() {
		return blockSize;
	}
}
