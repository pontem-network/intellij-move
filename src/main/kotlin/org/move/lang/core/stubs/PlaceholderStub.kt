package org.move.lang.core.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.*
import org.move.lang.core.psi.MoveElement

open class PlaceholderStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
) : StubBase<MoveElement>(parent, elementType) {

    open class Type<PsiT : MoveElement>(
        debugName: String,
        private val psiContructor: (PlaceholderStub, IStubElementType<*, *>) -> PsiT,
    ) : MoveStubElementType<PlaceholderStub, PsiT>(debugName) {

        override fun shouldCreateStub(node: ASTNode): Boolean = createStubIfParentIsStub(node)

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            PlaceholderStub(parentStub, this)

        override fun serialize(stub: PlaceholderStub, dataStream: StubOutputStream) {}

        override fun createPsi(stub: PlaceholderStub) = psiContructor(stub, this)

        override fun createStub(psi: PsiT, parentStub: StubElement<*>?) = PlaceholderStub(parentStub, this)
    }
}