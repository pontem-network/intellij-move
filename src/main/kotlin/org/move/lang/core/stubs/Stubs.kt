package org.move.lang.core.stubs

import com.intellij.psi.stubs.*
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.impl.MvFunctionImpl
import org.move.lang.core.psi.impl.MvModuleImpl
import org.move.utils.readNameAsString

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

class MvFunctionStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?
) : StubBase<MvFunction>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvFunctionStub, MvFunction>("FUNCTION") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvFunctionStub(
                parentStub,
                this,
                dataStream.readNameAsString()
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
    }
}

fun factory(name: String): MvStubElementType<*, *> = when (name) {
    "MODULE" -> MvModuleStub.Type
    "FUNCTION" -> MvFunctionStub.Type

    else -> error("Unknown element $name")
}
