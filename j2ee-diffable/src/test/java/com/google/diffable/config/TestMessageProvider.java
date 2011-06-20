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

import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

import static org.junit.Assert.*;

import static org.easymock.EasyMock.*;

public class TestMessageProvider {
	private AbstractModule module;
	private MessageProvider provider;
	private Logger logger = createMock(Logger.class);
	
	@Before
	public void setUp() {
		final Properties props = new Properties();
		props.put("MessageBundlePath", "com/google/diffable/test/");
		module = new AbstractModule() {
			@Override
			protected void configure() {
				Names.bindProperties(binder(), props);
			}
		};
		provider =
			Guice.createInjector(module).getInstance(MessageProvider.class);
	}

	@Test
	public void testNewMessageProviderPath() {
		MessageProvider provider =
			new MessageProvider("com/google/diffable/test/");
		assertEquals("FAKE", provider.getMessage(
			"TestMessages", "fake.message"));
	}
	
	@Test
	public void testMessageProviderFormattedMessage() {
		MessageProvider provider =
			new MessageProvider("com/google/diffable/test/");
		assertEquals("FAKE CAKE", provider.getMessage(
			"TestMessages", "fake.messagewithargs", "CAKE"));
	}
	
	@Test
	public void testError() {
		logger.error("FAKE");
		replay(logger);
		provider.error(logger, "error.message");
		verify(logger);
	}
	
	@Test
	public void testErrorWithArgs() {
		logger.error("FAKE CAKE");
		replay(logger);
		provider.error(logger, "error.messagewithargs", "CAKE");
		verify(logger);
	}
	
	@Test
	public void testInfo() {
		logger.info("FAKE");
		replay(logger);
		provider.info(logger, "info.message");
		verify(logger);
	}
	
	@Test
	public void testInfoWithArgs() {
		logger.info("FAKE CAKE");
		replay(logger);
		provider.info(logger, "info.messagewithargs", "CAKE");
		verify(logger);
	}
	
	@Test
	public void testDebug() {
		logger.debug("FAKE");
		replay(logger);
		provider.debug(logger, "debug.message");
		verify(logger);
	}
	
	@Test
	public void testDebugWithArgs() {
		logger.debug("FAKE CAKE");
		replay(logger);
		provider.debug(logger, "debug.messagewithargs", "CAKE");
		verify(logger);
	}
	
	@Test
	public void testGetError() {
		assertEquals("FAKE", provider.error("error.message"));
	}
	
	@Test
	public void testGetErrorWithArgs() {
		assertEquals("FAKE CAKE",
				     provider.error("error.messagewithargs", "CAKE"));
	}
	
	@Test
	public void testGetInfo() {
		assertEquals("FAKE", provider.info("info.message"));
	}
	
	@Test
	public void testGetInfoWithArgs() {
		assertEquals("FAKE CAKE",
				     provider.info("info.messagewithargs", "CAKE"));
	}
	
	@Test
	public void testGetDebug() {
		assertEquals("FAKE", provider.debug("debug.message"));
	}
	
	@Test
	public void testGetDebugWithArgs() {
		assertEquals("FAKE CAKE",
				     provider.debug("debug.messagewithargs", "CAKE"));
	}
}
