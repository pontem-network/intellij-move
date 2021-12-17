package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvPathReference
import org.move.lang.core.resolve.ref.MvPathReferenceImpl
import org.move.lang.core.resolve.ref.Namespace

//val MvQualPath.address: Address? get() = (moduleRef as? MvFQModuleRef)?.addressRef?.address()
//
//val MvQualPath.moduleName: String? get() = moduleRef?.identifier?.text
//
fun MvPath.isPrimitiveType(): Boolean =
    this.parent is MvPathType
            && this.referenceName in PRIMITIVE_TYPE_IDENTIFIERS.union(BUILTIN_TYPE_IDENTIFIERS)

val MvPath.identifierName: String? get() = identifier?.text

val MvPathIdent.isIdentifierOnly: Boolean
    get() =
        identifier != null && this.moduleRef == null

val MvPath.typeArguments: List<MvTypeArgument>
    get() = typeArgumentList?.typeArgumentList.orEmpty()

val MvPath.maybeStruct: MvStruct_?
    get() {
        return reference?.resolve() as? MvStruct_
    }


abstract class MvPathMixin(node: ASTNode) : MvElementImpl(node), MvPath {

    override val identifier: PsiElement? get() = this.pathIdent.identifier

    override fun getReference(): MvPathReference? {
        val namespace = when {
            this.isInsideSpecBlock() -> Namespace.SPEC
            this.parent is MvPathType -> Namespace.TYPE
            else -> Namespace.NAME
        }
        return MvPathReferenceImpl(this, namespace)
    }
}
