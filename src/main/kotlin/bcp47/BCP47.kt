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

import java.util.*

/**
 * Class for BCP47 tags.
 *
 * @see <a href="https://www.ietf.org/rfc/bcp/bcp47.txt">BCP47 Standards>
 * @author Adrian Panton
 */
object BCP47 {

    /**
     * Class to store an extension language subtag or a private use language
     * subtag.
     *
     * @property singleton The singleton of the extension. If value is 'x' or
     *     'X' then this a private use language subtag, otherwise this is an
     *     extension language subtag.
     */
    private class ExtensionSubTag(val singleton: Char) {

        // The extension language subtag.
        var extension = ""

        override fun toString(): String {
            return "$singleton-$extension"
        }
    }

    /* Enumerator for different types of language sub tags, in the order they should be found in a BCP47 language tag. */
    enum class SubTags { LANGUAGE, EXTENDED, SCRIPT, REGION, VARIANT, EXTENSION, PRIVATE_USE }

    // The original BCP47 language tag.
    private var bcp47Tag: String = ""

    // Return true if BCP47 language tag has a blank language tag, false otherwise.
    private var hasBlankTag = false

    // Return true if BCP47 language tag is well-formed, false otherwise.
    private var isWellFormed = true

    // Return true if sub tags are in the correct order, false otherwise.
    private var inCorrectOrder = false

    // Return true if BCP24 language tag is deprecated, false otherwise.
    private val isDeprecated: Boolean
        get() = ianaSubTags.getDeprecatedSubTag(bcp47Tag) != null

    // Return a list of illegal characters found in BCP47 language tag.
    private val illegalCharacters: List<String>
        get() {
            val illegalChars = mutableListOf<String>()

            // Only 'a' to 'z', 'A to 'Z' and '-' are valid characters.
            bcp47Tag.forEach { c ->
                if (!(c.isLetterOrDigit() || c == '-')) illegalChars.add(c.toString())
            }
            return illegalChars
        }

    // Return a list of extension sub tags.
    private val extensionTags = mutableListOf<ExtensionSubTag>()

    // Return a list of private use sub tags.
    private val privateUseTags = mutableListOf<ExtensionSubTag>()

    // IANA sub language tags data.
    private val ianaSubTags = IANASubTags()

    /**
     * Now parse a BCP47 tag.
     *
     * @param bcp47Tag The BCP47 tag to parse.
     * @return A class Results with results of BCP47 language tag after being
     *     parsed.
     */
    fun parse(bcp47Tag: String): Results {

        BCP47.bcp47Tag = bcp47Tag
        hasBlankTag = false
        isWellFormed = true
        inCorrectOrder = false

        extensionTags.clear()
        privateUseTags.clear()

        val results = Results()

        // Check to see if BCP47 language tag is not empty
        if (bcp47Tag.isNotEmpty()) {

            // Check no illegal characters are to be found in BCP47 language tag.
            if (illegalCharacters.isEmpty()) {

                // Check that BCP47 language has not been deprecated.
                if (!isDeprecated) {

                    // Now split BCP47 tag into  it subtag list.
                    val subTagList = bcp47Tag.split("-").toMutableList()

                    // First tag is always a language tag.
                    results.languageTag = subTagList[0]

                    // Remove language subtag from list.
                    subTagList.removeAt(0)

                    // The extension, or private use subtag being extracted, or null if none are being extracted.
                    var extensionSubTag: ExtensionSubTag? = null

                    // The last type of Subtag added.
                    var lastTypeAdded = SubTags.LANGUAGE
                    // The current type of subtag being added.
                    var currentTypeAdded = SubTags.LANGUAGE

                    // Now process all language sub tags.
                    for (subtag in subTagList) {

                        // Check if previous tag was a single tag.
                        if (extensionSubTag != null) {

                            extensionSubTag.extension = subtag
                            // Reset to not extracting an extension or private use language subtag.
                            extensionSubTag = null

                            continue
                        }

                        when (subtag.length) {
                            // Empty tags.
                            0 -> hasBlankTag = true
                            // Singleton language subtag.
                            1 -> {
                                extensionSubTag = ExtensionSubTag(subtag[0])
                                // Check if private use subtag or extension subtag.
                                if (subtag.equals("x", true)) {
                                    currentTypeAdded = SubTags.PRIVATE_USE
                                    privateUseTags.add(extensionSubTag)
                                } else {
                                    currentTypeAdded = SubTags.EXTENSION
                                    extensionTags.add(extensionSubTag)
                                }
                            }
                            // Region language subtag.
                            2 -> {
                                currentTypeAdded = SubTags.REGION
                                results.regionTags.add(subtag)
                            }
                            // Extended, or region language subtag.
                            3 ->
                                if (onlyAlpha(subtag)) {
                                    currentTypeAdded = SubTags.EXTENDED
                                    results.extendedTags.add(subtag)
                                } else {
                                    currentTypeAdded = SubTags.REGION
                                    results.regionTags.add(subtag)
                                }
                            // Script, or variant language subtag.
                            4 ->
                                if (onlyAlpha(subtag)) {
                                    currentTypeAdded = SubTags.SCRIPT
                                    results.scriptTags.add(subtag)
                                } else {
                                    currentTypeAdded = SubTags.VARIANT
                                    results.variantTags.add(subtag)
                                }
                            // Variant language sub tag
                            else -> {
                                currentTypeAdded = SubTags.VARIANT
                                results.variantTags.add(subtag)
                            }
                        }

                        // Check that tags are in the correct order in BCP47 language tag.
                        if (lastTypeAdded > currentTypeAdded)
                            inCorrectOrder = true

                        lastTypeAdded = currentTypeAdded
                    }
                }
            } else
                isWellFormed = false
        }

        validate(results)
        canonicalize(results)
        extensionTags.forEach { results.extensionTags.add(it.toString()) }
        privateUseTags.forEach { results.privateUseTags.add(it.toString()) }
        results.isValid = results.canonicalize != null
        return results
    }

