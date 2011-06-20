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
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class defines the diffable context which contains
 * data about the current file versions, the resource folders, ...  
 * 
 * @author ibrahim Chaehoi
 */
public class DiffableContext implements Serializable {

	/** The serial version UID */
	private static final long serialVersionUID = 5324694195292552070L;

	/** The list of resource folders where check for resources. */ 
	private List<File> resourceFolders = null;
	
	/** The servlet prefix set in the diffable servlet initialzation parameters. */
	private String servletPrefix = null;
	
	/**
	 * Cache for storing the most recent version of a managed resource. Used by
	 * the tag to insert the current version of a resource into the page
	 * context.
	 */
	private Map<File, String> currentVersions = 
		new HashMap<File, String>();

	/**
	 * Returns the resource folders
	 * @return the resource folders
	 */
	public List<File> getResourceFolders() {
		return resourceFolders;
	}

	/**
	 * Returns the Diffable servlet prefix
	 * @return the Diffable servlet prefix
	 */
	public String getServletPrefix() {
		return servletPrefix;
	}

	/**
	 * Returns the map of resources current version 
	 * @return the map of resources current version
	 */
	public Map<File, String> getCurrentVersions() {
		return currentVersions;
	}

	/**
	 * Sets the resource folder
	 * @param resourceFolders the resource folders to set
	 */
	public void setFolder(List<File> resourceFolders) {
		this.resourceFolders = resourceFolders;
	}
	
	/**
	 * Sets the servlet prefix 
	 * @param servletPrefix the servlet prefix to set
	 */
	public void setServletPrefix(String servletPrefix) {
		this.servletPrefix = servletPrefix;
	}
	
	/**
	 * Sets the current version of a resource
	 * @param resource the resource
	 * @param currentVersion the current version
	 */
	public void setCurrentVersion(File resource, String currentVersion) {
		currentVersions.put(resource, currentVersion);
	}

	/**
	 * Returns the current version of a resource
	 * @param resource the resource
	 * @return the current version
	 */
	public String getCurrentVersion(File resource) {
		
		return currentVersions.get(resource);
	}
}
