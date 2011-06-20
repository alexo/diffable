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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.diffable.Constants;
import com.google.diffable.config.MessageProvider;
import com.google.diffable.diff.Differ;
import com.google.diffable.diff.JSONHelper;
import com.google.diffable.exceptions.ResourceManagerException;
import com.google.diffable.exceptions.StackTracePrinter;
import com.google.diffable.utils.IOUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * The FileResourceManager implements the resource manager interface using the
 * file system as its backing store.
 * 
 * @author joshua Harrison
 * @author ibrahim Chaehoi
 */
@Singleton
public class FileResourceManager implements ResourceManager {
	
	@Inject
	private StackTracePrinter printer;
	
	@Inject
	private MessageProvider provider;
	
	@Inject(optional=true)
	private Logger logger = Logger.getLogger(FileResourceManager.class);
	
	/** 
	 * This property dictates whether the string data for the latest version of
	 * a given resource is stored in memory or read from disk.
	 */
	@Inject(optional=true) @Named(value="KeepResourcesInMemory")
	private boolean keepResourcesInMemory = true;
	
	/** The map of resource content */
	private Map<File, String> resourceContents = new HashMap<File, String>();
	
	/** Used to reference managed resources by their path hash. */
	private Map<String, File> hashsToResources = new HashMap<String, File>();
	
	/** The resource store path is provided by the DiffableConfigProperties file. */
	@Inject(optional=true) @Named(value="ResourceStorePath")
	private String resourceStorePath = null;
	
	/** The resource store */
	private File resourceStore = null;
	
	/** The diffable context */
	private DiffableContext diffableCtx;
	
	/** The differ */
	private Differ differ = null;

	/**
	 * If the manifest is null, the resource manager cannot be used, and
	 * Diffable will not host any resources.
	 */
	private Properties manifest = null;
	
	/** A map relating managed resources to their corresponding resource folders. */
	private Map<File, File> managedResouceFolders = new HashMap<File, File>();
	
	/**
	 * A private inner class used for filtering the contents of a managed
	 * resource folder.  It collects the versions of a managed resource not
	 * including the latest version and deletes deltas as it goes.
	 * @author joshua Harrison
	 *
	 */
	private class VersionFilter implements FileFilter {
		
		private static final String VERSION_FILE_EXTENSION = "version";
		
		private String currentHash = null;
		private File currentFile = null;
		
		public VersionFilter(String currentHash) {
			this.currentHash = currentHash;
		}
		
		public File getCurrentFile() {
			return this.currentFile;
		}
		
		@Override
		public boolean accept(File current) {
			String extension = current.getName().split("\\.")[1];
			if (extension.equals(VERSION_FILE_EXTENSION)) {
				// Return false for the current version.
				if (current.getAbsolutePath().contains(this.currentHash)) {
					this.currentFile = current;
					return false;
				} else {
					return true;
				}
			} else {
				// Delete old deltas.
				current.delete();
				return false;
			}
		}
	}

	@Override
	public synchronized void deleteResource(File resource) {
		if (isManaged(resource)) {
			manifest.remove(resource.getAbsolutePath());
			cleanUpResource(resource);
		}
	}

	@Override
	public List<File> getManagedResources() {
		ArrayList<File> managedResources = new ArrayList<File>();
		for (Object key : manifest.keySet()) {
			managedResources.add(new File(key.toString()));
		}
		return managedResources;
	}

	@Override
	public synchronized void getResource(ResourceRequest request)
			throws ResourceManagerException {
		File resource = hashsToResources.get(request.getResourceHash());
		if (resource != null) {
			provider.debug(logger, "filemgr.getresource",
					       request.getResourceHash(),
					       resource.getAbsolutePath());
			if (request.isDiff()) {
				File folder = managedResouceFolders.get(resource);
				File diff = new File(
					folder.getAbsolutePath() + File.separator +
					request.getOldVersionHash() + "_" +
					request.getNewVersionHash() + ".diff");
				if (diff.exists()) {
					request.setResponse(readFileContents(diff));
				} else {
					// If the diff being requested cannot be located, then the
					// response should be set to the whole string of the latest
					// version of the resource.  This means that in the worst
					// case, if a diff can't be located, the client will still
					// execute the latest version of the resource.
					if (keepResourcesInMemory) {
						request.setResponse(
							"[" +
							JSONHelper.quote(resourceContents.get(resource)) +
							"]");
					} else {
						String contents = readFileContents(resource);
						request.setResponse(
							"[" +
							JSONHelper.quote(contents) +
							"]");
					}
				}
			} else {
				if (keepResourcesInMemory) {
					request.setResponse(resourceContents.get(resource));
				} else {
					request.setResponse(readFileContents(resource));
				}
				request.setNewVersionHash(diffableCtx.getCurrentVersion(resource));
			}
		}
	}

