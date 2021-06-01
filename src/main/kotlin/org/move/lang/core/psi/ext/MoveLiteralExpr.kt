package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveLiteralExpr
import org.move.lang.core.types.*

abstract class MoveLiteralExprMixin(node: ASTNode) : MoveElementImpl(node), MoveLiteralExpr {
    override fun resolvedType(): BaseType? {
        return when {
            boolLiteral != null -> PrimitiveType("bool")
            addressLiteral != null
                    || bech32AddressLiteral != null
                    || polkadotAddressLiteral != null -> PrimitiveType("address")
            integerLiteral != null -> {
                val literal = integerLiteral!!
                when {
                    literal.text.endsWith("u8") -> IntegerType("u8")
                    literal.text.endsWith("u64") -> IntegerType("u64")
                    literal.text.endsWith("u128") -> IntegerType("u128")
                    else -> IntegerType()
                }
            }
            byteStringLiteral != null -> VectorType(IntegerType("u8"))
            else -> null
        }
    }
}
