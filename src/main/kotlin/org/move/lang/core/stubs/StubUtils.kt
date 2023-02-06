package org.move.lang.core.stubs

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.DataInputOutputUtil
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNameIdentifierOwner
import org.move.lang.core.psi.MvPsiFactory

abstract class MvStubbedElementImpl<StubT : StubElement<*>> : StubBasedPsiElementBase<StubT>, MvElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun toString(): String = "${javaClass.simpleName}($elementType)"
}

abstract class MvStubbedNamedElementImpl<StubT> : MvStubbedElementImpl<StubT>,
                                                  MvNameIdentifierOwner
        where StubT : MvNamedStub, StubT : StubElement<*> {

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getNameIdentifier(): PsiElement? = findChildByType(MvElementTypes.IDENTIFIER)

    override fun getName(): String? {
        val stub = greenStub
        return if (stub !== null) stub.name else nameIdentifier?.text
    }

    override fun setName(name: String): PsiElement? {
        nameIdentifier?.replace(MvPsiFactory(project).identifier(name))
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}

private fun StubInputStream.readNameAsString(): String? = readName()?.string
private fun StubInputStream.readUTFFastAsNullable(): String? =
    DataInputOutputUtil.readNullable(this, this::readUTFFast)

private fun StubOutputStream.writeUTFFastAsNullable(value: String?) =
    DataInputOutputUtil.writeNullable(this, value, this::writeUTFFast)

private fun StubOutputStream.writeLongAsNullable(value: Long?) =
    DataInputOutputUtil.writeNullable(this, value, this::writeLong)

private fun StubInputStream.readLongAsNullable(): Long? = DataInputOutputUtil.readNullable(this, this::readLong)

private fun StubOutputStream.writeDoubleAsNullable(value: Double?) =
    DataInputOutputUtil.writeNullable(this, value, this::writeDouble)

private fun StubInputStream.readDoubleAsNullable(): Double? =
    DataInputOutputUtil.readNullable(this, this::readDouble)
