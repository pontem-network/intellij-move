package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.PrimitiveType

abstract class MoveQualPathTypeMixin(node: ASTNode) : MoveQualTypeReferenceElementImpl(node),
                                                      MoveQualPathType
{
    override fun resolvedType(): BaseType? {
        val referred = this.reference.resolve()
        if (referred == null) {
            val refName = this.referenceName ?: return null
            return when (refName) {
                in PRIMITIVE_TYPE_IDENTIFIERS,
                in BUILTIN_TYPE_IDENTIFIERS -> PrimitiveType(refName)
                else -> null
            }
        }

        return when (referred) {
            is MoveTypeParameter -> referred.typeParamType
            is MoveStructSignature -> referred.structDef?.structType
            else -> null
        }
    }
}

val MoveQualPathType.referredStructSignature: MoveStructSignature?
    get() = reference?.resolve() as? MoveStructSignature

val MoveQualPathType.referredStructDef: MoveStructDef?
    get() = referredStructSignature?.structDef