    /**
     * Return a Boolean value depending on whether a region subtag is a private
     * region subtag.
     *
     * @param subtag The region subtag to check.
     * @return True if region subtag is a private region subtag, otherwise
     *     false.
     */
    private fun isPrivateRegion(subtag: String): Boolean {
        val region = subtag.uppercase(Locale.getDefault())

        return region == "AA" || region == "ZZ" || (region >= "QM" && region <= "QZ") ||
                (region >= "XA" && region <= "XZ")
    }

    /**
     * Return a Boolean value on whether a language subtag is a private
     * language subtag.
     *
     * @param subtag The language subtag to check.
     * @return True if language subtag is a private language subtag, otherwise
     *     false.
     */
    private fun isPrivateLanguage(subtag: String): Boolean {

        val language = subtag.lowercase(Locale.getDefault())

        // Check language subtag is a private language tag.
        return language >= "qaa" && language <= "qtz"
    }

    /**
     * Return a Boolean value on whether a script subtag is a private language
     * subtag.
     *
     * @param subtag The script subtag to check.
     * @return True if language subtag is a private language subtag, otherwise
     *     false.
     */
    private fun isPrivateScript(subtag: String): Boolean {

        val script = subtag.lowercase(Locale.getDefault())
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        return script >= "Qaaa" && script <= "Qabx"
    }

    /**
     * Validate non-specific parts of BCP47 language tag.
     *
     * @param results The class to store results from validation.
     * @return True if serious errors where found in language tag, false
     *     otherwise.
     */
    private fun validateGeneral(results: Results): Boolean {

        // Check if BCP47 language is deprecated.
        if (isDeprecated) {
            // Now generate error message as BCP47 language tag is deprecated.
            val deprecatedTag = ianaSubTags.getDeprecatedSubTag(bcp47Tag)
            val sb = StringBuilder()

            sb.append("Deprecated language tag \"$bcp47Tag\" ")

            if (deprecatedTag != null && deprecatedTag.preferredValue.isNotEmpty())
                sb.append("use \"${deprecatedTag.preferredValue}\".")
            else
                sb.append("do not use.")

            results.errorMessages.add(sb.toString())
            return true
        }

        // Check for illegal characters.
        if (illegalCharacters.isNotEmpty()) {
            results.errorMessages.add(
                "Found illegal characters:\" ${illegalCharacters.joinToString(", ")}\" in " +
                        "language tag."
            )
            isWellFormed = false
            return true
        }

        // Check if language tags are correctly order.
        if (inCorrectOrder) {

            // Use to rebuilt language sub tags in the correct order error message.
            val sb = StringBuilder()

            sb.append("Language sub tags incorrectly order. Should be \"${results.languageTag}")

            // Append extended sub tags.
            if (results.extendedTags.isNotEmpty())
                results.extendedTags.forEach { sb.append("-$it") }

            // Append script sub tags.
            if (results.scriptTags.isNotEmpty())
                results.scriptTags.forEach { sb.append("-$it") }

            // Append region sub tags.
            if (results.regionTags.isNotEmpty())
                results.regionTags.forEach { sb.append("-$it") }

            // Append variant sub tags.
            if (results.variantTags.isNotEmpty())
                results.variantTags.forEach { sb.append("-$it") }

            // Append extension sub tags.
            if (extensionTags.isNotEmpty())
                extensionTags.forEach { sb.append("-${it}") }

            // Append private use sub tags.
            if (privateUseTags.isNotEmpty())
                privateUseTags.forEach { sb.append("-${it}") }

            sb.append("\".")

            results.errorMessages.add(sb.toString())
            isWellFormed = false

        }

        if (hasBlankTag) {
            results.errorMessages.add("Language tag has blank subtag(s) caused by more than one contiguous hyphen.")
            isWellFormed = false
        }

        return false
    }

