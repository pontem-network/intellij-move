package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MovePathReferenceImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.Namespace

//val MoveQualPath.address: Address? get() = (moduleRef as? MoveFQModuleRef)?.addressRef?.address()
//
//val MoveQualPath.moduleName: String? get() = moduleRef?.identifier?.text
//
fun MovePath.isPrimitiveType(): Boolean =
    this.parent is MovePathType
            && this.referenceName in PRIMITIVE_TYPE_IDENTIFIERS.union(BUILTIN_TYPE_IDENTIFIERS)

val MovePath.identifierName: String? get() = identifier?.text

val MovePathIdent.isIdentifierOnly: Boolean
    get() =
        identifier != null && this.moduleRef == null

val MovePath.typeArguments: List<MoveTypeArgument>
    get() = typeArgumentList?.typeArgumentList.orEmpty()

val MovePath.maybeStructSignature: MoveStructSignature?
    get() {
        return reference?.resolve() as? MoveStructSignature
    }

val MovePath.maybeStruct: MoveStructDef?
    get() {
        return maybeStructSignature?.structDef
    }


abstract class MovePathMixin(node: ASTNode) : MoveElementImpl(node), MovePath {

    override val identifier: PsiElement? get() = this.pathIdent.identifier

    override fun getReference(): MoveReference? {
        if (this.parent is MovePathType) return MovePathReferenceImpl(this, Namespace.TYPE)
        return MovePathReferenceImpl(this, Namespace.NAME)
    }
}
