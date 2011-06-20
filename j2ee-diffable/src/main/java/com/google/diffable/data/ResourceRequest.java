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

import java.util.HashMap;
import java.util.Map;

import com.google.diffable.config.MessageProvider;
import com.google.diffable.exceptions.DiffableException;
import com.google.diffable.exceptions.StackTracePrinter;
import com.google.inject.Inject;

/**
 * This class encapsulates a request for a resource from the client.  As such
 * it must be maintained in concert with the Javascript code so that it can 
 * correctly interpret requests.  For instance, it interprets resources ending
 * with '.diff' to be a request for a diff between two versions of a resource.
 * As such, the Javascript client must be sure to append '.diff' to requests
 * for deltas.  Places in the code where such assumptions are made will be
 * marked.
 * 
 * @author joshua Harrison
 *
 */
public class ResourceRequest {
	
	@Inject
	private StackTracePrinter printer;
	
	@Inject
	private MessageProvider provider;
	
	// An enum representing the types of resources that can be managed and
	// served by Diffable.
	public static enum ResourceTypes {
		JAVASCRIPT,
		HTML,
		CSS
	}
	
	// The map contains a mapping between resource suffixes and the type of
	// resource they map to.
	public static Map<String, ResourceTypes> extensionMap =
		new HashMap<String, ResourceTypes>();
	static {
		extensionMap.put("js", ResourceTypes.JAVASCRIPT);
		extensionMap.put("html", ResourceTypes.HTML);
		extensionMap.put("css", ResourceTypes.CSS);
	}
	
	private ResourceTypes requestType;
	private boolean isDiff = false;
	private String resourceHash;
	private String oldVersion;
	private String newVersion;
	private String response;
	private String basePath;
	
	/**
	 * Initialize the request (stripped of servlet context)
	 * and a list of resource folders where managed resources may exist.
	 * @param basePath the basePath containing the application context path and the servlet path
	 * @param requested The requested resource. At this point, any servlet
	 *     context information should be removed.  For instance, if the request
	 *     was for http://localhost/SomeWebApp/diffable/aabbcc, then requested
	 *     should only be 'aabbcc.'
	 */
	public void setRequest(String basePath, String requested)
	throws DiffableException {

		if(requested == null){
			throw new DiffableException("Error creating resource request for '"+requested+"'.");
		}
		try {
			
			this.basePath = basePath;
			
			// The Javascript client must end all requests for diffs with
			// '.diff'.
			if (requested.endsWith(".diff")) {
				this.isDiff = true;
			}
			// Diff requests are formed by listing the resource hash followed by
			// an underscore followed by the old version hash followed by an
			// underscore followed by the new version hash followed by '.diff.'
			// For instance, assumed js/fake.js gets hashed as aa, the old
			// version is bb and the new version is cc, the diff request will
			// look like: aa_bb_cc.diff
			if (this.isDiff) {
				// Take the part of the string before the '.diff' and split out
				// the component hashes.
				String[] parts = requested.split("\\.")[0].split("_");
				this.resourceHash = parts[0];
				this.oldVersion = parts[1];
				this.newVersion = parts[2];
			} else {
				// If this is not a request for a delta then the request should
				// simply take the form of the resource hash.  For instance, if
				// fake.js hashes to aa, then the request should look like:
				// aa.js.
				this.resourceHash = requested;
			}
			// The printer and the provider are not available in the Constructor at that time
		} catch (Exception exc) {
			printer.print(exc);
			DiffableException newExc = new DiffableException(
				provider.error("resourcerequest.error", requested));
			newExc.setStackTrace(exc.getStackTrace());
			throw newExc;
		}
	}
	
	public ResourceTypes getRequestType() {
		return this.requestType;
	}
	
	public void setRequestType(ResourceTypes requestType) {
		this.requestType = requestType;
	}
	
	public boolean isDiff() {
		return this.isDiff;
	}
	
	public String getResourceHash() {
		return this.resourceHash;
	}
	
	public void setResourceHash(String resourceHash) {
		this.resourceHash = resourceHash;
	}
	
	public String getOldVersionHash() {
		return this.oldVersion;
	}
	
	public void setOldVersionHash(String oldVersion) {
		this.oldVersion = oldVersion;
	}
	
	public String getNewVersionHash() {
		return this.newVersion;
	}
	
	public void setNewVersionHash(String newVersion) {
		this.newVersion = newVersion;
	}
	
	/**
	 * Returns the base path
	 * @return the base path
	 */
	public String getBasePath() {
		return basePath;
	}

	public String getResponse() {
		return this.response;
	}
	
	public void setResponse(String response) {
		this.response = response;
	}
}
