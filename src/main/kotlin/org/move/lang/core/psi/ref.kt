package org.move.lang.core.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.lang.core.psi.ext.structDef
import org.move.lang.core.resolve.ref.MoveQualPathReferenceImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.Namespace

interface MoveReferenceElement : MoveElement {
    val identifier: PsiElement?

    override fun getReference(): MoveReference?

    @JvmDefault
    val referenceNameElement: PsiElement?
        get() = identifier

    @JvmDefault
    val referenceName: String?
        get() = identifier?.text

    @JvmDefault
    val isUnresolved: Boolean
        get() = reference?.resolve() == null
}

interface MoveSchemaReferenceElement : MoveReferenceElement

//abstract class MoveReferenceElementImpl(node: ASTNode) : MoveElementImpl(node),
//                                                         MoveReferenceElement {
//    override val referenceNameElement: PsiElement
//        get() =
//        get() = requireNotNull(findFirstChildByType(MoveElementTypes.IDENTIFIER)) {
//            "Reference elements should all have IDENTIFIER as a direct child: $node doesn't for some reason"
//        }

//    abstract override fun getReference(): MoveReference
//}

interface MoveQualPathReferenceElement : MoveReferenceElement {
    val isPrimitive: Boolean

    val qualPath: MoveQualPath

    @JvmDefault
    override val isUnresolved: Boolean
        get() = !isPrimitive && reference?.resolve() == null
}

//abstract class MoveQualPathReferenceElementImpl : MoveQualPathReferenceElement {
//    override fun getReference(): MoveReference = qualPath.reference
//}

interface MoveQualTypeReferenceElement : MoveQualPathReferenceElement {
    override val isPrimitive: Boolean
        get() = referenceName in PRIMITIVE_TYPE_IDENTIFIERS
                || referenceName in BUILTIN_TYPE_IDENTIFIERS

    @JvmDefault
    val referredStructSignature: MoveStructSignature?
        get() = reference?.resolve() as? MoveStructSignature

    @JvmDefault
    val referredStructDef: MoveStructDef?
        get() = referredStructSignature?.structDef
}

abstract class MoveQualTypeReferenceElementImpl(node: ASTNode) : MoveElementImpl(node),
                                                                 MoveQualTypeReferenceElement {
    override val identifier: PsiElement get() = qualPath.identifier

    override fun getReference(): MoveReference = MoveQualPathReferenceImpl(this, Namespace.TYPE)
}

interface MoveQualNameReferenceElement : MoveQualPathReferenceElement {
    override val isPrimitive: Boolean
        get() = referenceName in BUILTIN_FUNCTIONS
}

abstract class MoveQualNameReferenceElementImpl(node: ASTNode) : MoveElementImpl(node),
                                                                 MoveQualNameReferenceElement {

    override val identifier: PsiElement get() = qualPath.identifier

    override fun getReference(): MoveReference = MoveQualPathReferenceImpl(this, Namespace.NAME)
}


interface MoveQualSchemaReferenceElement : MoveQualPathReferenceElement {
    @JvmDefault
    override val isUnresolved: Boolean
        get() =
            reference?.resolve() == null
}

abstract class MoveQualSchemaReferenceElementImpl(node: ASTNode) : MoveElementImpl(node),
                                                                   MoveQualSchemaReferenceElement {

    override val identifier: PsiElement get() = qualPath.identifier

    override fun getReference(): MoveReference = MoveQualPathReferenceImpl(this, Namespace.SCHEMA)
}

interface MoveStructFieldReferenceElement : MoveReferenceElement
