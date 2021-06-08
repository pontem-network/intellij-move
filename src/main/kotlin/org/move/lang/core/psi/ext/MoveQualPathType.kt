package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.annotator.INTEGER_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.types.*

abstract class MoveQualPathTypeMixin(node: ASTNode) : MoveQualTypeReferenceElementImpl(node),
                                                      MoveQualPathType {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        val referred = this.reference.resolve()
        if (referred == null) {
            val refName = this.referenceName ?: return null
            return when (refName) {
                in INTEGER_TYPE_IDENTIFIERS -> IntegerType(refName)
                "bool", "address" -> PrimitiveType(refName)
                "signer" -> SignerType()
                "vector" -> {
                    val vectorItem = this.qualPath.typeArguments.firstOrNull() ?: return null
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
                    this.qualPath.typeArguments
                        .map { it.type.resolvedType(typeVars) }
                StructType(referred, typeArguments)
            }
            else -> null
        }
    }
}

val MoveQualPathType.referredStructSignature: MoveStructSignature?
    get() = reference?.resolve() as? MoveStructSignature

val MoveQualPathType.referredStructDef: MoveStructDef?
    get() = referredStructSignature?.structDef
