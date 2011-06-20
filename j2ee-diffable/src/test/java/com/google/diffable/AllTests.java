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

package com.google.diffable;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.google.diffable.config.TestBaseModule;
import com.google.diffable.config.TestMessageProvider;
import com.google.diffable.data.TestFileResourceManager;
import com.google.diffable.diff.vcdiff.TestBlockText;
import com.google.diffable.diff.vcdiff.TestDictionary;
import com.google.diffable.diff.vcdiff.TestVCDiff;
import com.google.diffable.diff.vcdiff.hash.TestRollingHash;
import com.google.diffable.exceptions.TestStackTracePrinter;
 
@RunWith(Suite.class)
@Suite.SuiteClasses({
  TestBaseModule.class,
  TestMessageProvider.class,
  TestFileResourceManager.class,
  TestBlockText.class,
  TestDictionary.class,
  TestVCDiff.class,
  TestRollingHash.class,
  TestStackTracePrinter.class
})

public class AllTests {}