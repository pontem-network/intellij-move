package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveFullyQualifiedModuleRef
import org.move.lang.core.psi.MoveQualPath
import org.move.lang.core.psi.MoveType
import org.move.lang.core.types.Address


val MoveQualPath.address: Address? get() = (moduleRef as? MoveFullyQualifiedModuleRef)?.addressRef?.address()

val MoveQualPath.moduleName: String? get() = moduleRef?.identifier?.text

val MoveQualPath.identifierName: String get() = identifier.text

val MoveQualPath.isIdentifierOnly: Boolean get() = moduleRef == null

val MoveQualPath.typeArguments: List<MoveType>
    get() =
        typeArgumentList?.typeList.orEmpty()


//abstract class MoveQualPathMixin(node: ASTNode) : MoveReferenceElementImpl(node),
//                                                       MoveQualPath {
//    override fun getReference(): MoveReference =
//        when (parent) {
//            is MoveQualPathType,
//            is MoveStructPat,
//            is MoveStructLiteralExpr,
//            ->
//                MoveQualPathReferenceImpl(this, Namespace.TYPE)
//
//            is MoveCallExpr,
//            is MoveRefExpr,
//            -> MoveQualPathReferenceImpl(this, Namespace.NAME)
//
//            is MoveApplySchemaName -> MoveQualPathReferenceImpl(this, Namespace.SCHEMA)
//
//            else -> error("Parser ensures that types are exhaustive")
//        }
//}
