package org.move.utils.tests.types

import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import org.intellij.lang.annotations.Language
import org.move.ide.presentation.expectedTyText
import org.move.ide.presentation.text
import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvType
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.MvInferenceContextOwner
import org.move.lang.core.types.infer.inferExpectedTy
import org.move.lang.core.types.infer.inference
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

    protected inline fun <reified T : MvElement> testExpectedType(@Language("Move") code: String) {
        InlineFile(myFixture, code, "main.move")
        val (element, data) = myFixture.findElementAndDataInEditor<T>()
        val expectedType = data.trim()

//        val ctx = element.maybeInferenceContext(msl) ?: InferenceContext(msl, element.itemContext(msl))

        val msl = element.isMsl()
        val inference = element.inference(msl) ?: error("No inference at caret element")

        val actualType = inferExpectedTy(element, inference)?.expectedTyText() ?: "null"
        check(actualType == expectedType) {
            "Type mismatch. Expected $expectedType, found: $actualType"
        }
    }

    protected fun testExpr(@Language("Move") code: String) {
        InlineFile(myFixture, code, "main.move")
        check()
//        if (!allowErrors) checkNoInferenceErrors()
        checkAllExpressionsTypified()
    }

    protected fun testExprsTypified(@Language("Move") code: String) {
        InlineFile(myFixture, code, "main.move")
        checkAllExpressionsTypified()
    }

    protected fun testBinding(
        @Language("Move") code: String,
//        allowErrors: Boolean = false
    ) {
        InlineFile(myFixture, code, "main.move")
        val (bindingPat, data) = myFixture.findElementAndDataInEditor<MvBindingPat>()
        val expectedType = data.trim()

        val msl = bindingPat.isMsl()
        val inference = bindingPat.inference(msl) ?: error("No InferenceContextOwner at the caret")

        val actualType = inference.getPatType(bindingPat).text(true)
        check(actualType == expectedType) {
            "Type mismatch. Expected $expectedType, found: $actualType"
        }
//        if (!allowErrors) checkNoInferenceErrors()
        checkAllExpressionsTypified()
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

        val msl = expr.isMsl()
        val inference = expr.inference(msl) ?: error("No inference owner at the caret position")
        val actualType = inference.getExprType(expr).text(true)
        check(actualType == expectedType) {
            "Type mismatch. Expected $expectedType, found: $actualType"
        }
    }

    private fun checkNoInferenceErrors() {
        val errors = myFixture.file.descendantsOfType<MvInferenceContextOwner>().asSequence()
            .flatMap { it.inference(false).typeErrors.asSequence() }
            .map { it.element to it.message() }
            .toList()
        if (errors.isNotEmpty()) {
            error(
                errors.joinToString(
                    "\n",
                    "Detected errors during type inference: \n"
                ) {
                    "\tAt `${it.first.text}` (line ${it.first.lineNumber}) ${it.second}"
                }
            )
        }
    }

    private fun checkAllExpressionsTypified() {
        val notTypifiedExprs = myFixture.file
            .descendantsOfType<MvExpr>().toList()
            .filter { expr ->
                expr.inference(false)?.hasExprType(expr) == false
            }
        if (notTypifiedExprs.isNotEmpty()) {
            error(
                notTypifiedExprs.joinToString(
                    "\n",
                    "Some expressions are not typified during type inference: \n",
                    "\nNote: All `MvExpr`s must be typified during type inference"
                ) { "\tAt `${it.text}` (line ${it.lineNumber})" }
            )
        }
    }

    private val PsiElement.lineNumber: Int
        get() = myFixture.getDocument(myFixture.file).getLineNumber(textOffset)
}
