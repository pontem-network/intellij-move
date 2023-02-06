package org.move.lang.core.stubs.impl

import com.intellij.psi.stubs.*
import com.intellij.util.io.DataInputOutputUtil
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.impl.MvModuleImpl
import org.move.lang.core.stubs.MvStubElementType

interface MvNamedStub {
    val name: String?
}

class MvModuleStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?
) : StubBase<MvModule>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvModuleStub, MvModule>("MODULE") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvModuleStub(
                parentStub,
                this,
                dataStream.readNameAsString()
            )

        override fun serialize(stub: MvModuleStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun createPsi(stub: MvModuleStub): MvModule =
            MvModuleImpl(stub, this)

        override fun createStub(psi: MvModule, parentStub: StubElement<*>?): MvModuleStub {
            return MvModuleStub(parentStub, this, psi.name)
        }
    }
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
