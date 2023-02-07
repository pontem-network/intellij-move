package org.move.lang.core.stubs

import com.intellij.psi.stubs.*
import com.intellij.util.io.DataInputOutputUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.*
import org.move.lang.core.resolve.ref.Visibility

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

        override fun indexStub(stub: MvModuleStub, sink: IndexSink) = sink.indexModuleStub(stub)
    }
}

class MvFunctionStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?,
) : StubBase<MvFunction>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvFunctionStub, MvFunction>("FUNCTION") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvFunctionStub(
                parentStub,
                this,
                dataStream.readNameAsString(),
            )

        override fun serialize(stub: MvFunctionStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun createPsi(stub: MvFunctionStub): MvFunction =
            MvFunctionImpl(stub, this)

        override fun createStub(psi: MvFunction, parentStub: StubElement<*>?): MvFunctionStub {
            return MvFunctionStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: MvFunctionStub, sink: IndexSink) = sink.indexFunctionStub(stub)
    }
}

class MvSpecFunctionStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?,
) : StubBase<MvSpecFunction>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvSpecFunctionStub, MvSpecFunction>("SPEC_FUNCTION") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvSpecFunctionStub(
                parentStub,
                this,
                dataStream.readNameAsString(),
            )

        override fun serialize(stub: MvSpecFunctionStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun createPsi(stub: MvSpecFunctionStub): MvSpecFunction =
            MvSpecFunctionImpl(stub, this)

        override fun createStub(psi: MvSpecFunction, parentStub: StubElement<*>?): MvSpecFunctionStub {
            return MvSpecFunctionStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: MvSpecFunctionStub, sink: IndexSink) = sink.indexSpecFunctionStub(stub)
    }
}

class MvStructStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?
) : StubBase<MvStruct>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvStructStub, MvStruct>("STRUCT") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvStructStub(
                parentStub,
                this,
                dataStream.readNameAsString()
            )

        override fun serialize(stub: MvStructStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun createPsi(stub: MvStructStub): MvStruct =
            MvStructImpl(stub, this)

        override fun createStub(psi: MvStruct, parentStub: StubElement<*>?): MvStructStub {
            return MvStructStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: MvStructStub, sink: IndexSink) = sink.indexStructStub(stub)
    }
}

class MvSchemaStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?
) : StubBase<MvSchema>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvSchemaStub, MvSchema>("SCHEMA") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvSchemaStub(
                parentStub,
                this,
                dataStream.readNameAsString()
            )

        override fun serialize(stub: MvSchemaStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun createPsi(stub: MvSchemaStub): MvSchema =
            MvSchemaImpl(stub, this)

        override fun createStub(psi: MvSchema, parentStub: StubElement<*>?): MvSchemaStub {
            return MvSchemaStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: MvSchemaStub, sink: IndexSink) = sink.indexSchemaStub(stub)
    }
}

class MvConstStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?
) : StubBase<MvConst>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvConstStub, MvConst>("CONST") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvConstStub(
                parentStub,
                this,
                dataStream.readNameAsString()
            )

        override fun serialize(stub: MvConstStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun createPsi(stub: MvConstStub): MvConst =
            MvConstImpl(stub, this)

        override fun createStub(psi: MvConst, parentStub: StubElement<*>?): MvConstStub {
            return MvConstStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: MvConstStub, sink: IndexSink) = sink.indexConstStub(stub)
    }
}



fun factory(name: String): MvStubElementType<*, *> = when (name) {
    "MODULE" -> MvModuleStub.Type
    "FUNCTION" -> MvFunctionStub.Type
    "SPEC_FUNCTION" -> MvSpecFunctionStub.Type
    "STRUCT" -> MvStructStub.Type
    "SCHEMA" -> MvSchemaStub.Type
    "CONST" -> MvConstStub.Type

    else -> error("Unknown element $name")
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
