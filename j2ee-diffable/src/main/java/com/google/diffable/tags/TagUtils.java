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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import com.google.diffable.Constants;
import com.google.inject.Injector;

/**
 * The TagUtils class contains utility methods to be shared by all
 * Diffable tags.
 * 
 * @author Joshua Harrison
 */
public class TagUtils {

	public static Injector getInjector(PageContext pageContext) {
		return (Injector)pageContext.getServletContext().getAttribute(
				Constants.DIFFABLE_GUICE_INJECTOR);
	}
	
	/**
	 * Returns the current page's PageCoordinator.  If one doesn't yet exist,
	 * a new one is created, otherwise the existing coordinator is returned.
	 *
	 * @param ctx The current JSP page's page context.
	 * @return The current page's PageCoordinator.
	 */
	public static PageCoordinator getCoordinator(PageContext ctx, Injector inj)
	throws JspException {
		PageCoordinator coordinator =
			(PageCoordinator)ctx.getAttribute(Constants.DIFFABLE_PAGE_COORDINATOR);
		if (coordinator != null) {
			return coordinator; 
		} else {
			coordinator = new PageCoordinator(ctx);
			inj.getMembersInjector(PageCoordinator.class).injectMembers(coordinator);
			ctx.setAttribute(Constants.DIFFABLE_PAGE_COORDINATOR, coordinator);
			return coordinator;
		}
	}
}
