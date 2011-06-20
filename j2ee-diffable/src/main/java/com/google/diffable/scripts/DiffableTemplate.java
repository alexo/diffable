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
package com.google.diffable.scripts;

import java.io.InputStream;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.diffable.config.MessageProvider;
import com.google.diffable.exceptions.StackTracePrinter;
import com.google.diffable.utils.IOUtils;
import com.google.inject.Inject;

/**
 * The abstract DiffableTemplate class provides a method for wrapping
 * Diffable resources with the necessary Javascript.  It does this in
 * a template fashion by replacing certain template values with the
 * passed in replacement values.  This class provides a shared method
 * for loading and rendering template.
 * 
 * @author Joshua Harrison
 */
public abstract class DiffableTemplate {
	@Inject
	private StackTracePrinter printer;
	
	@Inject
	private MessageProvider provider;
	
	@Inject(optional=true)
	private Logger logger =
		Logger.getLogger(DiffableTemplate.class);

	private String resourceString = null;
	
	/**
	 * Sets the current resourceString by reading in the URI of the
	 * template resource as a string;
	 * 
	 * @param resourceUri The URI on the classpath of the resource to
	 *     use as a template.
	 */
	public void setTemplate(String resourceUri) {
		InputStream in =
			this.getClass().getResourceAsStream(resourceUri);
		try {
			int currentChar;
			StringBuffer content = new StringBuffer();
			while ((currentChar = in.read()) != -1) {
				content.append(Character.toChars(currentChar));
			}
			this.resourceString = content.toString();
		} catch (Exception exc) {
			provider.error(
				logger, "bootstrap.resourceerror", resourceUri);
			printer.print(exc);
		}finally{
			IOUtils.close(in);
		}
	}
	
	/**
	 * Returns the un-edited resource string. Useful for templates
	 * that don't have any values which need to be filled in.
	 * 
	 * @return The template as a string.
	 */
	public String render() {
		return this.resourceString;
	}
	
	/**
	 * Replaces the template place-holders with actual values and
	 * returns the rendered string. NOTE(joshharrison) This function
	 * does not currently fail when a place-holder is not found, so
	 * if you notice any weird undefined JS errors...
	 * 
	 * @param values A map containing place-holder strings as names
	 *     and the string to replace with as values.
	 * @return The template with place-holders filled in by values.
	 */
	public String render(Map<String, String> values) {
		String rendered = resourceString;
		for (String name : values.keySet()) {
			rendered = rendered.replace(name, values.get(name));
		}
		return rendered;
	}
}
