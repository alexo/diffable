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

/**
 * NOTE(joshharrison) This class is minimized for production using the closure
 * compiler.jar with the following command:
 * java -jar compiler.jar --js {file_location} --js_output_file {file_location}.min.js \
 *     --compilation_level ADVANCED_OPTIMIZATIONS
 */

/**
 * The syncLoad script loads a script into the page synchronously. When
 * document.write is called from JS while the page is still loading, the
 * rendering will block until the HTTP request returns and the JS is
 * loaded or an error has occurred. Note that using synchronous loading
 * means the diffable resource must be included in before the body onLoad
 * is fired or the page will be destroyed by the document.write call.
 * @param {string} url The URL of the resource to request.
 * @private
 */
function syncLoad_(url) {
	document.write('<SCR'+'IPT src="' + url + '"><\/SCR'+'IPT>');
};

/**
 * The asyncLoad will add a script to the page by appending it to the
 * document head. This, of course, means it must appear after the
 * head has been rendered.  It does not block execution in any way.
 * @param {string} url The URL of the resource to request.
 * @private
 */
function asyncLoad_(url) {
	var head = document.getElementsByTagName('head')[0];
	var script = document.createElement('script');
	script.src = url;
	head.appendChild(script);
};

/**
 * Returns a boolean indicating whether HTML5 local storage is
 * available.
 * @return {boolean}
 * @private
 */
function localStorageAvailable_() {
	try {
		return 'localStorage' in window && 
		    window['localStorage'] !== null;
	} catch (e) {
		return false;
	}
};

/**
 * This function writes a script to the page. Because it uses
 * document.write, it prevent the page from rendering until the
 * script has been written out, which prevents any race conditions
 * that can occur from loading a script immediately from local
 * storage.
 * 
 * @param {string} content The stringified JS to write out.
 * @private
 */
function syncLoadContent_(content) {
	document.write('<SCR' + 'IPT>' + content + '</SCR' + 'IPT>');
};

/**
 * The constructor takes no resource-specific options as it will be behaving
 * as a singleton.
 * 
 * @constructor
 * @private
 * @returns {DiffableBootstrap}
 */
function DiffableBootstrap() {
	/**
	 * The loadingList_ stores the resources currently being loaded.
	 * @type {Object.<string, string>} 
	 * @private
	 */
	this.loadingList_ = {};
}

/**
 * This function is used to add the resource referenced by the url by
 * retrieving it from local storage, when available, or adding a script
 * tag to the page.
 * @param {string} url The url of the resource. Also serves as the key
 *     in local storage.
 */
DiffableBootstrap.prototype.addResource = function(url) {
	var urlParts = url.split("/");
	var resourceHash = urlParts[urlParts.length - 1];
	if (localStorageAvailable_()) {
		var storedVersion = localStorage.getItem(resourceHash + '.cv');
		if (storedVersion) {
			this.loadingList_[resourceHash] =
				localStorage[resourceHash + '.code'];
			if (storedVersion == window['diffable'][resourceHash]['cv']) {
				this.applyAndExecute(resourceHash);
			} else {
				var diffUrl = this.getDiffName_(resourceHash,
						                        storedVersion);
				asyncLoad_(diffUrl);
			}
		} else {
			asyncLoad_(url);
		}
	}
};

/**
 * A Diffable resource is loaded into the page by a call to this function.
 * The DictionaryBootstrap template wraps the necessary information with a
 * call to this function and serves it up when the resource is requested.
 * @param {string} identifier An identifying string unique to this Diffable
 *     resource.  For instance, for the j2ee implementation, it will be the
 *     auto-generated hash of the resource.
 * @param {string} code The stringified code of the given resource.
 * @param {string} version The version of this resource corresponding to the
 *     code passed in. 
 */
DiffableBootstrap.prototype.bootstrap = function(identifier, code, version) {
	// The code is stored once in the loading list, both to indicate this resource
	// is currently being loaded by Diffable and to prevent pass by value calls.
	this.loadingList_[identifier] = code;
	if (window['diffable'][identifier]['cv'] == version) {
		this.applyAndExecute(identifier);
	} else {
		var diffUrl = this.getDiffName_(identifier, version);
		var syncType = window['diffable'][identifier]['sync'];
		syncType ? syncLoad_(diffUrl) : asyncLoad_(diffUrl);
	}
};

