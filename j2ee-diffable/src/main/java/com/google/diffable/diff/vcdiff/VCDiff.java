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

import com.google.diffable.diff.Differ;
import com.google.diffable.diff.JSONHelper;
import com.google.diffable.diff.vcdiff.hash.Hasher;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This class defines the Differ which use the VCDiff algorithm.
 *  
 * @author joshua Harrison
 */
public class VCDiff implements Differ {
	
	@Inject(optional=true) @Named("BlockSize")
	public int blockSize = 20;
	
	private Hasher hash;
	private Dictionary dictText;
	
	@Inject
	public VCDiff(Hasher hash,
			      Dictionary dictText) {
		this.hash = hash;
		this.dictText = dictText;
	}
	
	public String getDiffAsString(String dict, String target) {
		// If the strings are the exact same, return null for no diff.
		if (dict.equals(target)) {
			return null;
		}
		ArrayList<String> diffString = new ArrayList<String>(); 

		dictText.populateDictionary(new BlockText(dict, blockSize), hash);
		//StringBuilder targetBuffer = new StringBuilder(target);
		int targetLength = target.length();
		int targetIndex = 0;
		
		long currentHash = -1;
		StringBuilder addBuffer = new StringBuilder();
		
		// Increment the target index as characters are read from the target
		// and matched or added.
		while(targetIndex < targetLength) {
			// If the target is less than or equal to the block size add the
			// remainder of the target and addBuffer to the diff as an add 
			// command and finish.
			if(targetLength - targetIndex < blockSize) {
				diffString.add(
					JSONHelper.quote(
						addBuffer.toString() +
						target.substring(targetIndex, targetLength)));
				break;
			} else {
				if (currentHash == -1) {
					currentHash = hash.hash(target.substring(targetIndex,
							                targetIndex + blockSize));
				} else {
					currentHash =
						hash.nextHash(
							target.charAt(targetIndex + (blockSize-1)));
					// If the current hash is less than 0, than this hasher does
					// not implement rolling, and the value must be hashed over
					// again.
					if(currentHash < 0)
						currentHash =
							hash.hash(
								target.substring(0, targetIndex + blockSize));
				}
				
				Block match = dictText.getMatch(currentHash,
						                        blockSize, 
						                        target.substring(targetIndex));
				// If there's no match, remove the first character from the 
				// target and roll the hash using the next character at the
				// blockSize position.
				if(match == null) {
					addBuffer.append(target.charAt(targetIndex++));
				// Otherwise, add the match as a copy command and remove the
			    // appropriate number of characters from the target.
				} else {
					// If there were any non-matching characters in the 
					// addBuffer, add them as an "add" command to the diff, and
					// clear the buffer.
					if(addBuffer.length() > 0) {
						diffString.add(JSONHelper.quote(addBuffer.toString()));
						addBuffer = new StringBuilder();
					}
					diffString.add(Integer.toString(match.getOffset()));
					diffString.add(Integer.toString(match.getText().length()));
					targetIndex += match.getText().length();
					// Make sure to reset the currentHash since rolling it is
					// only valid when moving one character at a time.
					currentHash = -1;
				}
			}
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		for (String data : diffString) {
			buffer.append(data).append(",");
		}
		buffer.append("]");
		return buffer.toString();
	}
}
