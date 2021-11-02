package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.INTEGER_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.types.*

abstract class MovePathTypeMixin(node: ASTNode) : MoveElementImpl(node), MovePathType {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        val referred = this.path.reference?.resolve()
        if (referred == null) {
            val refName = this.path.referenceName ?: return null
            return when (refName) {
                in INTEGER_TYPE_IDENTIFIERS -> IntegerType(refName)
                "bool", "address" -> PrimitiveType(refName)
                "signer" -> SignerType()
                "vector" -> {
                    val vectorItem = this.path.typeArguments.firstOrNull() ?: return null
                    val itemType = vectorItem.type.resolvedType(emptyMap()) ?: return null
                    return VectorType(itemType)
                }
                else -> null
            }
        }

        return when (referred) {
            is MoveTypeParameter -> TypeParamType.withSubstitutedTypeVars(referred, typeVars)
            is MoveStructSignature -> {
                val typeArguments =
                    this.path.typeArguments.map { it.type.resolvedType(typeVars) }
                StructType(referred, typeArguments)
            }
            else -> null
        }
    }
}
