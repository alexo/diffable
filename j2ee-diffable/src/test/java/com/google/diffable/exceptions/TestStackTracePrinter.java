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

import java.util.Properties;

import org.junit.Test;

import com.google.diffable.config.BaseModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

import static org.easymock.EasyMock.*;

public class TestStackTracePrinter {

	@Test
	public void testDontPrintTraces() {
		StackTracePrinter printer =
			Guice.createInjector(
				new BaseModule()).getInstance(StackTracePrinter.class);
		ResourceManagerException exc = new ResourceManagerException("");
		printer.print(exc);
	}
	
	@Test
	public void testPrintTraces() {
		StackTracePrinter printer =
			Guice.createInjector(new AbstractModule() {
				@Override
				protected void configure() {
					Properties props = new Properties();
					props.put("PrintStackTraces", "true");
					Names.bindProperties(binder(), props);
				}
			}).getInstance(StackTracePrinter.class);
		ResourceManagerException exc =
			createMock(ResourceManagerException.class);
		exc.printStackTrace();
		replay(exc);
		printer.print(exc);
		verify(exc);
	}
}