/**
 * Utility function for constructing a diff resource name.
 * @param {string} identifier The resource identifier.
 * @param {string} version The bootstrap version.
 * @return {string} The name to use when requesting a diff for a given
 *     resource.
 * @private
 */
DiffableBootstrap.prototype.getDiffName_ = function(identifier, version) {
	return window['diffable'][identifier]['diff_url'] +
    	identifier + "_" + version + "_" +
    	window['diffable'][identifier]['cv'] + ".diff";
};

/**
 * A helper function for evaluating JS in the global context.
 * http://weblogs.java.net/blog/driscoll/archive/2009/09/08/eval-javascript-global-context
 * 
 * @param {string} identifier The identifier of the resource to eval. The source
 *     code exists in loadList_ when dereferenced by identifier.
 * @private
 */
DiffableBootstrap.prototype.globalEval_ = function(identifier) {
	var source = null;
	source = this.loadingList_[identifier];
	if (window.execScript) {
		window.execScript(source);
	} else {
		var fn = function() { window.eval.call(window, source); };
        fn();
	}
};

/**
 * The applyAndExecute function is used to apply a diff, if one is passed in,
 * and eval the resulting code.
 * 
 * @param {string} identifier The identifier of the resource whose code should
 *     be patched if necessary, and eval'd.
 * @param {Object} opt_diff An optional Diffable JSON diff as generated by
 *     VCDiff.
 */
DiffableBootstrap.prototype.applyAndExecute = function(identifier, opt_diff) {
	try {
		if (opt_diff) {
			this.apply_(identifier, opt_diff);
		}
        if (localStorageAvailable_() &&
            (opt_diff || !localStorage['identifier'])) {
			localStorage[identifier + '.cv'] =
				window['diffable'][identifier]['cv'];
			localStorage[identifier + '.code'] =
				this.loadingList_[identifier];
        }
		this.globalEval_(identifier);
	} finally {
		// No matter what happens with the patching or eval, this resource has
		// attempted a load and should be deleted from the pending loads list.
		delete this.loadingList_[identifier];
	}
};

/**
 * Function for applying a VCDiff patch to a given dictionary.
 * 
 * @param {string} identifier The identifier of the resource to be patched.
 * @param {Object} diff The JSON patch generated by the differ.
 * @private
 */
DiffableBootstrap.prototype.apply_ = function(identifier, diff) {
	var output = [];
	var dict = this.loadingList_[identifier];
	for (var i = 0, n = diff.length; i < n; i++) {
		var currentInstruction = diff[i];
		if (typeof currentInstruction == 'number') {
			var nextInstruction = diff[i + 1]; 
			// If the current diff operation is a number, then use it plus the next
			// one to perform a copy to the output array from the current
			// dictionary. Before performing the copy, confirm that the start and
			// end indexes form a valid substring.
			if (currentInstruction < 0 || currentInstruction >= dict.length) {
				throw identifier + ': Invalid start index: ' + currentInstruction;
			} else if (currentInstruction + nextInstruction > dict.length) {
				throw (identifier + ': Invalid end index: ' + (currentInstruction + nextInstruction));
			} output.push(
				dict.substring(currentInstruction, currentInstruction + nextInstruction));
			// Advance the diff index again since two entries in a row are used to
			// perform a copy command.
			i++;
		} else if (typeof currentInstruction == 'string') {
			// Otherwise the current operation is an add, in which case the current
			// string should be copied directly to the output.
			output.push(currentInstruction);
		}
	}
	output = output.join('');
	this.loadingList_[identifier] = output;
};
var djs = new DiffableBootstrap();
window['diffable'] = {};
window['diffable']['addResource'] = function(){ djs.addResource.apply(djs, arguments); }
window['diffable']['bootstrap'] = function(){ djs.bootstrap.apply(djs, arguments); }
window['diffable']['applyAndExecute'] = function(){ djs.applyAndExecute.apply(djs, arguments); }