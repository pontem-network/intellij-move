package org.move.ide.hints

import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.ParameterInfoUtils
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.psi.PsiFile
import org.move.ide.presentation.fullname
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveStructLiteralExpr
import org.move.lang.core.psi.MoveStructLiteralFieldsBlock
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.fieldsMap
import org.move.lang.core.psi.ext.maybeStruct
import org.move.lang.core.psi.ext.startOffset
import org.move.utils.AsyncParameterInfoHandler

class StructLiteralFieldsDescription(val fields: Array<String>) {
    val presentText = if (fields.isEmpty()) "<no fields>" else fields.joinToString(", ")

    companion object {
        fun fromStructLiteralBlock(block: MoveStructLiteralFieldsBlock): StructLiteralFieldsDescription? {
            val structLiteralExpr = block.parent as? MoveStructLiteralExpr ?: return null
            val struct = structLiteralExpr.path.maybeStruct ?: return null
            val fieldParams =
                struct.fieldsMap.entries.map { (name, field) ->
                    val type = field.resolvedType(emptyMap()).fullname()
                    "$name: $type"
                }.toTypedArray()
            return StructLiteralFieldsDescription(fieldParams)
        }
    }
}

class StructLiteralFieldsInfoHandler :
    AsyncParameterInfoHandler<MoveStructLiteralFieldsBlock, StructLiteralFieldsDescription>() {

    override fun findTargetElement(file: PsiFile, offset: Int): MoveStructLiteralFieldsBlock? =
        file.findElementAt(offset)?.ancestorStrict()

    override fun calculateParameterInfo(element: MoveStructLiteralFieldsBlock): Array<StructLiteralFieldsDescription>? =
        StructLiteralFieldsDescription.fromStructLiteralBlock(element)?.let { arrayOf(it) }

    override fun updateParameterInfo(
        parameterOwner: MoveStructLiteralFieldsBlock,
        context: UpdateParameterInfoContext
    ) {
        if (context.parameterOwner != parameterOwner) {
            context.removeHint()
            return
        }
        val currentParameterIndex = if (parameterOwner.startOffset == context.offset) {
            -1
        } else {
            ParameterInfoUtils.getCurrentParameterIndex(
                parameterOwner.node,
                context.offset,
                MoveElementTypes.COMMA
            )
        }
        context.setCurrentParameter(currentParameterIndex)

    }

    override fun updateUI(description: StructLiteralFieldsDescription, context: ParameterInfoUIContext) {
//        val range = description.getArgumentRange(context.currentParameterIndex)
        context.setupUIComponentPresentation(
            description.presentText,
            -1,
            -1,
            !context.isUIComponentEnabled,
            false,
            false,
            context.defaultParameterColor
        )
    }
}
