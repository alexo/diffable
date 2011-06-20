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

import java.util.Properties;

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
public class DiffableModule extends AbstractModule {
	
	private final Properties props;
	
	public DiffableModule() {
		this.props = null;
	}

	public DiffableModule(Properties props) {
		this.props = props;
	}
	
	@Override
	protected void configure() {
		if(props != null)
			Names.bindProperties(binder(), props);
	}
}
