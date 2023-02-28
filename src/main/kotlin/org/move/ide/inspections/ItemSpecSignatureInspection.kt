package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.move.ide.utils.FunctionSignature
import org.move.ide.utils.signature
import org.move.lang.core.psi.MvItemSpec
import org.move.lang.core.psi.MvItemSpecSignature
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.*

class ItemSpecSignatureInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        return object : MvVisitor() {
            override fun visitItemSpec(itemSpec: MvItemSpec) {
                val funcItem = itemSpec.funcItem ?: return
                val funcSignature = funcItem.signature ?: return
                val itemSpecSignature = itemSpec.itemSpecSignature ?: return

                val specSignature = itemSpecSignature.functionSignature
                if (funcSignature != specSignature) {
                    val itemSpecStart = itemSpec.startOffset
                    val start = itemSpec.itemSpecRef?.startOffset ?: itemSpec.startOffset
                    val end = itemSpecSignature.endOffset
                    holder.registerProblem(
                        itemSpec,
                        "Function signature mismatch",
                        ProblemHighlightType.WARNING,
                        TextRange(start - itemSpecStart, end - itemSpecStart)
                    )
                }
            }
        }
    }
}

private val MvItemSpecSignature.functionSignature: FunctionSignature
    get() {
        val paramList = itemSpecFunctionParameterList

        val specParameters = paramList.itemSpecFunctionParameterList
        val signatureParams = specParameters.map { specParam ->
            val paramName = specParam.referenceName
            val paramType = specParam.typeAnnotation?.type?.text ?: ""
            FunctionSignature.Parameter(paramName, paramType)
        }
        val specTypeParameters =
            itemSpecTypeParameterList?.itemSpecTypeParameterList.orEmpty()
        val signatureTypeParams = specTypeParameters
            .map { specTypeParam ->
                FunctionSignature.TypeParameter(specTypeParam.referenceName)
            }
        return FunctionSignature(signatureTypeParams, signatureParams)
    }
