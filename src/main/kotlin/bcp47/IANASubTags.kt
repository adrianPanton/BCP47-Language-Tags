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

import java.io.BufferedReader
import java.io.File
import java.util.*

/**
 * Class for IANA language sub tags.
 *
 * IANA data file used is from following source:
 * "https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry".
 *
 * @throws Exception if failed to load IANA file
 * @author Adrian Panton
 */
class IANASubTags {

    companion object {

        // The filename of IANA subtag text file to load from resource folder.
        private const val filename = "/language-subtag-registry.txt"

        // The marker for date file was updated.
        private const val FILE_DATE = "File-Date"

        // The marker for start of new IANA subtag record.
        private const val RECORD_MARKER = "%%"

        // The separator use to split field into field name, field body.
        private const val FIELD_SEPARATOR = ':'

        /* List IANA field names found within a IANA subtag. */
        private const val FN_ADDED = "Added"
        private const val FN_COMMENTS = "Comments"
        private const val FN_DEPRECATED = "Deprecated"
        private const val FN_DESCRIPTION = "Description"
        private const val FN_MACRO_LANGUAGE = "Macrolanguage"
        private const val FN_PREFERRED_VALUE = "Preferred-Value"
        private const val FN_PREFIX = "Prefix"
        private const val FN_SCOPE = "Scope"
        private const val FN_SUBTAG = "Subtag"
        private const val FN_SUPPRESS_SCRIPT = "Suppress-Script"
        private const val FN_TAG = "Tag"
        private const val FN_TYPE = "Type"

        /* List of field bodies for "Type" field name. */
        private const val LANGUAGE = "language"
        private const val EXT_LANG = "extlang"
        private const val SCRIPT = "script"
        private const val REGION = "region"
        private const val VARIANT = "variant"
        private const val GRANDFATHERED = "grandfathered"
        private const val REDUNDANT = "redundant"

        /* Where continue lines are to be added to. Only comments and descriptions continue beyond one line. */
        private const val ADD_TO_NONE = 0
        private const val ADD_TO_COMMENTS = 1
        private const val ADD_TO_DESCRIPTION = 2

    }

    // List of language IANA sub tags that been found.
    private var languageList = mutableMapOf<String, IANASubTag>()

    // List of extended language IANA sub tags that been found.
    private var extendedList = mutableMapOf<String, IANASubTag>()

    // List of script IANA sub tags that been found.
    private var scriptList = mutableMapOf<String, IANASubTag>()

    // List of region IANA sub tags that been found.
    private val regionList = mutableMapOf<String, IANASubTag>()

    // List of variant IANA sub tags that been found.
    private val variantList = mutableMapOf<String, IANASubTag>()

    // List of deprecated IANA sub tags that been found.
    private val deprecatedList = mutableMapOf<String, IANASubTag>()

    /** The date IANA File was updated. */
    private var fileDate: String = ""

    init {
        load()
    }

    /**
     * Return a IANA language subtag.
     *
     * @param tag The language subtag to find.
     * @return A language subtag if found, null otherwise.
     */
    fun getLanguageSubTab(tag: String): IANASubTag? {
        return languageList[tag.lowercase(Locale.getDefault())]
    }

    /**
     * Return a IANA extended language subtag.
     *
     * @param tag The extended language subtag to find.
     * @return An extended language subtag if found, null otherwise.
     */
    fun getExtendedLanguageSubTab(tag: String): IANASubTag? {
        return extendedList[tag.lowercase(Locale.getDefault())]
    }

    /**
     * Return a IANA region subtag.
     *
     * @param tag The region subtag to find.
     * @return An extended language subtag if found, null otherwise.
     */
    fun getRegionLanguageSubTab(tag: String): IANASubTag? {
        return regionList[tag.lowercase(Locale.getDefault())]
    }

    /**
     * Return a IANA script subtag.
     *
     * @param tag The script subtag to find.
     * @return A script subtag if found, null otherwise.
     */
    fun getScriptSubTab(tag: String): IANASubTag? {
        return scriptList[tag.lowercase(Locale.getDefault())]
    }

