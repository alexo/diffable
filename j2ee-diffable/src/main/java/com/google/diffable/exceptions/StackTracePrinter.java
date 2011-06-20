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
package com.google.diffable.exceptions;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This class defines the stacktrace printer
 * 
 * @author joshua Harrison
 */
public class StackTracePrinter {
	
	@Inject(optional=true) @Named("PrintStackTraces")
	private boolean printStackTraces = false;
	
	public void setPrintStackTraces(boolean printStackTraces) {
		this.printStackTraces = printStackTraces;
	}
	
	public void print(Throwable exc) {
		if (printStackTraces) {
			exc.printStackTrace();
		}
	}
}
