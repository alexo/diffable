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

import com.google.inject.ImplementedBy;

/**
 * This class defines the object which will handle the hash algorithm.
 * 
 * @author joshua Harrison
 */
@ImplementedBy(RollingHash.class)
public abstract class Hasher {
	public abstract long hash(String toHash);
	// If the hash implementation does not implement a rolling hash compatible
	// function, then calling nextHash returns -1, indicating that a new hash
	// value should be obtained by calling hash with the full value to hash.
	public long nextHash(char toAdd) {
		return -1;
	}
}