    /**
     * Return a IANA variant subtag.
     *
     * @param tag The variant subtag to find.
     * @return An extended language subtag if found, null otherwise.
     */
    fun getVariantSubTab(tag: String): IANASubTag? {
        return variantList[tag.lowercase(Locale.getDefault())]
    }

    /**
     * Return a deprecated IANA tag, which is either a grandfather IANA tag or
     * a redundant IANA subtag.
     *
     * @param tag The subtag to find.
     * @return A deprecated subtag if found, null otherwise.
     */
    fun getDeprecatedSubTag(tag: String): IANASubTag? {
        return deprecatedList[tag.lowercase(Locale.getDefault())]
    }

    /** Load IANA sub tags text file and process file into IANA subtag lists. */
    private fun load() {

        // List of IANA sub tags that have been found.
        val ianaTagList = mutableListOf<IANASubTag>()

        lateinit var br: BufferedReader

        // Open IANA text file.
        val path = String::class.java.getResource(filename)?.path ?: throw Exception("Failed to load $filename from " +
                "resource folder.")

        br = File(path).bufferedReader()

        // The subtag being created.
        var tagIANA = IANASubTag()

        // Use to determine were continuation lines are to be too.
        var continueLine = ADD_TO_NONE

        // Process file line by line.
        br.forEachLine { s ->

            // Check if we need to create a new subtag.
            if (s.startsWith(RECORD_MARKER)) {
                tagIANA = IANASubTag()
                ianaTagList.add(tagIANA)
            } else {
                // Check if it starts with a space character, if so it's a continuation of previous line, and
                // add it to correct subtag property.
                if (s.startsWith(" ")) {
                    when (continueLine) {
                        ADD_TO_COMMENTS -> {
                            tagIANA.comments += s.substring(1)
                        }

                        ADD_TO_DESCRIPTION -> {
                            tagIANA.description[tagIANA.description.size - 1] += s.substring(1)
                        }
                    }
                } else {
                    // Check if this is a property
                    if (s.contains(FIELD_SEPARATOR)) {

                        // Divide the property into property name, property value
                        val parts = s.split(FIELD_SEPARATOR)
                        val name = parts[0].trim()
                        val value = parts[1].trim()

                        continueLine = ADD_TO_NONE
                        when (name) {
                            FILE_DATE -> fileDate = value
                            FN_TYPE -> tagIANA.type = value
                            FN_SUBTAG -> tagIANA.tagOrSubTag = value
                            FN_DESCRIPTION -> {
                                tagIANA.description.add(value)
                                continueLine = ADD_TO_DESCRIPTION
                            }

                            FN_ADDED -> tagIANA.added = value
                            FN_SUPPRESS_SCRIPT -> tagIANA.suppressScript = value
                            FN_SCOPE -> tagIANA.scope = value
                            FN_MACRO_LANGUAGE -> tagIANA.macroLanguage = value
                            FN_COMMENTS -> {
                                tagIANA.comments = value
                                continueLine = ADD_TO_COMMENTS
                            }

                            FN_DEPRECATED -> tagIANA.deprecated = value
                            FN_PREFERRED_VALUE -> tagIANA.preferredValue = value
                            FN_PREFIX -> tagIANA.prefix.add(value)
                            FN_TAG -> tagIANA.tagOrSubTag = value
                        }
                    }
                }
            }
        }
        br.close()

        // Now sort IANA tag list into it's separates map lists.
        ianaTagList.forEach {
            when (it.type) {
                LANGUAGE -> languageList[it.tagOrSubTag.lowercase(Locale.getDefault())] = it
                EXT_LANG -> extendedList[it.tagOrSubTag.lowercase(Locale.getDefault())] = it
                SCRIPT -> scriptList[it.tagOrSubTag.lowercase(Locale.getDefault())] = it
                REGION -> regionList[it.tagOrSubTag.lowercase(Locale.getDefault())] = it
                VARIANT -> variantList[it.tagOrSubTag.lowercase(Locale.getDefault())] = it
                GRANDFATHERED -> deprecatedList[it.tagOrSubTag.lowercase(Locale.getDefault())] = it
                REDUNDANT -> deprecatedList[it.tagOrSubTag.lowercase(Locale.getDefault())] = it
            }
        }
    }
}