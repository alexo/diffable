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

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This class defines the Rolling hash algroithm.
 * 
 * @author joshua Harrison
 */
class RollingHash extends Hasher {

	@Inject(optional=true) @Named("PrimeBase")
	private int primeBase = 257;
	
	@Inject(optional=true) @Named("PrimeMod")
	private long primeMod = 1000000007;
	
	private long lastHash = 0;
	private long lastPower = 0;
	private StringBuffer lastString = null;
	
	private long moduloExp(int base, int power, long modulo) {
		long toReturn = 1;
		for(int i=0; i<power; i++) {
			toReturn = (base * toReturn) % modulo;
		}
		return toReturn;
	}
	
	public long hash(String toHash) {
		long hash = 0;
		for(int i=0; i<toHash.length(); i++) {
			hash += (toHash.charAt(i) * moduloExp(primeBase, toHash.length()-1-i, primeMod)) % primeMod;
			hash %= primeMod; 
		}
		lastPower = moduloExp(primeBase, toHash.length() - 1, primeMod); 
		lastString = new StringBuffer(toHash);
		lastHash = hash;
		return hash;
	}
	
	public long nextHash(char toAdd) {
		long hash = lastHash;
		hash -= (lastString.charAt(0) * lastPower);
		hash = hash * primeBase + toAdd;
		hash %= primeMod;
		if(hash < 0)
			hash += primeMod;
		lastString.deleteCharAt(0).append(toAdd);
		lastHash = hash;
		return hash;
	}
}
