package org.move.utils.tests.base

import org.move.utils.tests.base.TestCase.Companion
import java.nio.file.Path
import java.nio.file.Paths

interface TestCase {
    val testFileExtension: String
    fun getTestDataPath(): String
    fun getTestName(lowercaseFirstLetter: Boolean): String

    companion object {
        const val testResourcesPath = "src/test/resources"

        @JvmStatic
        fun camelOrWordsToSnake(name: String): String {
            if (' ' in name)
                return name.trim().replace(" ", "_")
            return name.split("(?=[A-Z])".toRegex()).joinToString("_", transform = String::lowercase)
        }
    }
}

fun TestCase.pathToSourceTestFile(): Path =
    Paths.get("${TestCase.testResourcesPath}/${getTestDataPath()}/${getTestName(true)}.$testFileExtension")

fun TestCase.pathToGoldTestFile(): Path =
    Paths.get("${TestCase.testResourcesPath}/${getTestDataPath()}/${getTestName(true)}.txt")
