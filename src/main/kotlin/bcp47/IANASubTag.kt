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
 * Class for storing information on a IANA language subtag.
 *
 * @author Adrian Panton
 */
class IANASubTag {

    /** Denotes the type of tag or subtag. */
    var type = ""

    /**
     * When type is of language, extended language, script, region, variant
     * contains the subtag, and when type is grandfathered, or redundant then
     * contains the whole language tag.
     */
    var tagOrSubTag = ""

    /** List of descriptions for subtag or tag. */
    var description = mutableListOf<String>()

    /** The date IANA tag was added. */
    var added = ""

    /**
     * Contains a script subtag that should not be used when forming language
     * tags with this language or extended language subtag.
     */
    var suppressScript = ""

    /** Contains information on the type of language. */
    var scope = ""

    /**
     * Contains a primary language subtag only used by language or extended
     * language sub tags.
     */
    var macroLanguage = ""

    /** Addition information on subtag or tag. */
    var comments = ""

    /**
     * The date subtag or tag was deprecated, empty string means not
     * deprecated.
     */
    var deprecated = ""

    /** The preferred value to used. */
    var preferredValue = ""

    /** Recommended prefix to an extended language, variant language subtag. */
    var prefix = mutableListOf<String>()
}

