package org.move.ide.hints

import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvStructLitField
import org.move.lang.core.psi.MvStructLitFieldsBlock
import org.move.lang.core.psi.MvValueArgumentList
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.type
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.TyUnknown
import org.move.utils.AsyncParameterInfoHandlerBase

class StructLitFieldsInfoHandler :
    AsyncParameterInfoHandlerBase<MvStructLitFieldsBlock, FieldsDescription>() {

    override fun findTargetElement(file: PsiFile, offset: Int): MvStructLitFieldsBlock? {
        val element = file.findElementAt(offset) ?: return null
        return element.ancestorOrSelf(stopAt = MvValueArgumentList::class.java)
    }

    override fun calculateParameterInfo(element: MvStructLitFieldsBlock): Array<FieldsDescription>? =
        FieldsDescription.fromStructLitBlock(element)?.let { arrayOf(it) }

    override fun updateParameterInfo(
        block: MvStructLitFieldsBlock,
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
        block: MvStructLitFieldsBlock,
        context: UpdateParameterInfoContext
    ): Int {
        if (block.startOffset == context.offset) return -1
        var elementAtOffset = context.file.findElementAt(context.offset) ?: return -1

        val selectedField = elementAtOffset.ancestorStrict<MvStructLitField>()
        if (selectedField != null) {
            elementAtOffset = selectedField
        }
        val chunks = block
            .childrenWithLeaves
            .splitAround(MvElementTypes.COMMA)
        val chunk = chunks.find { it.contains(elementAtOffset) } ?: return -1
        val struct = block.litExpr.path.maybeStruct ?: return -1

        val fieldName =
            chunk.filterIsInstance<MvStructLitField>().firstOrNull()?.referenceName
        if (fieldName == null) {
            val filledFieldNames = chunks
                .mapNotNull { it.filterIsInstance<MvStructLitField>().firstOrNull()?.referenceName }
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
        fun fromStructLitBlock(block: MvStructLitFieldsBlock): FieldsDescription? {
            val structPath = block.litExpr.path
            val struct = structPath.maybeStruct ?: return null
            val msl = structPath.isMslScope
//            val itemContext = struct.outerItemContext(msl)
            val fieldParams =
                struct.fieldsMap.entries.map { (name, field) ->
                    val type = field.type?.loweredType(msl) ?: TyUnknown
//                    val type = itemContext.getStructFieldItemTy(field).fullname()
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
