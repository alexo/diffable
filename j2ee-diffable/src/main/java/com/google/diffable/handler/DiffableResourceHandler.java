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
package com.google.diffable.handler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import com.google.diffable.data.ResourceManager;
import com.google.diffable.data.ResourceRequest;
import com.google.diffable.diff.JSONHelper;
import com.google.diffable.exceptions.ResourceManagerException;
import com.google.diffable.scripts.DeltaBootstrapTemplate;
import com.google.diffable.scripts.DictionaryBootstrapTemplate;
import com.google.inject.Inject;

/**
 * This class will handle the diffable request.
 * 
 * @author ibrahim Chaehoi
 */
public class DiffableResourceHandler {

	@Inject
	private ResourceManager mgr;
	
	@Inject
	private DictionaryBootstrapTemplate jsDictWrapper;
	
	@Inject
	private DeltaBootstrapTemplate bootstrapWrapper;
	
	private Calendar jan2000 = Calendar.getInstance();
	
	public DiffableResourceHandler() {
		// Set the 2000 calendar.
		jan2000.set(2000, 1, 1);
	}
	
	/**
	 * Handle the diff resource request
	 * @param request the request
	 * @param resp the response
	 * @throws IOException if an IO exception occurs
	 * @throws ResourceManagerException if a resource manager exception occurs
	 */
	public boolean handleResourceRequest(
		ResourceRequest request, 
		HttpServletResponse resp) throws IOException, ResourceManagerException{
		
		boolean processed = false;
		mgr.getResource(request);
		if (request.getResponse() != null) {
			resp.setStatus(200);
			Calendar now = Calendar.getInstance();
			now.set(Calendar.YEAR, now.get(Calendar.YEAR) + 2);
			// All responses are denoted as last being modified on Jan 1,
			// 2000 to allow for very agressive caching.
			resp.setHeader("Last-Modified",
				new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US).format(
					jan2000.getTime()));
			resp.setHeader("Cache-Control", "public, max-age=63072000");
			resp.setHeader("Expires", 
				new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US).format(
					now.getTime()));
			HashMap<String, String> values = new HashMap<String, String>();
			values.put("{{DJS_RESOURCE_IDENTIFIER}}", request.getResourceHash());
			if (request.isDiff()) {
				values.put("{{DJS_DIFF_CONTENT}}", request.getResponse());
				String response = bootstrapWrapper.render(values);
				resp.setContentLength(response.length());
				resp.getWriter().print(response);
				processed = true;
			} else {
				values.put("{{DJS_CODE}}", JSONHelper.quote(request.getResponse()));
				values.put("{{DJS_BOOTSTRAP_VERSION}}", request.getNewVersionHash());
				String response = jsDictWrapper.render(values);
				resp.setContentLength(response.length());
				resp.getWriter().print(response);
				processed = true;
			}
		}

		return processed;
	}
}