	@Override
	public boolean hasResourceChanged(File resource)
			throws ResourceManagerException {
		
		// To know if a resource has change, we save the last modified date of the resource in corresponding
		// resource folder, so we will be able to check if the resource has change by comparing the last modified date
		// of the resource and the last modified date of the resource folder
		if (isManaged(resource)) {
			if (managedResouceFolders.containsKey(resource) && (
				resource.lastModified() ==
				managedResouceFolders.get(resource).lastModified())) {
				return false;
			} else {
				return true;
			}
		} else {
			throw new ResourceManagerException(
				provider.error("filemgr.filenotmanaged", 
						       resource.getAbsolutePath()));
		}
	}

	@Override
	public boolean isManaged(File resource) {
		if (manifest.containsKey(resource.getAbsolutePath())) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized void putResource(File resource) throws ResourceManagerException {
		// Creates any artifacts necessary for managing a resource.  If all
		// artifacts are already setup, this call is a no-op.
		try {
			createResourceArtifacts(resource);
		} catch (Exception exc) {
			printer.print(exc);
			throw new ResourceManagerException(
				provider.error("filemgr.cantcreateresource",
						       resource.getAbsolutePath()));
		}
		// If the resource has changed obtain the new hash of the resources
		// contents and generate diffs between the older versions of the
		// resource and the latest version.
		if (hasResourceChanged(resource)) {
			provider.info(logger, "filemgr.resourcechanged",
					      resource.getAbsolutePath());
			try {
				String latestHash = readInAndCopyLatestVersion(resource, false);

				// If the latest hash is not null, then the actual contents of
				// the managed resource have changes, and new diffs must be
				// generated.
				if (latestHash != null) {
					provider.info(logger, "filemgr.gendeltas",
						      	  resource.getAbsolutePath());
					generateDeltas(resource, latestHash);
					// Update the Diffable context so it can correctly identify
					// the most recent version of this resource.
					diffableCtx.setCurrentVersion(resource, latestHash);
				}
			} catch (Exception exc) {
				printer.print(exc);
				throw new ResourceManagerException(
						provider.error("filemgr.cantloadlatest",
								       resource.getAbsolutePath()));
			}
		}
	}
	
	@Override @Inject
	public void setDiffer(Differ differ) {
		this.differ = differ;
	}

	@Override
	public ResourceManager initialize(String baseDir, DiffableContext ctx)
	throws ResourceManagerException {
		
		diffableCtx = ctx;
		String webAppBaseDir = baseDir;
		if(!webAppBaseDir.endsWith(File.separator)){
			webAppBaseDir += File.separator;
		}
		// If the resource store path is not defined, it defaults to ".diffable"
		// relative to the web app folder.  If the path starts with a slash, it
		// is interpreted as absolute.  Otherwise, it is interpreted relative to
		// the web app folder.
		ArrayList<String> paths = new ArrayList<String>();
		paths.add(webAppBaseDir + ".diffable"); 
		if (resourceStorePath != null) {
			
			if(resourceStorePath.startsWith(Constants.FILE_URI_SCHEME_PREFIX)){
				resourceStorePath = resourceStorePath.substring(Constants.FILE_URI_SCHEME_PREFIX.length());
			}else{
				resourceStorePath =
					webAppBaseDir + resourceStorePath;
			}
			
			paths.add(0, resourceStorePath);
		}
		// Check to make sure the path resolves to a valid folder on the
		// file system.
		for (String path : paths) {
			provider.debug(logger, "resourcestore.init", path);
			File resourceStore = new File(path);
			if (!resourceStore.exists()) {
				provider.info(logger, "filemgr.createfolder",
					          resourceStore.getAbsolutePath());
				resourceStore.mkdir();
			}
			if (!resourceStore.isDirectory()) {
				provider.error(logger, "filemgr.notfolder", resourceStorePath);
			} else {
				// If the resource store exists, attempt to open the manifest file,
				// and if one doesn't exist, create it.
				this.resourceStore = resourceStore;
				initializeManifest();
				provider.info(logger, "filemgr.resourcestore",
						      resourceStore.getAbsolutePath());
				break;
			}
		}
		if (resourceStore == null) {
			throw new ResourceManagerException(
				provider.error("filemgr.cantcreatestore"));
		} else {
			return this;
		}
	}
	
	/**
	 * The FileResourceManager uses a manifest file to persist information
	 * about the resources it is managing.  The file keeps track of which file
	 * system resources are managed along with which versions of each exist.
	 * 
	 * This function is called once when Diffable first loads. If a manifest
	 * does not yet exist, one is created.  If one does exist, it is loaded into
	 * memory and checked against the file system for consistency. If a resource
	 * that was managed cannot be found, then the corresponding resource manager
	 * resources will be freed. If resource manager resources, such as diffs or
	 * versions, have been removed from the file system outside of Diffable, the
	 * manifest will be updated to reflect reality.
	 */
	private void initializeManifest() {
		// If the manifest does not yet exist, create it.
		File manifestFile = 
			new File(resourceStore.getAbsolutePath() + "/diffable.manifest");
		manifest = new Properties();
		if (!manifestFile.exists()) {
			try {
				manifestFile.createNewFile();
				provider.debug(logger, "filemgr.createmanifest",
							   manifestFile.getAbsolutePath());
			} catch (IOException exc) {
				provider.error(logger, "filemgr.cantcreatemanifest",
							   manifestFile.getAbsolutePath());
				printer.print(exc);
			}
		} else {
			FileInputStream is = null;
			FileOutputStream out = null;
			try {
				is = new FileInputStream(manifestFile);
				manifest.load(is);
				Set<Object> keys = manifest.keySet();
				// Each of the keys in the properties file should be the
				// absolute path of a managed resource file. The value should
				// be the hash of that file path.
				for (Object key : keys) {
					String path = key.toString();
					
					// Check to make sure the file still exists.  If it doesn't
					// clean up all remaining resources and remove the entry.
					File managedResource = new File(path);
					if (!managedResource.exists()) {
						cleanUpResource(managedResource);
						keys.remove(key);
					} else {
						// Call putResource, which will make sure all necessary
						// artifacts are created and the resource is properly
						// managed.
						putResource(managedResource);
						
						// Explicitly retrieve the hash of the current version
						// of the managed resource and pass it to the Diffable
						// tag on start up.
						String latestHash =
							readInAndCopyLatestVersion(managedResource, true);
						// Update the Diffable context so it can correctly identify
						// the most recent version of this resource.
						diffableCtx.setCurrentVersion(managedResource, latestHash);
					}
				}
				out = new FileOutputStream(manifestFile);
				manifest.store(out, null);
			} catch (Exception exc) {
				provider.error(logger, "manifest.cantload",
						       manifestFile.getAbsolutePath());
				printer.print(exc);
			}finally{
				IOUtils.close(is);
				IOUtils.close(out);
			}
		}
	}
	
	/**
	 * This function generates deltas between the latest version of a managed
	 * resource and all the previous versions of that resource.  It also deletes
	 * all deprecated diffs so that only deltas bringing older versions to the
	 * latest version are stored in the managed resource folder.
	 * 
	 * @param resource The managed resource.
	 * @param latestHash The hash of the contents of the latest version of the
	 *     resource.
	 */
	private void generateDeltas(File resource, final String latestHash) {
		// Create a file filter that only returns old versions of a managed
		// resource.  It also deletes deprecated diffs as it's going.
		VersionFilter filter = new VersionFilter(latestHash);
		try {
			String hash = hashResourcePath(resource);
			File resourceFolder = getManagedResourceFolder(resource, hash);
			File[] oldVersions = resourceFolder.listFiles(filter);
			File currentVersion = filter.getCurrentFile();
			String currentContent = null;
			// Get the current content from memory or from the version file.
			if (keepResourcesInMemory) {
		        currentContent = this.resourceContents.get(resource);
			} else {
				currentContent = readFileContents(currentVersion);
			}
            if (currentContent != null && oldVersions != null) {
            	// For each of the old versions, get the content and generate a
            	// diff between the old version and the newest version.
				for (File version : oldVersions) {
					String oldContent = readFileContents(version);
					String diff =
						differ.getDiffAsString(oldContent.toString(),
								               currentContent);
					File newDelta = new File(
						resourceFolder.getAbsolutePath() + File.separator +
						version.getName().split("\\.")[0] + "_" +
						latestHash + ".diff");
					provider.debug(logger, "filemgr.deltagenerated",
							       newDelta.getAbsolutePath(),
							       resource.getAbsolutePath());
					newDelta.createNewFile();
					FileOutputStream out = null;
					try{
						out = new FileOutputStream(newDelta);
						out.write(diff.getBytes());
					}
					finally{
						IOUtils.close(out);
					}
				}
            }
            
            // Ensure that the resource folder last modified date match with the resource last modified date
            resourceFolder.setLastModified(resource.lastModified());
    		
		} catch (Exception exc) {
			provider.error(logger, "filemgr.deltaerror",
					       resource.getAbsolutePath(), latestHash);
			printer.print(exc);
		}
		
		
	}
	
	/**
	 * Used to create the initial artifacts necessary to manage a resource, such
	 * as the managed resource folder and the entry in the manifest.
	 * 
	 * @param resource The resource to manage.
	 */
	private void createResourceArtifacts(File resource)
	throws Exception {
		// If the resource is unmanaged, make sure there are no artifacts
		// that will get in the way of managing it.
		if (!isManaged(resource)) {
			cleanUpResource(resource);
		}
		
		String resourceNameHash = hashResourcePath(resource);
		// Set up the reverse mapping of hashs to resources.
		hashsToResources.put(resourceNameHash, resource);
		
		File resourceFolder =
			getManagedResourceFolder(resource, resourceNameHash);
		// If the resource is already managed and its corresponding resource
		// folder already exists, then return.
		if (isManaged(resource) && resourceFolder.exists()) {
			if (!this.managedResouceFolders.containsKey(resource)) {
				this.managedResouceFolders.put(resource, resourceFolder);
			}
			return;
		// Otherwise, create the managed resource folder for the given resource
		// and copy over the most recent version.
		} else {
			resourceFolder.mkdir();
			// Ensure that the resourceFolder last modified date match with the resource last modified date
			resourceFolder.setLastModified(resource.lastModified());
			
			this.managedResouceFolders.put(resource, resourceFolder);
			manifestPutAndSave(resource.getAbsolutePath(), resourceNameHash);
			
			String latestHash = readInAndCopyLatestVersion(resource, false);
			// Update the Diffable context so it can correctly identify
			// the most recent version of this resource.
			diffableCtx.setCurrentVersion(resource, latestHash);
		}
	}

	/**
	 * Utility method for getting the contents of a managed resource and copying
	 * the current version of that resource into the corresponding managed
	 * resource folder.
	 * 
	 * @param resource The managed resource to read.
	 * @param force If true, this function will return the hash and copy the
	 *     latest version into the resource folder even if it already exists.
	 * @return The hash of the contents of latest version of the resource, or
	 *     null if the resource hasn't changed (based on the hash of the
	 *     resource's contents).
	 * @throws Exception
	 */
	private String readInAndCopyLatestVersion(File resource, boolean force)
	throws Exception {
		String hash = hashResourcePath(resource);
		StringBuilder resourceContents = new StringBuilder();
		String resourceContentsHash =
			readAndGetChecksum(resource, resourceContents);
		
		File resourceFolder = getManagedResourceFolder(resource, hash);

		File version =
			new File(resourceFolder.getAbsolutePath() + File.separator +
					 resourceContentsHash + ".version");
		// If the actual contents of the file haven't changed then return null
		// to indicate this to consumers of this function.  Since the names of
		// version files correspond to the MD5 cache of the file's contents, the
		// existence of the version file can be used to see whether the contents
		// have changed.
		if (!version.exists() || force) {
			version.createNewFile();
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(version);
				out.write(resourceContents.toString().getBytes());
			}finally{
				IOUtils.close(out);
			}
			
			if (keepResourcesInMemory) {
				this.resourceContents.put(
					resource, resourceContents.toString());
			}
			
		} else {
			resourceContentsHash = null;
		}
		
		// Ensure that the resourceFolder last modified date match with the resource last modified date
		resourceFolder.setLastModified(resource.lastModified());
		
		return resourceContentsHash;
		
		
	}
	
