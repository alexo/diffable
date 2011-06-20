package com.google.diffable.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.diffable.data.ResourceRequest.ResourceTypes;
import com.google.diffable.exceptions.DiffableException;
import com.google.diffable.exceptions.StackTracePrinter;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

public class TestResourceRequest {
	Injector inj;
	
	@Before
	public void setUp() {
		inj = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(StackTracePrinter.class).toProvider(
					new Provider<StackTracePrinter>() {
						@Override
						public StackTracePrinter get() {
							StackTracePrinter printer = new StackTracePrinter();
							printer.setPrintStackTraces(false);
							return printer;
						}
					});
			}
		});
	}

	@Test
	public void testNonDiffRequest() 
	throws Throwable {
		ResourceRequest request = inj.getInstance(ResourceRequest.class); 
		request.setRequest(null, "abcd");
		assertFalse(request.isDiff());
		assertEquals("abcd", request.getResourceHash());
		assertNull(request.getNewVersionHash());
		assertNull(request.getOldVersionHash());
	}
	
	@Test
	public void testDiffRequest()
	throws Throwable {
		ResourceRequest request = inj.getInstance(ResourceRequest.class);
		request.setRequest(null, "abcd_defg_ghij.diff");
		assertTrue(request.isDiff());
		assertEquals("abcd", request.getResourceHash());
		assertEquals("defg", request.getOldVersionHash());
		assertEquals("ghij", request.getNewVersionHash());
	}
	
	@Test(expected=DiffableException.class)
	public void testDiffThrowsException()
	throws Throwable {
		ResourceRequest request = inj.getInstance(ResourceRequest.class);
		request.setRequest(null, "abcd_defg.diff");
	}
	
	@Test
	public void testGettersAndSetters()
	throws Throwable {
		ResourceRequest request = inj.getInstance(ResourceRequest.class);
		request.setRequestType(ResourceTypes.CSS);
		assertEquals(ResourceTypes.CSS, request.getRequestType());
		request.setResourceHash("abcd");
		assertEquals("abcd", request.getResourceHash());
		request.setNewVersionHash("abcd");
		assertEquals("abcd", request.getNewVersionHash());
		request.setOldVersionHash("abcd");
		assertEquals("abcd", request.getOldVersionHash());
		request.setResponse("abcd");
		assertEquals("abcd", request.getResponse());
	}
}
