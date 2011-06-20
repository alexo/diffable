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
package com.google.diffable.config;

import static org.easymock.EasyMock.*;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.inject.Guice;

import static org.junit.Assert.*;

public class TestBaseModule {
	private Properties props;
	private MessageProvider provider =
		new MessageProvider("com/google/diffable/bundles/");
	
	@Test
	public void testDefaultsNoProps() {
		BaseModule module = new BaseModule();
		ModuleTestInterface instance =
			Guice.createInjector(module).getInstance(ModuleTestInterface.class);
		
		assertTrue(instance instanceof DefaultImplementation);
		assertEquals("Default", instance.getInjectedValue());
	}
	
	@Test
	public void testDefaultImplementationModifiedValue() {
		props = new Properties();
		props.put("InjectedValue", "Modified");
		BaseModule module = new BaseModule(props);
		ModuleTestInterface instance =
			Guice.createInjector(module).getInstance(ModuleTestInterface.class);
		
		assertTrue(instance instanceof DefaultImplementation);
		assertEquals("Modified", instance.getInjectedValue());
	}
	
	@Test
	public void testOverrideImplementationDefaultValue() {
		props = new Properties();
		props.put("com.google.diffable.config.ModuleTestInterface",
				  "com.google.diffable.config.OverrideImplementation");
		BaseModule module = new BaseModule(props);
		ModuleTestInterface instance =
			Guice.createInjector(module).getInstance(ModuleTestInterface.class);
		
		assertTrue(instance instanceof OverrideImplementation);
		assertEquals("Override", instance.getInjectedValue());
	}
	
	@Test
	public void testOverrideImplementationOverrideValue() {
		props = new Properties();
		props.put("InjectedValue", "Modified");
		props.put("com.google.diffable.config.ModuleTestInterface",
		  		  "com.google.diffable.config.OverrideImplementation");
		BaseModule module = new BaseModule(props);
		ModuleTestInterface instance =
			Guice.createInjector(module).getInstance(ModuleTestInterface.class);
		
		assertTrue(instance instanceof OverrideImplementation);
		assertEquals("Modified", instance.getInjectedValue());
	}
	
	@Test
	public void testOverrideClassNotFound() {
		props = new Properties();
		props.put("com.google.diffable.config.ModuleTestInterface",
		  		  "com.google.diffable.config.DoesntExist");
		Logger mockLogger = createMock(Logger.class);
		String error = String.format(
			provider.getMessage(
				"ErrorMessages", "module.classnotfound"),
				"com.google.diffable.config.DoesntExist",
				"com.google.diffable.config.ModuleTestInterface");
		mockLogger.error(error);
		replay(mockLogger);
		BaseModule module = new BaseModule(props);
		module.setLogger(mockLogger);
		Guice.createInjector(module).getInstance(ModuleTestInterface.class);
	}
	
	@Test
	public void testOverrideDoesntImplement() {
		props = new Properties();
		props.put("com.google.diffable.config.ModuleTestInterface",
		  		  "com.google.diffable.config.TestBaseModule");
		Logger mockLogger = createMock(Logger.class);
		String error = String.format(
			provider.getMessage(
				"ErrorMessages", "module.notimplemented"),
				"com.google.diffable.config.TestBaseModule",
				"com.google.diffable.config.ModuleTestInterface");
		mockLogger.error(error);
		replay(mockLogger);
		BaseModule module = new BaseModule(props);
		module.setLogger(mockLogger);
		Guice.createInjector(module).getInstance(ModuleTestInterface.class);
	}
}
