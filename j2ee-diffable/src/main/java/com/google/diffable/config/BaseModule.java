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
package com.google.diffable.config;

import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * The Diffable guice configuration module. If properties are passed to the
 * module constructor, they are used to override the default implementations
 * with those specified in the file.
 * 
 * @author joshua Harrison
 *
 */
public class BaseModule extends AbstractModule {
	
	private Logger logger = Logger.getLogger(BaseModule.class);
	private Properties props;
	private MessageProvider provider = new MessageProvider();
	
	public BaseModule() {
		this.props = null;
	}

	public BaseModule(Properties props) {
		this.props = props;
	}
	
	public void setProperties(Properties props) {
		this.props = props;
	}
	
	void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	protected void configure() {
		if(props != null) {
			bindImplementationOverrides();
			Names.bindProperties(binder(), props);
		}
	}
	
	/**
	 * This method allows for the overriding of default implementations in the
	 * Guice injector.  It searches for keys that are valid interface classes
	 * and attempts to bind the corresponding value as the implementation class
	 * for the given interface.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void bindImplementationOverrides() {
		Enumeration<?> names = props.propertyNames();
		while (names.hasMoreElements()) {
			String currentName = names.nextElement().toString();
			// Ignore classNotFound for keys as they may be specifying a prop
			// other than an implementation override.
			try {
				Class interfaceClass = Class.forName(currentName);
				// If the key is an interface then attempt to instantiate
				// the corresponding value, check to make sure it implements the
				// interface, and if it does, attempt to set it as the bound
				// implementation.
				if (interfaceClass.isInterface()) {
					String implementation = props.getProperty(currentName);
					try {
						Class implementationClass =
							Class.forName(implementation);
						boolean implementsInterface = false;
						for (Class implemented :
							implementationClass.getInterfaces()) {
							if (implemented.equals(interfaceClass)) {
								implementsInterface = true;
								break;
							}
						}
						if (implementsInterface) {
							bind(interfaceClass).to(implementationClass);
						} else {
							String error = String.format(
								provider.getMessage(
									"ErrorMessages", "module.notimplemented"),
								implementation, currentName);
							logger.error(error);
						}
					} catch (ClassNotFoundException exc) {
						String error = String.format(
							provider.getMessage(
								"ErrorMessages", "module.classnotfound"),
							implementation, currentName);
						logger.error(error);
					}
				}
			} catch (ClassNotFoundException ignore) {}
		}
	}
}
