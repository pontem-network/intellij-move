/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.testFramework.enableInspectionTool
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language
import org.move.cli.settings.Blockchain
import org.move.cli.settings.moveSettings
import org.move.utils.tests.base.MvTestCase
import org.move.utils.tests.base.TestCase
import java.lang.annotation.Inherited
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@Inherited
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DebugMode(val enabled: Boolean)

@Inherited
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithEnabledInspections(vararg val inspections: KClass<out InspectionProfileEntry>)

@Inherited
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithBlockchain(val blockchain: Blockchain)

abstract class MvTestBase: BasePlatformTestCase(),
                           MvTestCase {
    protected val fileName: String
        get() = "${getTestName(true)}.$testFileExtension"
    open val dataPath: String = ""

    override fun setUp() {
        super.setUp()

        setupInspections()

        val debugMode = this.findAnnotationInstance<DebugMode>()?.enabled ?: true
        val blockchain = this.findAnnotationInstance<WithBlockchain>()?.blockchain ?: Blockchain.APTOS
        // triggers projects refresh
        project.moveSettings.modify {
            it.debugMode = debugMode
            it.blockchain = blockchain
        }
    }

    private fun setupInspections() {
        for (inspection in findAnnotationInstance<WithEnabledInspections>()?.inspections.orEmpty()) {
            enableInspectionTool(project, inspection.createInstance(), testRootDisposable)
        }
    }

    override fun getTestDataPath(): String = "${TestCase.testResourcesPath}/$dataPath"
    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }

    protected fun inlineFile(@Language("Move") code: String, name: String = "main.move"): InlineFile {
        return InlineFile(myFixture, code, name)
    }

    protected fun checkByText(
        @Language("Move") before: String,
        @Language("Move") after: String,
        action: () -> Unit,
    ) {
        inlineFile(before)
        action()
        myFixture.checkResult(replaceCaretMarker(after))
    }

    protected fun checkByFile(ignoreTrailingWhitespace: Boolean = true, action: () -> Unit) {
        val (before, after) = (fileName to fileName.replace(".move", "_after.move"))
        myFixture.configureByFile(before)
        action()
        myFixture.checkResultByFile(after, ignoreTrailingWhitespace)
    }

//    protected fun FileTree.prepareTestProjectFromFixture(): TestProject =
//        toTestProject(myFixture.project, myFixture.findFileInTempDir("."))

    protected open fun checkEditorAction(
        @Language("Move") before: String,
        @Language("Move") after: String,
        actionId: String,
        trimIndent: Boolean = true,
    ) {
        fun String.trimIndentIfNeeded(): String = if (trimIndent) trimIndent() else this

        checkByText(before.trimIndentIfNeeded(), after.trimIndentIfNeeded()) {
            myFixture.performEditorAction(actionId)
        }
    }

    companion object {
        @JvmStatic
        fun checkHtmlStyle(html: String) {
            // http://stackoverflow.com/a/1732454
            val re = "<body>(.*)</body>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val body = (re.find(html)?.let { it.groups[1]!!.value } ?: html).trim()
            check(body[0].isUpperCase()) {
                "Please start description with the capital latter"
            }

            check(body.last() == '.') {
                "Please end description with a period"
            }
        }

        @JvmStatic
        fun getResourceAsString(path: String): String? {
            val stream = MvTestBase::class.java.classLoader.getResourceAsStream(path)
                ?: return null

            return stream.bufferedReader().use { it.readText() }
        }
    }
}
