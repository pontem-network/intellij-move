package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MovePathType
import org.move.lang.core.types.TypeVarsMap
import org.move.lang.core.types.infer.inferMoveTypeTy
import org.move.lang.core.types.ty.Ty

abstract class MovePathTypeMixin(node: ASTNode) : MoveElementImpl(node), MovePathType {
    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        return inferMoveTypeTy(this)
//        val item = this.path.reference?.resolve()
//        if (item == null) {
//            val refName = this.path.referenceName ?: return null
//            return when (refName) {
//                in INTEGER_TYPE_IDENTIFIERS -> TyInteger(refName)
//                "bool", "address" -> PrimitiveType(refName)
//                "signer" -> SignerType()
//                "vector" -> {
//                    val vectorItem = this.path.typeArguments.firstOrNull() ?: return null
//                    val itemType = vectorItem.type.resolvedType(emptyMap()) ?: return null
//                    return VectorType(itemType)
//                }
//                else -> null
//            }
//        }
////
//        return instantiateItemTy(item)
//        return when (item) {
//            is MoveTypeParameter -> TypeParamType.withSubstitutedTypeVars(item, typeVars)
//            is MoveStructSignature -> {
//                val typeArguments =
//                    this.path.typeArguments.map { it.type.resolvedType(typeVars) }
//                StructType(item, typeArguments)
//            }
//            else -> null
//        }
    }
}
