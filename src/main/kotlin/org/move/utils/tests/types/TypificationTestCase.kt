package org.move.utils.tests.types

import org.intellij.lang.annotations.Language
import org.move.ide.presentation.expectedTyText
import org.move.ide.presentation.text
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvType
import org.move.lang.core.psi.ext.inferredExprTy
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferExpectedTy
import org.move.lang.core.types.infer.itemContext
import org.move.lang.core.types.infer.maybeInferenceContext
import org.move.utils.tests.InlineFile
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.base.findElementAndDataInEditor

abstract class TypificationTestCase : MvTestBase() {
    protected fun testExpectedTyExpr(@Language("Move") code: String) {
        testExpectedType<MvExpr>(code)
    }

    protected fun testExpectedTyType(@Language("Move") code: String) {
        testExpectedType<MvType>(code)
    }

    protected inline fun <reified T: MvElement> testExpectedType(@Language("Move") code: String) {
        InlineFile(myFixture, code, "main.move")
        val (element, data) = myFixture.findElementAndDataInEditor<T>()
        val expectedType = data.trim()

        val msl = element.isMsl()
        val ctx = element.maybeInferenceContext(msl) ?: InferenceContext(msl, element.itemContext(msl))

        val actualType = inferExpectedTy(element, ctx)?.expectedTyText() ?: "null"
        check(actualType == expectedType) {
            "Type mismatch. Expected $expectedType, found: $actualType"
        }
    }

    protected fun testExpr(
        @Language("Move") code: String,
//        allowErrors: Boolean = false
    ) {
        InlineFile(myFixture, code, "main.move")
        check()
//        if (!allowErrors) checkNoInferenceErrors()
//        checkAllExpressionsTypified()
    }

//    protected fun testExpr(
//        @Language("Move") code: String,
//        description: String = "",
//        allowErrors: Boolean = false
//    ) = testExpr(code, description, allowErrors)

//    protected fun stubOnlyTypeInfer(
//        @Language("Rust") code: String,
//        description: String = "",
//        allowErrors: Boolean = false
//    ) {
//        val testProject = fileTreeFromText(code)
//            .createAndOpenFileWithCaretMarker()
//
//        checkAstNotLoaded { file ->
//            !file.path.endsWith(testProject.fileWithCaret)
//        }
//
//        check(description)
//        if (!allowErrors) checkNoInferenceErrors()
//        checkAllExpressionsTypified()
//    }

    private fun check() {
        val (expr, data) = myFixture.findElementAndDataInEditor<MvExpr>()
//        val expectedTypes = data.split("|").map(String::trim)
        val expectedType = data.trim()

//        val ctx = InferenceContext(expr.isMsl())
        val type = expr.inferredExprTy().text(true)
        check(type == expectedType) {
            "Type mismatch. Expected $expectedType, found: $type"
        }
//        check(type in expectedType) {
//            "Type mismatch. Expected one of $expectedType, found: $type. $description"
//        }
    }

//    private fun checkNoInferenceErrors() {
//        val errors = myFixture.file.descendantsOfType<RsInferenceContextOwner>().asSequence()
//            .flatMap { it.inference.diagnostics.asSequence() }
//            .map { it.element to it.prepare() }
//            .filter { it.second.severity == Severity.ERROR }
//            .toList()
//        if (errors.isNotEmpty()) {
//            error(
//                errors.joinToString("\n", "Detected errors during type inference: \n") {
//                    "\tAt `${it.first.text}` (line ${it.first.lineNumber}) " +
//                            "${it.second.errorCode?.code} ${it.second.header} | ${it.second.description}"
//                }
//            )
//        }
//    }

//    private fun checkAllExpressionsTypified() {
//        val notTypifiedExprs = myFixture.file.descendantsWithMacrosOfType<RsExpr>()
//            .filter { expr ->
//                expr.inference?.isExprTypeInferred(expr) == false
//            }.filter { expr ->
//                expr.expandedFrom?.let { it.macroName in BUILTIN_MACRO_NAMES } != true
//            }
//        if (notTypifiedExprs.isNotEmpty()) {
//            error(
//                notTypifiedExprs.joinToString(
//                    "\n",
//                    "Some expressions are not typified during type inference: \n",
//                    "\nNote: All `RsExpr`s must be typified during type inference"
//                ) { "\tAt `${it.text}` (line ${it.lineNumber})" }
//            )
//        }
//    }
}