    /**
     * Validate language subtag.
     *
     * @param results The class to store results from validation.
     */
    private fun validateLanguage(results: Results) {

        if (ianaSubTags.getLanguageSubTab(results.languageTag) == null && !isPrivateLanguage(results.languageTag)) {
            results.errorMessages.add("Language subtag \"${results.languageTag}\" is not valid")
            isWellFormed = false
        }
    }

    /**
     * Validate extended sub tags.
     *
     * @param results The class to store results from validation.
     */
    private fun validateExtendedSubtag(results: Results) {

        // Check to see if there is more than one extended subtag.
        if (results.extendedTags.size > 1) {
            isWellFormed = false
            results.errorMessages.add(
                "More than one extended language subtag found" +
                        " \"${results.extendedTags.joinToString(", ")}\", only one is allowed."
            )
        }

        // Check to see if extended tags are valid IANA extended tags.
        val extendedInvalidList = mutableListOf<String>()
        results.extendedTags.forEach {
            val extendedLanguageSubTab = ianaSubTags.getExtendedLanguageSubTab(it)

            // Check valid if not valid IANA region subtag.
            if (extendedLanguageSubTab == null) {
                // Not private region so add to invalid list.
                extendedInvalidList.add(it)
            } else {

                var isPrefix = false

                // Valid extended tag so check used with correct prefix (language tag).
                extendedLanguageSubTab.prefix.forEach { tag ->
                    if (tag.equals(results.languageTag, true)) isPrefix = true
                }

                // If language is not a prefix for extended tag this is an error.
                if (!isPrefix) {
                    results.errorMessages.add(
                        "Extended subtag \"$it\" should not be used with language subtag" +
                                " \"${results.languageTag}\"."
                    )
                    isWellFormed = false
                }
            }
        }

        // Generate error message for invalid extended language sub tags.
        if (extendedInvalidList.isNotEmpty()) {
            results.errorMessages.add(
                "Extended subtag(s) \"${extendedInvalidList.joinToString(", ")}\" " +
                        "are not valid."
            )
            isWellFormed = false
        }
    }

    /**
     * Validate script sub tags.
     *
     * @param results The class to store results from validation.
     */
    private fun validateScriptSubtag(results: Results) {

        // Check to see if there is more than one script subtag.
        if (results.scriptTags.size > 1) {
            isWellFormed = false
            results.errorMessages.add(
                "More than one script subtag found \"${results.scriptTags.joinToString(", ")}\"" +
                        ", only one is allowed."
            )
        }

        // Check to see if script sub tags are valid IANA region tags.
        val scriptInvalidList = mutableListOf<String>()
        results.scriptTags.forEach {
            // Check valid if not valid IANA region subtag.
            if (ianaSubTags.getScriptSubTab(it) == null) {
                // Check if it is private region subtag.
                if (!isPrivateScript(it))
                // Not private region so add to invalid list.
                    scriptInvalidList.add(it)
            }
        }

        // Generate error message for invalid region sub tags.
        if (scriptInvalidList.isNotEmpty()) {
            results.errorMessages.add(
                "Script subtag(s) \"${scriptInvalidList.joinToString(", ")}\" " +
                        "are not valid."
            )
            isWellFormed = false
        }
    }

