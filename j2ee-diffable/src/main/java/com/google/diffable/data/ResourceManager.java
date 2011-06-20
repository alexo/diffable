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
import java.util.List;

import com.google.diffable.diff.Differ;
import com.google.diffable.exceptions.ResourceManagerException;
import com.google.inject.ImplementedBy;

/**
 * A ResourceManager is responsible for versioning and hosting Diffable
 * resources.  It is also responsible for serving deltas between managed
 * resources.
 * 
 * @author joshua Harrison
 */
@ImplementedBy(FileResourceManager.class)
public interface ResourceManager {
	
	/**
	 * An initialization function for the resource manager.  This is called
	 * before the resource manager is used.
	 * 
	 * @param baseDir the webapp base directory
	 * @param ctx the diffable context
	 * 
	 * @return Returns the ResourceManager instance on successful creation of
	 *     the resource store. If this operation fails for any reason, the
	 *     operation should throw a ResourceManagerException.
	 */
	public ResourceManager initialize(String baseDir, DiffableContext ctx)
	throws ResourceManagerException;
	
	/**
	 * All ResourceManagers should take in a differ to be used for generating
	 * and returning deltas.
	 * 
	 * @param differ The differ instance to use.
	 */
	public void setDiffer(Differ differ);
	
	/**
	 * Determines whether or not the given resource is being managed.
	 * 
	 * @param resource The file system resource to check for.
	 * @return True if the resource is managed, false otherwise.
	 */
	public boolean isManaged(File resource);
	
	/**
	 * Used to add a file system resource to the resource store. If the resource
	 * is already being managed, then it will be updated and deltas will be
	 * generated between the managed versions. Otherwise it will be added.
	 *  
	 * @param resource The file system resource to manage.
	 */
	public void putResource(File resource)
	throws ResourceManagerException;
	
	/**
	 * This function will check to see if the passed in content matches that
	 * of the most recent version of the specified resource.
	 * 
	 * @param resource The file system resource to manage.
	 * @return true if the diff is NOT empty and false if no changes to the
	 * resource were detected.
	 */
	public boolean hasResourceChanged(File resource)
	throws ResourceManagerException;
	
	/**
	 * Retrieves a managed resource as a string.  This is either the content of
	 * the resource or a delta between versions of that resource.  The resource
	 * request encapsulates the type of resource being requested as well as
	 * the resource identifier and whether or not the request is for a delta.
	 * The request also contains the response that will be returned to the
	 * client.  This method should fill in the response field of the
	 * ResourceRequest using the setResponse(String) method of the request.
	 *
	 * @param request The ResourceRequest containing the request information.
	 * @return The string contents of the response.
	 */
	public void getResource(ResourceRequest request)
	throws ResourceManagerException;
	
	/**
	 * Deletes a managed resource from the manager. Does not affect the resource
	 * on the file system.
	 *  
	 * @param resource The file system resource to remove from the manager.
	 */
	public void deleteResource(File resource);
	
	/**
	 * Returns a list of files representing the resources being managed.
	 * 
	 * @return A list of files corresponding to the resources being managed.
	 */
	public List<File> getManagedResources();
}
