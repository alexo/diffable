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
import java.util.HashMap;
import java.util.List;

import com.google.diffable.diff.vcdiff.hash.Hasher;

/**
 * The Dictionary class encapsulates the functionality of a dictionary
 * in the VCDiff algorithm.  It stores offsets into the dictionary text
 * keyed off of the finger print of the text block at that offset. It
 * also stores the text itself so that, given a finger print, it will
 * return the longest match from the original text.
 * 
 * @author joshua Harrison
 *
 */
class Dictionary {
	private BlockText dictionaryText;
	private final HashMap<Long, DictionaryEntry> dictionary = 
		new HashMap<Long, DictionaryEntry>();
	
	/**
	 * The private DictionaryEntry class is used to store colliding text
	 * blocks from the original text.  All entries in the Dictionary have
	 * a DictionaryEntry as their value, and this class simply stores
	 * multiple offsets if two blocks have the same finger print. It is
	 * private because the user does not need to know how collisions are
	 * handled, but only whether there is any matching text string, given
	 * a finger print, in the original text.
	 * 
	 * @author joshua Harrison
	 *
	 */
	private class DictionaryEntry {
		private ArrayList<Block> blocks =
			new ArrayList<Block>();
		
		private void addBlock(Block block) {
			this.blocks.add(block);
		}
		
		private List<Block> getBlocks() {
			return Collections.unmodifiableList(blocks);
		}
	}
	
	public void put(Long key, Block block) {
		if(!dictionary.containsKey(key)) {
			dictionary.put(key, new DictionaryEntry());
		}
		dictionary.get(key).addBlock(block);
	}
	
	public void populateDictionary(BlockText dictText, Hasher hasher) {
		dictionary.clear();
		this.dictionaryText = dictText;
		for(Block b : dictText) {
			put(hasher.hash(b.getText()), b);
		}
	}
	
	/**
	 * Returns the longest possible matching block from the dictionary or null
	 * if no match is found. The passed in hash is checked against stored finger
	 * prints. If a finger print match is found then all corresponding blocks are
	 * checked for a match. 
	 * 
	 * If a matching block is found, the nextBlock attribute
	 * is null, and the dictionary has dictionaryText, then target text is compared
	 * against dictionary text continuing after the matching block to obtain the
	 * longest possible string match. 
	 * 
	 * If there is no dictionary text but the block has a nextBlock, then the block 
	 * was added using an explicit put, and the nextBlock() method is used to find 
	 * the longest possible match. 
	 * 
	 * Otherwise, the matching block is returned.
	 * 
	 * @param hash The hash of the current target block.
	 * @param blockSize The size of the current target block used to create the hash.
	 * @param target The target text starting with the current block and containing
	 * the rest of the target text to allow for matching beyond the current block.
	 * @return
	 */
	public Block getMatch(long hash, int blockSize, String target) {
		if(dictionary.containsKey(hash)) {
			for(Block b : dictionary.get(hash).getBlocks()) {
				if(b.getText().equals(target.substring(0, blockSize))) {
					// If there is dictionaryText (via populateDictionary) and the block we are checking does
					// not have a nextBlock, indicating it is part of a BlockText, then use the BlockText's
					// original text to find the longest match with the target.
					if(dictionaryText != null && b.getNextBlock() == null) {
						String dictText = dictionaryText.getOriginalText().substring(b.getOffset() + blockSize);
						String targetText = target.substring(blockSize);
						// If either the dictionary or target text has no further charaters to check, return the
						// matching block.
						if(dictText.length() == 0 || targetText.length() == 0)
							return b;
						int currentPointer = 0;
						while(currentPointer < dictText.length() && currentPointer < targetText.length() && 
							  dictText.charAt(currentPointer) == targetText.charAt(currentPointer)) {
							currentPointer++;
						}
						return new Block(b.getText() + dictText.substring(0, currentPointer), b.getOffset());
					} else if (b.getNextBlock() != null){
						return b;
					} else {
						return b;
					}
				}
			}
			return null;
		}
		return null;
	}
}