	/**
	 * Utility function for obtaining the MD5 hash of a given file's absolute
	 * path.  This is then used for uniquely identifying the managed resource
	 * within the FileResourceManager.
	 * 
	 * @param resource The managed resource.
	 * @return A hex string representing a hash of the file's absolute path.
	 * @throws Exception
	 */
	private String hashResourcePath(File resource)
	throws Exception {
		if (manifest.containsKey(resource.getAbsolutePath())) {
			return manifest.getProperty(resource.getAbsolutePath());
		} else {
			try {
				byte[] filePathBytes = resource.getAbsolutePath().getBytes();
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				md5.update(filePathBytes, 0, filePathBytes.length);
				return new BigInteger(1, md5.digest()).toString(16);
			} catch (Exception exc) {
				provider.error(logger, "filemgr.pathhasherror",
					       	   resource.getAbsolutePath());
				throw exc;
			}
		}
	}
	
	/**
	 * Reads and the contents of a file.
	 * 
	 * @param toRead The file to read.
	 *
	 * @return the file content 
	 */
	private String readFileContents(File toRead) {
		try {
			Reader rd = new FileReader(toRead);
			StringWriter sw = new StringWriter();
			IOUtils.copy(rd, sw, true);			
			return sw.toString();
		} catch (IOException exc) {
			provider.error(
				logger, "filemgr.readerror", toRead.getAbsolutePath());
			printer.print(exc);
		}
		return null;
	}
	
