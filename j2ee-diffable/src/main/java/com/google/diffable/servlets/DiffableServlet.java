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
package com.google.diffable.servlets;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.diffable.Constants;
import com.google.diffable.config.MessageProvider;
import com.google.diffable.data.ResourceRequest;
import com.google.diffable.exceptions.StackTracePrinter;
import com.google.diffable.handler.DiffableResourceHandler;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * This class defines the servlet which will handle the request for the diffable resources.
 *  
 * @author joshua Harrison
 */
public class DiffableServlet extends HttpServlet {

	/** The serial version UID */
	private static final long serialVersionUID = -5774775375357121469L;

	@Inject
	/** The stacktrace printer */
	private StackTracePrinter printer;
	
	@Inject
	/** The message provider */
	private MessageProvider provider;
	
	@Inject(optional=true)
	/** The logger */
	private Logger logger = Logger.getLogger(DiffableServlet.class);
	
	@Inject
	/** The diffable resource handler*/
	private DiffableResourceHandler handler;
	
	/** The Guice injector */
	private Injector inj;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		inj = (Injector)
		    config.getServletContext().getAttribute(
		    		Constants.DIFFABLE_GUICE_INJECTOR);
		inj.getMembersInjector(DiffableServlet.class).injectMembers(this);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// Strip the servlet path information, leaving just the resource 
		// name, which includes all information after the servlet path.
		String basePath = req.getContextPath() + req.getServletPath() + "/";
		String requestString = req.getRequestURI().replace(basePath, "");
		// Remove any preceding slashes as these would correspond to empty
		// paths, i.e. path/to//resource would separate into path/to and
		// /resource.  This will fix /resource to be resource.
		while (requestString.startsWith("/")) {
			requestString = requestString.substring(1);
		}
		provider.debug(logger, "servlet.resourcerequest", requestString);
		resp.setStatus(500);
		try {
			ResourceRequest request = inj.getInstance(ResourceRequest.class); 
			request.setRequest(basePath, requestString);
			if(handler.handleResourceRequest(request, resp)){
				resp.setStatus(200);
			}
		} catch (Exception exc) {
			provider.error(logger, "servlet.cantserverequest",
					       req.getRequestURI());
			printer.print(exc);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}
	
}
