package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveQualifiedPath
import org.move.lang.core.psi.MoveQualifiedPathType
import org.move.lang.core.psi.MoveTypeParametersOwner
import org.move.lang.core.psi.typeParameters


val MoveQualifiedPath.address: String? get() = addressRef?.addressLiteral?.text

val MoveQualifiedPath.moduleName: String? get() = moduleRef?.identifier?.text

val MoveQualifiedPath.identifierName: String get() = identifier.text

val MoveQualifiedPath.isIdentifierOnly: Boolean get() = addressRef == null && moduleRef == null

val MoveQualifiedPath.typeArguments: List<MoveQualifiedPathType>
    get() =
        typeArgumentList?.qualifiedPathTypeList.orEmpty()