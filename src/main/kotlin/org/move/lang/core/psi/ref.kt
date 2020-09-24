package org.move.lang.core.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.resolve.ref.MoveQualPathReferenceImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.Namespace

interface MoveReferenceElement : MoveElement {
    val identifier: PsiElement

    override fun getReference(): MoveReference

    @JvmDefault
    val referenceNameElement: PsiElement
        get() = identifier

    @JvmDefault
    val referenceName: String
        get() = identifier.text
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
    val qualPath: MoveQualPath
}

//abstract class MoveQualPathReferenceElementImpl : MoveQualPathReferenceElement {
//    override fun getReference(): MoveReference = qualPath.reference
//}

interface MoveQualTypeReferenceElement : MoveQualPathReferenceElement

abstract class MoveQualTypeReferenceElementImpl(node: ASTNode) : MoveElementImpl(node),
                                                                 MoveQualTypeReferenceElement {
    override val identifier: PsiElement get() = qualPath.identifier

    override fun getReference(): MoveReference = MoveQualPathReferenceImpl(this, Namespace.TYPE)
}

interface MoveQualNameReferenceElement : MoveQualPathReferenceElement

abstract class MoveQualNameReferenceElementImpl(node: ASTNode) : MoveElementImpl(node),
                                                                 MoveQualNameReferenceElement {

    override val identifier: PsiElement get() = qualPath.identifier

    override fun getReference(): MoveReference = MoveQualPathReferenceImpl(this, Namespace.NAME)
}


interface MoveQualSchemaReferenceElement : MoveQualPathReferenceElement

abstract class MoveQualSchemaReferenceElementImpl(node: ASTNode) : MoveElementImpl(node),
                                                                   MoveQualSchemaReferenceElement {

    override val identifier: PsiElement get() = qualPath.identifier

    override fun getReference(): MoveReference = MoveQualPathReferenceImpl(this, Namespace.SCHEMA)
}


