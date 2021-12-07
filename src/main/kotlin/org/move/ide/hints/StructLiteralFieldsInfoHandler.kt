package org.move.ide.hints

import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.move.ide.presentation.fullname
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveCallArguments
import org.move.lang.core.psi.MoveStructLitField
import org.move.lang.core.psi.MoveStructLitFieldsBlock
import org.move.lang.core.psi.ext.*
import org.move.utils.AsyncParameterInfoHandler

class StructLitFieldsInfoHandler :
    AsyncParameterInfoHandler<MoveStructLitFieldsBlock, FieldsDescription>() {

    override fun findTargetElement(file: PsiFile, offset: Int): MoveStructLitFieldsBlock? {
        val element = file.findElementAt(offset) ?: return null
        val block = element.ancestorStrict<MoveStructLitFieldsBlock>() ?: return null
        val callExpr = element.ancestorStrict<MoveCallArguments>()
        if (callExpr != null && block.contains(callExpr)) return null
        return block
    }

    override fun calculateParameterInfo(element: MoveStructLitFieldsBlock): Array<FieldsDescription>? =
        FieldsDescription.fromStructLitBlock(element)?.let { arrayOf(it) }

    override fun updateParameterInfo(
        block: MoveStructLitFieldsBlock,
        context: UpdateParameterInfoContext
    ) {
        if (context.parameterOwner != block) {
            context.removeHint()
            return
        }
        val currentParameterIndex = findParameterIndex(block, context)
        context.setCurrentParameter(currentParameterIndex)
    }

    override fun updateUI(description: FieldsDescription, context: ParameterInfoUIContext) {
        val range = description.getArgumentRange(context.currentParameterIndex)
        context.setupUIComponentPresentation(
            description.presentText,
            range.startOffset,
            range.endOffset,
            !context.isUIComponentEnabled,
            false,
            false,
            context.defaultParameterColor
        )
    }

    private fun findParameterIndex(
        block: MoveStructLitFieldsBlock,
        context: UpdateParameterInfoContext
    ): Int {
        if (block.startOffset == context.offset) return -1
        var elementAtOffset = context.file.findElementAt(context.offset) ?: return -1

        val selectedField = elementAtOffset.ancestorStrict<MoveStructLitField>()
        if (selectedField != null) {
            elementAtOffset = selectedField
        }
        val chunks = block
            .childrenWithLeaves
            .splitAround(MoveElementTypes.COMMA)
        val chunk = chunks.find { it.contains(elementAtOffset) } ?: return -1
        val struct = block.litExpr.path.maybeStruct ?: return -1

        val fieldName =
            chunk.filterIsInstance<MoveStructLitField>().firstOrNull()?.referenceName
        if (fieldName == null) {
            val filledFieldNames = chunks
                .mapNotNull { it.filterIsInstance<MoveStructLitField>().firstOrNull()?.referenceName }
                .toSet()
            if (filledFieldNames.isEmpty()) return 0
            return struct
                .fieldNames.withIndex()
                .asSequence()
                .filter { it.value !in filledFieldNames }.firstOrNull()?.index ?: -1
        }
        return struct.fieldNames.indexOf(fieldName)
    }
}

class FieldsDescription(val fields: Array<String>) {
    val presentText = if (fields.isEmpty()) "<no fields>" else fields.joinToString(", ")

    fun getArgumentRange(index: Int): TextRange {
        if (index < 0 || index >= fields.size) return TextRange.EMPTY_RANGE
        val start = fields.take(index).sumOf { it.length + 2 }
        return TextRange(start, start + fields[index].length)
    }

    companion object {
        fun fromStructLitBlock(block: MoveStructLitFieldsBlock): FieldsDescription? {
            val struct = block.litExpr.path.maybeStruct ?: return null
            val fieldParams =
                struct.fieldsMap.entries.map { (name, field) ->
                    val type = field.declaredTy.fullname()
                    "$name: $type"
                }.toTypedArray()
            return FieldsDescription(fieldParams)
        }
    }
}

private fun Sequence<PsiElement>.splitAround(elementType: IElementType): List<List<PsiElement>> {
    val chunks = mutableListOf<List<PsiElement>>()
    val chunk = mutableListOf<PsiElement>()
    this.forEach {
        if (it.elementType != elementType)
            chunk.add(it)
        else {
            chunk.add(it)
            chunks.add(chunk.toList())
            chunk.clear()
        }
    }
    if (chunk.isNotEmpty()) chunks.add(chunk)
    return chunks
}
