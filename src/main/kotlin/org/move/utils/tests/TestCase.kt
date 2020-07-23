/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests

import java.nio.file.Path
import java.nio.file.Paths

fun camelOrWordsToSnake(name: String): String {
    if (' ' in name) return name.trim().replace(" ", "_")

    return name.split("(?=[A-Z])".toRegex()).joinToString("_", transform = String::toLowerCase)
}

interface TestCase {
    val testFileExtension: String
    fun getTestDataPath(): String
    fun getTestName(lowercaseFirstLetter: Boolean): String

    companion object {
        const val testResourcesPath = "src/test/resources"
    }
}

fun TestCase.pathToSourceTestFile(): Path =
    Paths.get("${TestCase.testResourcesPath}/${getTestDataPath()}/${getTestName(true)}.$testFileExtension")

fun TestCase.pathToGoldTestFile(): Path =
    Paths.get("${TestCase.testResourcesPath}/${getTestDataPath()}/${getTestName(true)}.txt")
