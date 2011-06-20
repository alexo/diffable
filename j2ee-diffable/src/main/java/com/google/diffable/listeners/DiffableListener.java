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
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.google.diffable.Constants;
import com.google.diffable.config.BaseModule;
import com.google.diffable.config.MessageProvider;
import com.google.diffable.data.DiffableContext;
import com.google.diffable.data.ResourceManager;
import com.google.diffable.exceptions.ResourceManagerException;
import com.google.diffable.exceptions.StackTracePrinter;
import com.google.diffable.utils.IOUtils;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

/**
 * The servlet context listener which handle the lifecycle of the resource monitor and the Guice injector 
 * based on the servlet context creation or destruction.
 * 
 * @author Joshua Harrison
 */
public class DiffableListener implements ServletContextListener {
	
	@Inject(optional=true)
	private StackTracePrinter printer;
	
	@Inject(optional=true)
	private ResourceManager mgr = null;
	
	@Inject(optional=true)
	private Logger logger = Logger.getLogger(DiffableListener.class);
	
	@Inject(optional=true)
	private MessageProvider provider;
	
	@Inject(optional=true)
	private ResourceMonitor monitor;
	
	// The interval to wait between checking for changes in managed resources.
	// It is interpreted in milliseconds.
	@Inject(optional=true) @Named("ResourceMonitorInterval")
	private int interval = 2000;
	
	private BaseModule baseModule = null;
	
	private Injector inj = null;
	
	private Timer timer = new Timer("folderMonitor", true);
	
	public void contextDestroyed(ServletContextEvent ctxEvent) {
		timer.cancel();
	}
	
	public void contextInitialized(ServletContextEvent ctxEvent) {
		ServletContext ctx = ctxEvent.getServletContext();
	    baseModule = new BaseModule();
	    
		// Initialize Guice provided properties.
		inj = initializeInjectedProperties(ctx, baseModule);
		
		// Save the injector for later use by Diffable.
		ctx.setAttribute(Constants.DIFFABLE_GUICE_INJECTOR, inj);
		
		// Inject members into the listener via Guice.
		inj.getMembersInjector(DiffableListener.class).injectMembers(this);
		
		// Initialize the diffable context 
		DiffableContext diffableCtx = new DiffableContext();
		ctx.setAttribute(Constants.DIFFABLE_CONTEXT, diffableCtx);
		
		// Initialize the resource manager. For testing, the DiffableListener
		// is retrieved via Guice with a mock ResourceManager injected already,
		// which is why this conditional check is performed.
		if (mgr == null) {
			mgr = inj.getInstance(ResourceManager.class);
		}
		
		// Initialize the resource.
		try {
			String webAppBaseDir = ctx.getRealPath("/");
			mgr.initialize(webAppBaseDir, diffableCtx);
		} catch (ResourceManagerException exc) {
			provider.error(logger, "resourcestore.problem");
			printer.print(exc);
		}
		
		// Retrieve the directories containing resources to be managed and
		// crawl them for resources, adding them to the resource manager as
		// they are found.  If the path begins with a slash, it will be
		// interpreted as absolute. Otherwise, it will be interpreted as
		// relative to the web app directory. If multiple folders are specified,
		// they should be separated by commas.
		String currentPath = ctx.getRealPath("");
		String resourceFolders = ctx.getInitParameter("ResourceFolders");
		ArrayList<File> foundFolders = new ArrayList<File>();
		
		if (resourceFolders == null) {
			String error =
				provider.getMessage(
					"ErrorMessages", "resourcefolders.none");
			logger.error(error);
		} else {
			String[] folders = resourceFolders.split(",");
			for (String path : folders) {
				File folder = path.startsWith("/") ?
					new File(path) :
					new File(currentPath + File.separator + path);
				if (!folder.exists()) {
					provider.error(logger, "resourcefolders.nonexistent",
								   folder.getAbsolutePath());
				} else if (!folder.isDirectory()) {
					provider.error(logger, "resourcefolders.notfolder",
								   folder.getAbsolutePath());
				} else {
					foundFolders.add(folder);
				}
			}
			// Set the folders on the DiffableTag so it can locate resources.
			//DiffableResourceTag.setFolder(foundFolders);
			diffableCtx.setFolder(foundFolders);
			
			// Start up the monitoring thread.
			monitor.setFolderAndManager(foundFolders, mgr);
			timer.schedule(monitor, 0, interval);
		}
		
		// Get the servlet prefix initialization parameter.  This is mandatory
		// for correctly linking to managed resources, and an exception is
		// thrown if this parameter is not defined.
		String servletPrefix = ctx.getInitParameter("ServletPrefix");
		if (servletPrefix != null) {
			diffableCtx.setServletPrefix(servletPrefix);
		} else {
			provider.error(logger, "servlet.noservletprefix");
		}
		
	}
	
	/**
	 * Private function for initializing Guice provided member variables. Since
	 * the listener is initialized by the servlet container, this needs to be
	 * performed manually outside of testing.
	 */
	protected Injector initializeInjectedProperties(ServletContext ctx, 
			                                        BaseModule module) {
		// Attempt to load the configuration properties if provided by the
		// web.xml under 'DiffableConfigProperties'.  If the file exists, use
		// it to initialize the module.
		String propFilePath = ctx.getInitParameter("DiffableConfigProperties");
		String currentPath = ctx.getRealPath("");
		if (propFilePath != null) {
			File propFile = propFilePath.startsWith("/") ?
					new File(propFilePath) :
			        new File(currentPath + File.separator + propFilePath);
			FileInputStream is = null;
			try {
				Properties props = new Properties();
				is = new FileInputStream(propFile);
				props.load(is);
				module.setProperties(props);
			} catch (Exception exc) {
				new MessageProvider().error(logger, 
						                    "module.propfile",
						                    propFile.getAbsolutePath());
				new StackTracePrinter().print(exc);
			}finally{
				IOUtils.close(is);
			}
		}
		return Guice.createInjector(module);
	}
}
