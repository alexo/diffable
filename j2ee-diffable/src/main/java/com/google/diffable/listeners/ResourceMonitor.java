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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.google.diffable.config.MessageProvider;
import com.google.diffable.data.ResourceManager;
import com.google.diffable.exceptions.StackTracePrinter;
import com.google.inject.Inject;

/**
 * This class defines the object, which will handle the monitoring of the resource defined in the
 * watched folders.
 *  
 * @author joshua Harrison
 */
public class ResourceMonitor extends TimerTask {

	@Inject
	private StackTracePrinter printer;
	
	@Inject
	private MessageProvider provider;
	
	@Inject(optional=true)
	private Logger logger = Logger.getLogger(ResourceMonitor.class);
	
	private ArrayList<File> foldersToWatch = new ArrayList<File>();
	private ResourceManager mgr;
	
	public void setFolderAndManager(List<File> folders, ResourceManager mgr) {
		foldersToWatch.addAll(folders);
		this.mgr = mgr;
	}
	
	/**
	 * This TimerTask updates the resource manager when managed resources
	 * change.  This is to prevent the servlet from having to wait on diffs to
	 * be generated at request time.  
	 */
	@Override
	public void run() {
		checkFolders();
	}
	
	private void checkFolders() {
		for (File folder : foldersToWatch) {
			checkForChanges(folder);
		}
	}
	
	private void checkForChanges(File folder) {
		for (File child : folder.listFiles()) {
			if (child.isDirectory()) {
				checkForChanges(child);
			} else {
				try {
					if (!mgr.isManaged(child) ||
					    mgr.hasResourceChanged(child)) {
						mgr.putResource(child);
					}
				} catch (Exception exc) {
					provider.error(logger, "resourcemonitor.resouceerror",
							       child.getAbsolutePath());
					printer.print(exc);
				}
			}
		}
	}
}
