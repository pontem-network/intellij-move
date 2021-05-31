package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.INTEGER_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.IntegerType
import org.move.lang.core.types.PrimitiveType
import org.move.lang.core.types.TypeParamType

abstract class MoveQualPathTypeMixin(node: ASTNode) : MoveQualTypeReferenceElementImpl(node),
                                                      MoveQualPathType
{
    override fun resolvedType(): BaseType? {
        val referred = this.reference.resolve()
        if (referred == null) {
            val refName = this.referenceName ?: return null
            return when (refName) {
                in INTEGER_TYPE_IDENTIFIERS -> IntegerType(refName)
                in BUILTIN_TYPE_IDENTIFIERS -> PrimitiveType(refName)
                else -> null
            }
        }

        return when (referred) {
            is MoveTypeParameter -> TypeParamType(referred)
            is MoveStructSignature -> referred.structDef?.structType
            else -> null
        }
    }
}

val MoveQualPathType.referredStructSignature: MoveStructSignature?
    get() = reference?.resolve() as? MoveStructSignature

val MoveQualPathType.referredStructDef: MoveStructDef?
    get() = referredStructSignature?.structDef
