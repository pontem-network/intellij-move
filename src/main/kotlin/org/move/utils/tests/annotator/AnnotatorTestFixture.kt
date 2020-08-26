/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests.annotator

import com.intellij.codeInsight.daemon.impl.SeveritiesProvider
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.BaseFixture
import org.move.ide.annotator.AnnotatorBase
import kotlin.reflect.KClass

class AnnotatorTestFixture(
    private val codeInsightFixture: CodeInsightTestFixture,
    private val annotatorClasses: List<KClass<out AnnotatorBase>> = emptyList()
) : BaseFixture() {

    override fun setUp() {
        super.setUp()
        annotatorClasses.forEach {
            AnnotatorBase.enableAnnotator(
                it.java,
                testRootDisposable
            )
        }
    }

    private fun replaceCaretMarker(text: String) = text.replace("/*caret*/", "<caret>")

    private fun configureByText(text: String) {
        codeInsightFixture.configureByText("main.move", replaceCaretMarker(text.trimIndent()))
    }

    fun check(
        text: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        ignoreExtraHighlighting: Boolean = false
    ) {
        configureByText(text)
        codeInsightFixture.checkHighlighting(checkWarn, checkInfo, checkWeakWarn, ignoreExtraHighlighting)
    }

    fun registerSeverities(severities: List<HighlightSeverity>) {
        val testSeverityProvider = TestSeverityProvider(severities)
        // BACKCOMPAT: 2020.1
        @Suppress("DEPRECATION")
        SeveritiesProvider.EP_NAME.getPoint(null).registerExtension(testSeverityProvider, testRootDisposable)
    }
}
