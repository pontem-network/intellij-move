package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.resolve.ref.MoveQualifiedPathReferenceImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.types.Address


val MoveQualifiedPath.address: Address? get() = (moduleRef as? MoveFullyQualifiedModuleRef)?.addressRef?.address()

val MoveQualifiedPath.moduleName: String? get() = moduleRef?.identifier?.text

val MoveQualifiedPath.identifierName: String get() = identifier.text

val MoveQualifiedPath.isIdentifierOnly: Boolean get() = moduleRef == null

val MoveQualifiedPath.typeArguments: List<MoveQualifiedPathType>
    get() =
        typeArgumentList?.qualifiedPathTypeList.orEmpty()


abstract class MoveQualifiedPathMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                       MoveQualifiedPath {
    override fun getReference(): MoveReference =
        when (parent) {
            is MoveQualifiedPathType,
            is MoveStructPat,
            is MoveStructLiteralExpr,
            ->
                MoveQualifiedPathReferenceImpl(this, Namespace.TYPE)

            is MoveCallExpr,
            is MoveRefExpr,
            -> MoveQualifiedPathReferenceImpl(this, Namespace.NAME)

            is MoveApplySchemaName -> MoveQualifiedPathReferenceImpl(this, Namespace.SCHEMA)

            else -> error("Parser ensures that types are exhaustive")
        }
}
