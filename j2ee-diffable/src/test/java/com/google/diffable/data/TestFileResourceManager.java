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
package com.google.diffable.data;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.diffable.diff.vcdiff.VCDiff;
import com.google.diffable.exceptions.ResourceManagerException;
import com.google.diffable.exceptions.StackTracePrinter;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public class TestFileResourceManager {
	private Logger logger = createMock(Logger.class);
	private File tempDir;
	private String tmp;
	private FileResourceManager mgr;
	private Injector inj;
	private String fileSeparator = File.separator;
	private DiffableContext diffableCtx;
	
	@Before
	public void setUp() throws Throwable {
		
		String userDir = System.getProperty("user.dir");
		tempDir = new File(userDir, "temp");
		
		// Clean dir
		if(tempDir.exists()){
			deleteDir(tempDir);
		}
		
		tempDir.mkdir();
		tmp = tempDir.getAbsolutePath() + File.separator;
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
		mgr = inj.getInstance(FileResourceManager.class);
		diffableCtx = new DiffableContext();
	}
	
	@After
	public void tearDown() {
		deleteDir(tempDir);
	}
	
	private void deleteDir(File dir) {
		for (File tempFile : dir.listFiles()) {
			if (tempFile.isDirectory()) {
				deleteDir(tempFile);
			} else {
				tempFile.delete();
			}
		}
		dir.delete();
	}
	
	private String hashString(String input)
	throws Exception {
		byte[] bytes = input.getBytes();
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(bytes, 0, bytes.length);
		return new BigInteger(1, md5.digest()).toString(16);
	}
	
	@Test
	public void testSetResourceStorePathNonexistentAbsolute()
	throws Throwable {
		final File doesntExist = new File(tmp + "doesntexist");
		File manifest = 
			new File(doesntExist.getAbsolutePath() + "/diffable.manifest");

		inj.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("ResourceStorePath")).to(
							"file://"+doesntExist.getAbsolutePath());
			}
		}).getMembersInjector(FileResourceManager.class).injectMembers(mgr);
		mgr.initialize(tmp, diffableCtx);

		assertTrue(manifest.exists());
		assertTrue(doesntExist.exists());
	}
	
	@Test
	public void testSetResourceStorePathNonFolder()
	throws Throwable {
		final File doesntExist = new File(tmp + "doesntexist");
		doesntExist.createNewFile();
		File diffable = new File(tmp + ".diffable");
		File diffableManifest =
			new File(diffable.getAbsolutePath() + "/diffable.manifest");

		inj.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("ResourceStorePath")).to(
						"file://"+doesntExist.getAbsolutePath());
			}
		}).getMembersInjector(FileResourceManager.class).injectMembers(mgr);
		mgr.initialize(tmp, diffableCtx);

		assertTrue(diffable.exists());
		assertTrue(diffableManifest.exists());
	}
	
	@Test
	public void testCleanupArtifactsOfNonExistentResource()
	throws Throwable {
		final File resourceStore = new File(tmp + "store");
		resourceStore.mkdir();
		File manifest =
			new File(resourceStore.getAbsolutePath() + "/diffable.manifest");
		manifest.createNewFile();
		Properties manifestProps = new Properties();
		manifestProps.put(tmp+"resource"+fileSeparator+"doesnt"+fileSeparator+"exist", "fakehash");
		manifestProps.store(new FileOutputStream(manifest), null);
		
		File resourceFolder =
			new File(resourceStore.getAbsolutePath() + "/fakehash");
		resourceFolder.mkdir();
		File fakeVersion =
			new File(resourceFolder.getAbsolutePath() + "/version");
		fakeVersion.createNewFile();
		
		inj.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("ResourceStorePath")).to(
						"file://"+resourceStore.getAbsolutePath());
			}
		}).getMembersInjector(FileResourceManager.class).injectMembers(mgr);
		mgr.initialize(tmp, diffableCtx);
		assertFalse(resourceFolder.exists());
		manifestProps = new Properties();
		manifestProps.load(new FileInputStream(manifest));
		assertEquals(0, manifestProps.keySet().size());
	}
	
	@Test
	public void testGetManagedResourcesAndManagedResourceFolderCreated() 
	throws Throwable {
	    File managedFile = new File(tmp + "tempFile");
	    managedFile.createNewFile();
		final File resourceStore = new File(tmp + "store");
		resourceStore.mkdir();
		File manifest =
			new File(resourceStore.getAbsolutePath() + "/diffable.manifest");
		manifest.createNewFile();
		Properties manifestProps = new Properties();
		manifestProps.put(managedFile.getAbsolutePath(), "fakehash");
		manifestProps.store(new FileOutputStream(manifest), null);
		inj.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("ResourceStorePath")).to(
							"file://"+resourceStore.getAbsolutePath());
			}
		}).getMembersInjector(FileResourceManager.class).injectMembers(mgr);
		mgr.initialize(tmp, diffableCtx);
		assertEquals(1, mgr.getManagedResources().size());
		assertEquals(managedFile, mgr.getManagedResources().get(0));
		assertTrue(new File(
			resourceStore.getAbsolutePath() + "/fakehash").exists());
		assertEquals(new File(
			resourceStore.getAbsolutePath() + "/fakehash").lastModified(),
			managedFile.lastModified());
	}
	
	@Test
	public void testGetManagedResourcesAndManagedResourceFolderCreatedWithRelativeStore() 
	throws Throwable {
	    File managedFile = new File(tmp + "tempFile");
	    managedFile.createNewFile();
		final File resourceStore = new File(tmp + "store");
		resourceStore.mkdir();
		File manifest =
			new File(resourceStore.getAbsolutePath() + "/diffable.manifest");
		manifest.createNewFile();
		Properties manifestProps = new Properties();
		manifestProps.put(managedFile.getAbsolutePath(), "fakehash");
		manifestProps.store(new FileOutputStream(manifest), null);
		inj.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("ResourceStorePath")).to(
							"store"); // Use relative path
			}
		}).getMembersInjector(FileResourceManager.class).injectMembers(mgr);
		mgr.initialize(tmp, diffableCtx);
		assertEquals(1, mgr.getManagedResources().size());
		assertEquals(managedFile, mgr.getManagedResources().get(0));
		assertTrue(new File(
			resourceStore.getAbsolutePath() + "/fakehash").exists());
		assertEquals(new File(
			resourceStore.getAbsolutePath() + "/fakehash").lastModified(),
			managedFile.lastModified());
	}
	
	@Test
	public void testIsManagedResource() 
	throws Throwable {
		final File resourceStore = new File(tmp + "store");
		resourceStore.mkdir();
		File manifest =
			new File(resourceStore.getAbsolutePath() + "/diffable.manifest");
		manifest.createNewFile();
		inj.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("ResourceStorePath")).to(
							"file://"+resourceStore.getAbsolutePath());
			}
		}).getMembersInjector(FileResourceManager.class).injectMembers(mgr);
		mgr.initialize(tmp, diffableCtx);
		assertFalse(mgr.isManaged(new File("fake/file/path")));
	}
	
	@Test
	public void testPutResourcePutAgainNoChange()
	throws Throwable {
		File managedFile = new File(tmp + "tempFile");
	    managedFile.createNewFile();
	    FileOutputStream out = new FileOutputStream(managedFile);
	    out.write("Hello World!".getBytes());
	    out.close();
	    mgr.initialize(tmp, diffableCtx);
	    
	    File store = new File(tmp + ".diffable");
	    assertTrue(store.exists());
	    File manifest =
	    	new File(store.getAbsolutePath() + "/diffable.manifest");
	    assertTrue(manifest.exists());
	    assertEquals(0, mgr.getManagedResources().size());
	    
	    mgr.putResource(managedFile);
	    assertEquals(1, mgr.getManagedResources().size());
	    String pathHash = hashString(managedFile.getAbsolutePath());
	    File resourceFolder =
	    	new File(store.getAbsolutePath() + "/" + pathHash);
	    assertTrue(resourceFolder.exists());
	    assertEquals(resourceFolder.lastModified(), managedFile.lastModified());
	    assertEquals(1, resourceFolder.listFiles().length);
	    
	    String contentHash = hashString("Hello World!");
	    File version =
	    	new File(resourceFolder.getAbsolutePath() + "/" +
	    			 contentHash + ".version");
	    assertTrue(version.exists());
	    
	    mgr.putResource(managedFile);
	    assertEquals(1, resourceFolder.listFiles().length);
	    assertEquals(resourceFolder.lastModified(), managedFile.lastModified());
	}
	
	@Test
	public void testPutThreeVersionsOfResource()
	throws Throwable {
		File managedFile = new File(tmp + "tempFile");
	    managedFile.createNewFile();
	    FileOutputStream out = new FileOutputStream(managedFile);
	    out.write("Hello World!".getBytes());
	    out.close();
	    mgr.initialize(tmp, diffableCtx);
	    
	    File store = new File(tmp + ".diffable");
	    assertTrue(store.exists());
	    File manifest =
	    	new File(store.getAbsolutePath() + "/diffable.manifest");
	    
	    mgr.putResource(managedFile);
	    String pathHash = hashString(managedFile.getAbsolutePath());
	    File resourceFolder =
	    	new File(store.getAbsolutePath() + "/" + pathHash);
	    String firstContentHash = hashString("Hello World!");
	    File firstVersion =
	    	new File(resourceFolder.getAbsolutePath() + "/" +
	    			 firstContentHash + ".version");
	    Thread.sleep(1000);
	    out = new FileOutputStream(managedFile);
	    out.write("Goodbye World!".getBytes());
	    out.close();
	    
	    assertTrue(managedFile.lastModified() != resourceFolder.lastModified());
	    String secondContentHash = hashString("Goodbye World!");
	    File secondVersion =
	    	new File(resourceFolder.getAbsolutePath() + "/" +
	    			 secondContentHash + ".version");
	    File diffFile =
	    	new File(resourceFolder.getAbsolutePath() + "/" +
	    			 firstContentHash + "_" + secondContentHash + ".diff");
	    mgr.putResource(managedFile);
	    
	    assertEquals(3, resourceFolder.listFiles().length);
	    assertTrue(firstVersion.exists());
	    assertTrue(secondVersion.exists());
	    assertTrue(diffFile.exists());
	    assertEquals(resourceFolder.lastModified(), managedFile.lastModified());
	    
	    Thread.sleep(1000);
	    out = new FileOutputStream(managedFile);
	    out.write("Hello Heaven!".getBytes());
	    out.close();
	    
	    assertTrue(managedFile.lastModified() != resourceFolder.lastModified());
	    String thirdContentHash = hashString("Hello Heaven!");
	    File thirdVersion =
	    	new File(resourceFolder.getAbsolutePath() + "/" +
	    			 secondContentHash + ".version");
	    mgr.putResource(managedFile);
	    assertEquals(5, resourceFolder.listFiles().length);
	    assertTrue(firstVersion.exists());
	    assertTrue(secondVersion.exists());
	    assertTrue(thirdVersion.exists());
	    assertTrue(new File(resourceFolder.getAbsolutePath() + "/" +
	    		   firstContentHash + "_" + thirdContentHash + 
	    		   ".diff").exists());
	    assertTrue(new File(resourceFolder.getAbsolutePath() + "/" +
	    		   secondContentHash + "_" + thirdContentHash + 
	    		   ".diff").exists());
	}
	
	@Test
	public void testDeleteResource()
	throws Throwable {
		File managedFile = new File(tmp + "tempFile");
	    managedFile.createNewFile();
	    FileOutputStream out = new FileOutputStream(managedFile);
	    out.write("Hello World!".getBytes());
	    out.close();
	    mgr.initialize(tmp, diffableCtx);
	    
	    // Delete the resource before adding it to confirm that attempting to
	    // delete an unmanaged resource doesn't throw an error.
	    mgr.deleteResource(managedFile);
	    
	    File store = new File(tmp + ".diffable");
	    mgr.putResource(managedFile);
	    assertEquals(1, mgr.getManagedResources().size());
	    String pathHash = hashString(managedFile.getAbsolutePath());
	    File resourceFolder =
	    	new File(store.getAbsolutePath() + "/" + pathHash);
	    assertTrue(resourceFolder.exists());
	    assertEquals(1, resourceFolder.listFiles().length);
	    
	    mgr.deleteResource(managedFile);
	    assertEquals(0, mgr.getManagedResources().size());
	    assertFalse(resourceFolder.exists());
	}
	
	@Test
	public void testHasResourceChanged()
	throws Throwable {
		File managedFile = new File(tmp + "tempFile");
	    managedFile.createNewFile();
		final File resourceStore = new File(tmp + "store");
		resourceStore.mkdir();
		File manifest =
			new File(resourceStore.getAbsolutePath() + "/diffable.manifest");
		manifest.createNewFile();
		Properties manifestProps = new Properties();
		manifestProps.put(managedFile.getAbsolutePath(), "fakehash");
		manifestProps.store(new FileOutputStream(manifest), null);
		File managedResourceFolder = new File(
			resourceStore.getAbsolutePath() + "/fakehash");
		managedResourceFolder.mkdir();
		inj.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("ResourceStorePath")).to(
							"file://"+resourceStore.getAbsolutePath());
			}
		}).getMembersInjector(FileResourceManager.class).injectMembers(mgr);
		mgr.initialize(tmp, diffableCtx);
		assertFalse(mgr.hasResourceChanged(managedFile));
		managedResourceFolder.setLastModified(0);
		assertTrue(mgr.hasResourceChanged(managedFile));
	}
	
	@Test(expected=ResourceManagerException.class)
	public void testHasResourceChangedNotManagedResource()
	throws Throwable {
		File managedFile = new File(tmp + "tempFile");
	    managedFile.createNewFile();
		mgr.initialize(tmp, diffableCtx);
		mgr.hasResourceChanged(managedFile);
	}
	
	@Test
	public void testGetNonManagedResource()
	throws Throwable {
		// Simply confirms that a bogus ResourceRequest won't cause a crash.
	    mgr.initialize(tmp, diffableCtx);
	    ResourceRequest req = inj.getInstance(ResourceRequest.class);
	    req.setResourceHash("aaa");
	    mgr.getResource(req);
	}
	
	@Test
	public void testGetResource()
	throws Throwable {
		VCDiff differ = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("BlockSize")).to(3);
			}
		}).getInstance(VCDiff.class);
		mgr.setDiffer(differ);
		
		File managedFile = new File(tmp + "tempFile");
	    managedFile.createNewFile();
	    FileOutputStream out = new FileOutputStream(managedFile);
	    out.write("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes());
	    out.close();
	    mgr.initialize(tmp, diffableCtx);
	    mgr.putResource(managedFile);
	    
	    String resourceHash = hashString(managedFile.getAbsolutePath());
	    
	    ResourceRequest req = new ResourceRequest();
	    req.setRequest(null, resourceHash);
	    mgr.getResource(req);
	    assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", req.getResponse());
	    
	    Thread.sleep(1000);
	    out = new FileOutputStream(managedFile);
	    out.write("bbbbbbbbbbbbbbbaaaaaaaaaaaaaaa".getBytes());
	    out.close();
	    mgr.putResource(managedFile);
	    req = new ResourceRequest();
	    req.setRequest(null, resourceHash);
	    mgr.getResource(req);
	    assertEquals("bbbbbbbbbbbbbbbaaaaaaaaaaaaaaa", req.getResponse());
	    
	    String oldVersion = hashString("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
	    String newVersion = hashString("bbbbbbbbbbbbbbbaaaaaaaaaaaaaaa");
	    req = new ResourceRequest();
	    req.setRequest(null, resourceHash + "_" + oldVersion + "_" +
								   newVersion + ".diff");
	    mgr.getResource(req);
	    assertEquals("[\"bbbbbbbbbbbbbbb\",0,15,]", req.getResponse());
	}
	
	private ResourceRequest noDiffHelper(FileResourceManager mgr,
			                             boolean isDiff)
	throws Throwable {
		File managedFile = new File(tmp + "tempFile");
	    managedFile.createNewFile();
	    FileOutputStream out = new FileOutputStream(managedFile);
	    out.write("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes());
	    out.close();
	    mgr.initialize(tmp, diffableCtx);
	    mgr.putResource(managedFile);
	    
	    String resourceHash = hashString(managedFile.getAbsolutePath());
	    
	    ResourceRequest req = new ResourceRequest();
	    String hash = resourceHash + (isDiff ? "_aa_bb.diff" : "");
	    req.setRequest(null, hash);
	    mgr.getResource(req);
	    return req;
	}
	
	/**
	 * Confirm that when the manager can't find the specified diff, it returns
	 * the contents of the latest version of the managed resource.
	 */
	@Test
	public void testNoDiffReturnLatestNotInMemory()
	throws Throwable {
		inj.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("KeepResourcesInMemory")).to(false);
			}
		}).getMembersInjector(FileResourceManager.class).injectMembers(mgr);
		
		ResourceRequest req = noDiffHelper(mgr, true);
	    assertEquals("[\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"]", req.getResponse());
	}
	
	@Test
	public void testNoDiffReturnLatestInMemory()
	throws Throwable {
		inj.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("KeepResourcesInMemory")).to(true);
			}
		}).getMembersInjector(FileResourceManager.class).injectMembers(mgr);
		
		ResourceRequest req = noDiffHelper(mgr, true);
	    assertEquals("[\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"]", req.getResponse());
	}
	
	@Test
	public void testGetResourceNotInMemory()
	throws Throwable {
		inj.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("KeepResourcesInMemory")).to(false);
			}
		}).getMembersInjector(FileResourceManager.class).injectMembers(mgr);
		
		ResourceRequest req = noDiffHelper(mgr, false);
	    assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", req.getResponse());
	}
	
	@Test
	public void testGetResourceInMemory()
	throws Throwable {
		inj.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(
					Names.named("KeepResourcesInMemory")).to(true);
			}
		}).getMembersInjector(FileResourceManager.class).injectMembers(mgr);
		
		ResourceRequest req = noDiffHelper(mgr, false);
	    assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", req.getResponse());
	}
}
