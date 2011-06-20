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
package com.google.diffable.listeners;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.google.diffable.config.BaseModule;
import com.google.diffable.exceptions.StackTracePrinter;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

import static org.easymock.EasyMock.*;

public class TestDiffableListener {
	private Logger logger = createMock(Logger.class);
	private ServletContext ctx;
	private Injector inj;
	private DiffableListener listener;
	
	@Before
	public void setUp() {
		ctx = createMock(ServletContext.class);
		inj = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Logger.class).toProvider(
					new Provider<Logger>() {
						@Override
						public Logger get() {
							return logger;
						}
					});
				bind(StackTracePrinter.class).toProvider(
					new Provider<StackTracePrinter>() {
						@Override
						public StackTracePrinter get() {
							StackTracePrinter printer = new StackTracePrinter();
							printer.setPrintStackTraces(true);
							return printer;
						}
					});
			}
		});
		listener = inj.getInstance(DiffableListener.class);
	}
	
	@Test
	public void testInitializeInjectedNoConfigProperties() 
	throws Throwable {
		BaseModule mod = new BaseModule();
		expect(
			ctx.getInitParameter("DiffableConfigProperties")).andReturn(null);
		expect(
			ctx.getRealPath("")).andReturn("");
		replay(ctx);
		listener.initializeInjectedProperties(ctx, mod);
		verify(ctx);
	}
}
