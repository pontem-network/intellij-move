package org.move.ide.hints

import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.psi.PsiFile
import org.move.lang.core.psi.MoveStructLiteralFieldsBlock
import org.move.utils.AsyncParameterInfoHandler

class StructLiteralFieldsDescription()

class StructLiteralFieldsInfoHandler :
    AsyncParameterInfoHandler<MoveStructLiteralFieldsBlock, StructLiteralFieldsDescription>() {

    override fun findTargetElement(file: PsiFile, offset: Int): MoveStructLiteralFieldsBlock? {
        TODO("Not yet implemented")
    }

    override fun calculateParameterInfo(element: MoveStructLiteralFieldsBlock): Array<StructLiteralFieldsDescription>? {
        TODO("Not yet implemented")
    }

    override fun updateParameterInfo(
        parameterOwner: MoveStructLiteralFieldsBlock,
        context: UpdateParameterInfoContext
    ) {
        TODO("Not yet implemented")
    }

    override fun updateUI(p: StructLiteralFieldsDescription?, context: ParameterInfoUIContext) {
        TODO("Not yet implemented")
    }
}
