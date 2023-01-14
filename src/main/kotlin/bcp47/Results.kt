/*
 *  Copyright (C) 2023 ARP Software
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */

package bcp47

/**
 * Class for holding the results of parsing a BCP47 language tag.
 *
 * @author Adrian Panton
 */
class Results {

    /** Return the language sub tag found. */
    var languageTag = ""

    /** Return a list of extended sub tags. */
    val extendedTags = mutableListOf<String>()

    /** Return the script subtag, or empty string if no script subtag. */
    val scriptTags = mutableListOf<String>()

    /** Return region subtag, or empty string if no region subtag. */
    val regionTags = mutableListOf<String>()

    /** Return a list of variant sub tags. */
    val variantTags = mutableListOf<String>()

    /** Return a list of extension sub tags. */
    val extensionTags = mutableListOf<String>()

    /** Return a list of private use sub tags. */
    val privateUseTags = mutableListOf<String>()

    /** Return the BCP47 language tag canonicalize, or null if failed. */
    var canonicalize: String? = null

    /** Return true if BCP 47 language tag is valid, otherwise false. */
    var isValid = false

    /** Return a list of error messages. */
    val errorMessages = mutableListOf<String>()
}