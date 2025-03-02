package org.move.ide.hints.type

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.codeInsight.hints.declarative.HintFontSize.ABitSmallerThanInEditor
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.bindingTypeOwner
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.inferenceContextOwner
import org.move.lang.core.types.ty.TyUnknown

class MvTypeInlayHintsProvider2: InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector = Collector()

    private class Collector: SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            when (element) {
                is MvPatBinding -> showTypeForPatBinding(element, sink)
            }
        }

        private fun showTypeForPatBinding(patBinding: MvPatBinding, sink: InlayTreeSink) {
            // skip private variables
            if (patBinding.name.startsWith("_")) return

            val parent = patBinding.bindingTypeOwner
            when (parent) {
                // require explicit type annotations
                is MvFunctionParameter, is MvConst, is MvSchemaFieldStmt, is MvTypeQuantBinding -> return
                is MvLambdaParameter -> {
                    // if lambda parameter has explicit type
                    if (parent.type != null) return
                }
                is MvPatFieldFull -> {
                    // skip hints for `field: field_alias`
                    return
                }
                is MvLetStmt -> {
                    // explicit type for let stmt
                    if (parent.type != null) return
                }
                is MvPatField -> {
                    // field shorthand, show type hint
                }
                is MvForIterCondition, is MvRangeQuantBinding -> {
                    // show hints for iteration indexes
                }
                else -> return
            }

            val contextInferenceOwner = patBinding.inferenceContextOwner() ?: return
            val msl = patBinding.isMsl()
            val ty = contextInferenceOwner.inference(msl).getBindingType(patBinding)
            if (ty is TyUnknown) return

            val pos = InlineInlayPosition(patBinding.endOffset, false)
            val format = HintFormat.default.withFontSize(ABitSmallerThanInEditor)
            sink.addPresentation(pos, hintFormat = format) {
                text(": ")
                MvTypeHintsFactory.typeHint(ty, this)
            }
        }
    }

    companion object {
        const val PROVIDER_ID: String = "org.move.hints.types"
    }
}