    /**
     * Validate region sub tags.
     *
     * @param results The class to store results from validation.
     */
    private fun validateRegionSubtag(results: Results) {

        // Check to see if there is more than one region subtag.
        if (results.regionTags.size > 1) {
            isWellFormed = false
            results.errorMessages.add(
                "More than one region subtag found \"${results.regionTags.joinToString(", ")}\"" +
                        ", only one is allowed."
            )
        }

        // Check to see if regions sub tags are valid IANA region tags.
        val regionInvalidList = mutableListOf<String>()
        results.regionTags.forEach {
            // Check valid if not valid IANA region subtag.
            if (ianaSubTags.getRegionLanguageSubTab(it) == null) {
                // Check if it is private region subtag.
                if (!isPrivateRegion(it))
                // Not private region so add to invalid list.
                    regionInvalidList.add(it)
            }
        }

        // Generate error message for invalid region sub tags.
        if (regionInvalidList.isNotEmpty()) {
            results.errorMessages.add(
                "Region subtag(s) \"${regionInvalidList.joinToString(", ")}\"" +
                        " are not valid."
            )
            isWellFormed = false
        }
    }

    /**
     * Validate variant sub tags.
     *
     * @param results The class to store results from validation.
     */
    private fun validateVariants(results: Results) {

        // Keep a list of duplicate variants.
        val duplicateVariants = mutableMapOf<String, String>()

        // Check for duplicates.
        results.variantTags.forEach { variant ->
            if (results.variantTags.count { it.equals(variant, true) } > 1)
                duplicateVariants[variant] = variant
        }

        // Generate error messages for  list for duplicate variants.
        if (duplicateVariants.isNotEmpty()) {
            duplicateVariants.forEach { results.errorMessages.add("Duplicate variant subtag: ${it.key}") }
            isWellFormed = false
        }

        // Check to see if variant sub tags are valid IANA region tags.
        val variantInvalidList = mutableListOf<String>()
        results.variantTags.forEach {
            // Check valid if not valid IANA variant subtag.
            if (ianaSubTags.getVariantSubTab(it) == null) {
                variantInvalidList.add(it)
            }
        }

        // Generate error message for invalid region sub tags.
        if (variantInvalidList.isNotEmpty()) {
            results.errorMessages.add(
                "Variant subtag(s) \"${variantInvalidList.joinToString(", ")}\"" +
                        " are not valid."
            )
            isWellFormed = false
        }

        // Check if errors have already occurred if so quit.
        if (duplicateVariants.isNotEmpty() || variantInvalidList.isNotEmpty()) return

        // Check preceding tags are valid for variant.

        // language tag is the first preceding tag.
        var precedingTag = results.languageTag

        // Now process in each variant and check if follows the expected sub tags.
        results.variantTags.forEach { variant ->

            val variantIANATag = ianaSubTags.getVariantSubTab(variant)

            // This at this point should never be null.
            if (variantIANATag != null) {

                // If true then found a correct list of preceding tags for variant.
                var verifyPrecedingTags = false

                // Check if one of variant tag prefix's matches the valid preceding tags.
                for (prefix in variantIANATag.prefix) {

                    if (prefix.equals(precedingTag, true))
                        verifyPrecedingTags = true
                }

                // Check we have not matched a variant prefix with the preceding tags so create an error message.
                if (!verifyPrecedingTags) {
                    val prefixList = variantIANATag.prefix.toMutableList()
                    // Add this variant to variant prefix's.
                    for (n in 0 until prefixList.size) {
                        prefixList[n] += "-$variant"
                    }

                    results.errorMessages.add(
                        "Sub tags preceding variant \"$variant\" did not match one of the" +
                                " following pattern(s): ${prefixList.joinToString(", ")}."
                    )
                    isWellFormed = false
                }

                // Add to variant preceding tag.
                precedingTag += "-$variant"
            }
        }
    }

    /**
     * Validate private use sub tags.
     *
     * @param results The class to store results from validation.
     */
    private fun validatePrivateUse(results: Results) {

        // Check to see if there is more than one private use subtag.
        if (privateUseTags.size > 1) {
            isWellFormed = false
            val privateList = mutableListOf<String>()
            privateUseTags.forEach { privateList.add(it.toString()) }
            results.errorMessages.add(
                "More than one private use subtag found \"${privateList.joinToString(", ")}\"" +
                        ", only one is allowed."
            )
        }
    }

    /**
     * Preforms validation on language sub tags parsed from a BCP47 language
     * tags.
     *
     * @param results The class to store results from validation.
     */
    private fun validate(results: Results) {

        // If they serious general errors do not do any more validations.
        if (validateGeneral(results)) return

        validateLanguage(results)
        validateRegionSubtag(results)
        validateExtendedSubtag(results)
        validateScriptSubtag(results)
        validateVariants(results)
        validatePrivateUse(results)
    }

