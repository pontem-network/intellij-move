package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType

val MoveStructPat.providedFields: List<MoveStructPatField>
    get() =
        structPatFieldsBlock.structPatFieldList

val MoveStructPat.providedFieldNames: List<String>
    get() =
        providedFields.mapNotNull { it.referenceName }

//abstract class MoveStructPatMixin(node: ASTNode) : MoveElementImpl(node), MoveStructPat {
//    override fun resolvedType(): BaseType? {
//        val parentPattern = ancestorStrict<MovePat>()
//    }
//}
