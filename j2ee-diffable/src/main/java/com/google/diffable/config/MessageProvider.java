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

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This class defines the message provider
 * 
 * @author joshua Harrison
 */
public class MessageProvider {

	@Inject(optional=true) @Named("MessageBundlePath")
	private String bundlePath = "com/google/diffable/bundles/";
	
	public MessageProvider(){}
	
	public MessageProvider(String bundlePath) {
		this.bundlePath = bundlePath;
	}
	
	public String getMessage(String propFile, String messageId) {
		ResourceBundle bundle =
			ResourceBundle.getBundle(bundlePath + propFile);
		return bundle.getString(messageId);
	}
	
	public String getMessage(String propFile, String messageId, String...args) {
		ResourceBundle bundle =
			ResourceBundle.getBundle(bundlePath + propFile);
		return String.format(bundle.getString(messageId), (Object[])args);
	}
	
	public String error(String messageId) {
		return getMessage("ErrorMessages", messageId);
	}
	
	public String error(String messageId, String...args) {
		return getMessage("ErrorMessages", messageId, args);
	}
	
	public String debug(String messageId) {
		return getMessage("DebugMessages", messageId);
	}
	
	public String debug(String messageId, String...args) {
		return getMessage("DebugMessages", messageId, args);
	}
	
	public String info(String messageId) {
		return getMessage("InfoMessages", messageId);
	}
	
	public String info(String messageId, String...args) {
		return getMessage("InfoMessages", messageId, args);
	}
	
	public void error(Logger logger, String messageId) {
		logger.error(getMessage("ErrorMessages", messageId));
	}
	
	public void error(Logger logger, String messageId, String...args) {
		logger.error(getMessage("ErrorMessages", messageId, args));
	}
	
	public void debug(Logger logger, String messageId) {
		logger.debug(getMessage("DebugMessages", messageId));
	}
	
	public void debug(Logger logger, String messageId, String...args) {
		logger.debug(getMessage("DebugMessages", messageId, args));
	}
	
	public void info(Logger logger, String messageId) {
		logger.info(getMessage("InfoMessages", messageId));
	}
	
	public void info(Logger logger, String messageId, String...args) {
		logger.info(getMessage("InfoMessages", messageId, args));
	}
}
