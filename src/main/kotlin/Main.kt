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

import bcp47.BCP47.parse
import java.util.*
import kotlin.system.exitProcess

/**
 * Class to allow user to enter a BCP47 language tag then check for BCP47
 * language for errors.
 *
 * @author Adrian Panton
 */
fun main(args: Array<String>) {

    val sc = Scanner(System.`in`)

    while (true) {

        displayMenu()

        // Wait for user to enter a number and skip any other type of data enter.
        while (!sc.hasNextInt()) {
            sc.next()
            println()
            displayMenu()
        }

        when (sc.nextInt()) {

            1 ->
                // Test BCP47 tag.
                validateBCP47()

            0 -> {
                // Exit program.
                sc.close()
                println("Finished")
                exitProcess(0)
            }

            else -> println("Invalid Option")
        }

    }
}

/** Display a list of menu options. */
fun displayMenu() {
    println("1). Enter a BCP47 Language Tag.")
    println("0). Quit program.")
    println()
    println("Please make a choice ?")
}

/**
 * Allow user to enter a BCP47 language tag. Then check BCP47 language tag
 * for errors.
 */
fun validateBCP47() {

    println("BCP47 Tag Validation")
    println("--------------------")
    println("Please enter a BCP47 tag to check")

    val tag = readlnOrNull()

    if (tag != null) {

        val results = parse(tag)

        // Print lists of language sub tags that have been found.
        println("language = " + results.languageTag)
        println("Extended = " + results.extendedTags)
        println("Scripts = " + results.scriptTags)
        println("Regions = " + results.regionTags)
        println("Variants = " + results.variantTags)
        println("Extensions = " + results.extensionTags)
        println("Private Use = " + results.privateUseTags)
        println("Canonicalize = " + results.canonicalize)

        println()

        // Check if there are any errors.
        if (results.isValid) {
            println("No Errors.")
        } else {
            println("Error List.")
            println("-----------")
            results.errorMessages.forEach { println(it) }
        }

        println()
        println()
    }
}