	/**
	 * Reads the contents of a file into the passed in StringBuffer and returns
	 * the checksum of the file.
	 * 
	 * @param toRead The file to read.
	 * @param fileContent A StringBuilder used to hold the contents of the file
	 *     being read.
	 * @return A hex representation of the md5 checksum of the file's contents. 
	 */
	private String readAndGetChecksum(File toRead, StringBuilder fileContent) {
		InputStream in = null; 
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			in = new FileInputStream(toRead);
			in = new DigestInputStream(in, md);
			Reader rd = new InputStreamReader(in);
			char[] buf = new char[IOUtils.BUFFER_SIZE];
			int num = 0;
			while ((num = rd.read(buf, 0, buf.length)) != -1) {
				fileContent.append(buf, 0, num);
			}
			
			return new BigInteger(1, md.digest()).toString(16);
		} catch (NoSuchAlgorithmException exc) {
			provider.error(
				logger, "filemgr.contenthasherror", toRead.getAbsolutePath());
			printer.print(exc);
		} catch (IOException exc) {
			provider.error(
				logger, "filemgr.readerror", toRead.getAbsolutePath());
			printer.print(exc);
		}finally{
			IOUtils.close(in);
		}
		return null;
	}
	
	/**
	 * For a path representing a managed resource, this function looks up the
	 * hash of that path from the manifest, and then deletes all artifacts
	 * corresponding to the previously managed resource.
	 * 
	 * @param path The absolute path of the previously managed resource.
	 */
	private void cleanUpResource(File toClean) { 
		String path = toClean.getAbsolutePath();
		String hash = null;
		try {
			hash = hashResourcePath(toClean);
		} catch (Exception exc) {
			provider.error(logger, "filemgr.cantcleanupresource",
					       toClean.getAbsolutePath());
			printer.print(exc);
		}
		if (hash != null) {
			File resourceFolder = getManagedResourceFolder(toClean, hash);
			if (resourceFolder.exists()) {
				for (File file : resourceFolder.listFiles()) {
					provider.debug(logger, "manifest.deletefile",
							       file.getAbsolutePath(), path);
					file.delete();
				}
				provider.debug(logger, "manifest.deletefolder",
					           resourceFolder.getAbsolutePath(),
					           path);
				resourceFolder.delete();
			}
		}
	}
	
	/**
	 * Simple utility function for getting the resource folder corresponding
	 * to a given resource.
	 * 
	 * @param resource The managed resource.
	 * @param hash The hash of the managed resource.
	 * @return The folder where artifacts corresponding to the managed resource
	 *     are stored.
	 */
	private File getManagedResourceFolder(File resource, String hash) {
		return new File(this.resourceStore, hash);
	}

	/**
	 * Utility function for adding a value to the manifest and persisting to
	 * disk.
	 * 
	 * @param key The key to add.
	 * @param value The value to add.
	 */
	private void manifestPutAndSave(String key, String value) {
		manifest.put(key, value);
		File manifestFile = new File(this.resourceStore , "diffable.manifest");
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(manifestFile);
			manifest.store(out, null);
			out.close();
		} catch (IOException exc) {
			provider.error(logger, "manifest.cantsave",
					       manifestFile.getAbsolutePath());
			printer.print(exc);
		}finally{
			IOUtils.close(out);
		}
	}
}