    /**
     * Canonicalize a BCP47 language tag.
     *
     * @param results The class that contains the language sub tags to
     *     canonicalize. Stores canonicalize language tag in the results.
     */
    private fun canonicalize(results: Results) {

        // Can not canonicalize a BCP47 language tag unless it is well-formed.
        if (!isWellFormed) return

        results.canonicalize = ""

        // Return an empty string for empty BCP47 tags.
        if (bcp47Tag.isEmpty())
            return

        // If BCP47 language tag is deprecate than used the preferred value, if no preferred value set
        // then canonicalize tag is just a lowercase version of language tag.
        if (isDeprecated) {
            val ianaTag = ianaSubTags.getDeprecatedSubTag(bcp47Tag)
            return if (ianaTag!!.preferredValue.isEmpty())
                results.canonicalize = ianaTag.tagOrSubTag
            else
                results.canonicalize = ianaTag.preferredValue
        }

        var language = results.languageTag

        // Get language IANA subtag.
        val languageIANA = ianaSubTags.getLanguageSubTab(language)

        // Check if there is a preferred language subtag if so used this as language subtag.
        if (languageIANA != null && languageIANA.preferredValue.isNotEmpty())
            language = languageIANA.preferredValue

        // Check if there is an extended language subtag if so processed it.
        if (results.extendedTags.isNotEmpty()) {
            // Get extended language IANA subtag.
            val extendedIANA = ianaSubTags.getExtendedLanguageSubTab(results.extendedTags[0])

            // Language subtag is replaced by extended language subtag preferred value if it has one, and extended
            // language subtag prefix is the same as language subtag.
            if (extendedIANA != null)
                if (extendedIANA.preferredValue.isNotEmpty() && language.equals(extendedIANA.prefix[0], true))
                    language = extendedIANA.preferredValue

        }

        results.canonicalize = language.lowercase(Locale.getDefault())

        // Now canonicalize script subtag.
        if (results.scriptTags.isNotEmpty())
        // If script subtag is the language subtag suppress script then script subtag can be dropped as it is the
        // assumed to be the default script for the language subtag, otherwise added.
            if (languageIANA != null && !languageIANA.suppressScript.equals(results.scriptTags[0], true))
                results.canonicalize += "-${
                    results.scriptTags[0].lowercase(Locale.getDefault())
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }"

        // Now canonicalize region subtag.
        if (results.regionTags.isNotEmpty()) {
            val regionSubtag = ianaSubTags.getRegionLanguageSubTab(results.regionTags[0])

            // If no region IANA tag found just append an uppercase region tag,  if found and a preferred
            // value exists append this value, otherwise append the subtag value.
            when {
                regionSubtag == null -> results.canonicalize += "-${results.regionTags[0].uppercase(Locale.getDefault())}"
                regionSubtag.preferredValue.isNotEmpty() -> results.canonicalize += "-${regionSubtag.preferredValue}"
                else -> results.canonicalize += "-${regionSubtag.tagOrSubTag}"
            }
        }

        // Now canonicalize variants sub tags.
        if (results.variantTags.isNotEmpty()) {
            results.variantTags.forEach { variant ->
                val variantIANA = ianaSubTags.getVariantSubTab(variant)
                if (variantIANA != null)
                    results.canonicalize += "-${variantIANA.tagOrSubTag}"
            }
        }

        // Now canonicalize extension tag by placing in a case-insensitive sort order for singleTon.
        if (extensionTags.isNotEmpty()) {
            // List of canonicalize extension tags.
            val extensionSubtagList = mutableListOf<ExtensionSubTag>()

            extensionSubtagList.addAll(
                extensionTags.sortedWith(
                    compareBy(
                        String.CASE_INSENSITIVE_ORDER
                    ) { it.singleton.toString() }
                )
            )
            extensionSubtagList.forEach { results.canonicalize += "-${it}" }
        }

        // Just add private use to extension subtag list if it exists.
        if (privateUseTags.isNotEmpty())
            results.canonicalize += "-${privateUseTags[0]}"
    }

    /**
     * Checks whether a string only contains alpha characters.
     *
     * @return True if string only contains alpha characters, false otherwise.
     */
    private fun onlyAlpha(s: String): Boolean = s.isNotEmpty() && (s.firstOrNull { !it.isLetter() } == null)

}
