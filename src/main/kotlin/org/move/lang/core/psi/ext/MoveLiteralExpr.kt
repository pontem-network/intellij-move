package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.HEX_INTEGER_LITERAL
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveLiteralExpr
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferLiteralExprTy
import org.move.lang.core.types.ty.Ty

val MoveLiteralExpr.hexIntegerLiteral: PsiElement? get() =
    this.findFirstChildByType(HEX_INTEGER_LITERAL)

abstract class MoveLiteralExprMixin(node: ASTNode) : MoveElementImpl(node), MoveLiteralExpr {
    override fun resolvedType(): Ty {
        return inferLiteralExprTy(this)
//        return when {
//            boolLiteral != null -> PrimitiveType("bool")
//            addressLiteral != null
//                    || bech32AddressLiteral != null
//                    || polkadotAddressLiteral != null -> PrimitiveType("address")
//            integerLiteral != null || hexIntegerLiteral != null -> {
//                val literal = (integerLiteral ?: hexIntegerLiteral)!!
//                when {
//                    literal.text.endsWith("u8") -> IntegerType("u8")
//                    literal.text.endsWith("u64") -> IntegerType("u64")
//                    literal.text.endsWith("u128") -> IntegerType("u128")
//                    else -> IntegerType()
//                }
//            }
//            byteStringLiteral != null -> VectorType(IntegerType("u8"))
//            else -> null
//        }
    }
}
