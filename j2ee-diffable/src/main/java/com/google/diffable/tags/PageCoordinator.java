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
package com.google.diffable.tags;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import com.google.diffable.scripts.DiffableClientTemplate;
import com.google.inject.Inject;

/**
 * The PageCoordinator is responsible for outputting page level
 * resources such as shared Diffable code.  It is also responsible
 * for enabling communication and coordination between multiple
 * Diffable tags on a given page.
 * 
 * @author Joshua Harrison
 */
public class PageCoordinator {
	
	@Inject
	private DiffableClientTemplate clientTemplate = new DiffableClientTemplate();
	
	private PageContext pageContext;
	
	public PageCoordinator(PageContext pageContext)
	throws JspException {
		this.pageContext = pageContext;
		// Set the response of the page containing this resource to be
		// uncacheable.
		((HttpServletResponse)this.pageContext.getResponse())
			.setHeader("Cache-Control", "private, max-age=0");
	    ((HttpServletResponse)this.pageContext.getResponse())
			.setHeader("Expires", "-1");
	    try {
	    	pageContext.getOut().println("<script type='text/javascript'>");
	    	pageContext.getOut().println(clientTemplate.render());
	    	pageContext.getOut().println("</script>");
	    } catch (IOException exc) {
	    	JspException jspExc = new JspException(
	    		exc.getMessage());
	    	jspExc.setStackTrace(exc.getStackTrace());
	    	throw jspExc;
	    }
	}
}
