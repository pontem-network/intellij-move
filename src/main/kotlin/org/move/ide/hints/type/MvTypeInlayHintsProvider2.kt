package org.move.ide.hints.type

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.codeInsight.hints.declarative.HintFontSize.ABitSmallerThanInEditor
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.ide.presentation.hintText
import org.move.lang.core.psi.MvLetStmt
import org.move.lang.core.psi.MvPatBinding
import org.move.lang.core.psi.ext.bindingOwner
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.psi.ext.hasAncestor
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.inferenceOwner
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.*

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

            // only show bindings for let statements
            if (patBinding.bindingOwner !is MvLetStmt) return

            val contextInferenceOwner = patBinding.inferenceOwner() ?: return

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