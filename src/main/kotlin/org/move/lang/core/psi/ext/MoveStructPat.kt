package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveStructPat
import org.move.lang.core.psi.MoveStructPatField

val MoveStructPat.fields: List<MoveStructPatField>
    get() =
        structPatFieldsBlock.structPatFieldList

val MoveStructPat.fieldNames: List<String>
    get() =
        fields.map { it.referenceName }

//abstract class MoveStructPatMixin(node: ASTNode) : MoveElementImpl(node), MoveStructPat {
//    override fun resolvedType(): BaseType? {
//        val parentPattern = ancestorStrict<MovePat>()
//    }
//}
