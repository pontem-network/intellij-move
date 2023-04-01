package org.move.lang.core.stubs

import com.intellij.psi.stubs.*
import com.intellij.util.BitUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.impl.*
import org.move.lang.core.types.ItemQualName
import org.move.lang.core.types.StubAddress
import org.move.lang.core.types.psiStubAddress
import org.move.openapiext.readNameAsString
import org.move.openapiext.readUTFFastAsNullable
import org.move.openapiext.writeUTFFastAsNullable
import org.move.stdext.makeBitMask

interface MvNamedStub {
    val name: String?
}

interface MvAttributeOwnerStub {
    val hasAttrs: Boolean

    val isTestOnly: Boolean

    companion object {
        val ATTRS_MASK: Int = makeBitMask(0)
        val TEST_ONLY_MASK: Int = makeBitMask(1)
        const val USED_BITS: Int = 2

        fun extractFlags(element: MvDocAndAttributeOwner): Int =
            extractFlags(element.queryAttributes)

        fun extractFlags(query: QueryAttributes): Int {
            var hasAttrs = false
            var testOnly = false
            for (attrItem in query.attrItems) {
                hasAttrs = true
                if (attrItem.name == "test_only") {
                    testOnly = true
                }
            }

            var flags = 0
            flags = BitUtil.set(flags, ATTRS_MASK, hasAttrs)
            flags = BitUtil.set(flags, TEST_ONLY_MASK, testOnly)
            return flags
        }
    }
}

abstract class MvAttributeOwnerStubBase<T : MvElement>(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>
) : StubBase<T>(parent, elementType),
    MvAttributeOwnerStub {

    override val hasAttrs: Boolean
        get() = BitUtil.isSet(flags, MvAttributeOwnerStub.ATTRS_MASK)

    override val isTestOnly: Boolean
        get() = BitUtil.isSet(flags, MvAttributeOwnerStub.TEST_ONLY_MASK)

    protected abstract val flags: Int
}

class MvModuleStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?,
    override val flags: Int,
    val address: StubAddress,
) : MvAttributeOwnerStubBase<MvModule>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvModuleStub, MvModule>("MODULE") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): MvModuleStub {
            val name = dataStream.readNameAsString()
            val flags = dataStream.readInt()

            val addressInt = dataStream.readInt()
            val address = when (addressInt) {
                StubAddress.UNKNOWN_INT -> StubAddress.Unknown
                StubAddress.VALUE_INT -> StubAddress.Value(dataStream.readUTFFast())
                StubAddress.NAMED_INT -> StubAddress.Named(dataStream.readUTFFast())
                else -> error("Invalid value")
            }

            return MvModuleStub(parentStub, this, name, flags, address)
        }

        override fun serialize(stub: MvModuleStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
                writeInt(stub.flags)
                writeInt(stub.address.asInt())
                when (stub.address) {
                    is StubAddress.Value -> writeUTFFast(stub.address.value)
                    is StubAddress.Named -> writeUTFFast(stub.address.name)
                    is StubAddress.Unknown -> {}
                }
            }

        override fun createPsi(stub: MvModuleStub): MvModule =
            MvModuleImpl(stub, this)

        override fun createStub(psi: MvModule, parentStub: StubElement<*>?): MvModuleStub {
            val attrs = QueryAttributes(psi.attrList.asSequence())
            val flags = MvAttributeOwnerStub.extractFlags(attrs)
            val address = psi.psiStubAddress()
            return MvModuleStub(parentStub, this, psi.name, flags, address)
        }

        override fun indexStub(stub: MvModuleStub, sink: IndexSink) = sink.indexModuleStub(stub)
    }
}

class MvFunctionStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?,
    override val flags: Int,
    val visibility: FunctionVisibility,
    val fqName: ItemQualName,
) : MvAttributeOwnerStubBase<MvFunction>(parent, elementType), MvNamedStub {

    val isTest: Boolean get() = BitUtil.isSet(flags, TEST_MASK)
    val isEntry: Boolean get() = BitUtil.isSet(flags, IS_ENTRY_MASK)
    val isView: Boolean get() = BitUtil.isSet(flags, IS_VIEW_MASK)

    object Type : MvStubElementType<MvFunctionStub, MvFunction>("FUNCTION") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): MvFunctionStub {
            val name = dataStream.readNameAsString()
            val flags = dataStream.readInt()

            val vis = dataStream.readInt()
            val visibility = FunctionVisibility.values()
                .find { it.ordinal == vis } ?: error("Invalid vis value $vis")

            val fqName = ItemQualName.deserialize(dataStream)

            return MvFunctionStub(parentStub, this, name, flags, visibility, fqName)
        }

        override fun serialize(stub: MvFunctionStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
                writeInt(stub.flags)
                writeInt(stub.visibility.ordinal)
                ItemQualName.serialize(stub.fqName, this)
            }

        override fun createPsi(stub: MvFunctionStub): MvFunction =
            MvFunctionImpl(stub, this)

        override fun createStub(psi: MvFunction, parentStub: StubElement<*>?): MvFunctionStub {
            val attrs = QueryAttributes(psi.attrList.asSequence())

            var flags = MvAttributeOwnerStub.extractFlags(attrs)
            flags = BitUtil.set(flags, TEST_MASK, attrs.isTest)
            flags = BitUtil.set(flags, IS_ENTRY_MASK, psi.isEntry)
            flags = BitUtil.set(flags, IS_VIEW_MASK, psi.isView)

            return MvFunctionStub(
                parentStub,
                this,
                psi.name,
                flags,
                visibility = psi.visibilityFromPsi(),
                fqName = psi.qualName
            )
        }

        override fun indexStub(stub: MvFunctionStub, sink: IndexSink) = sink.indexFunctionStub(stub)
    }

    companion object {
        private val TEST_MASK: Int = makeBitMask(MvAttributeOwnerStub.USED_BITS + 1)
        private val IS_ENTRY_MASK: Int = makeBitMask(MvAttributeOwnerStub.USED_BITS + 2)
        private val IS_VIEW_MASK: Int = makeBitMask(MvAttributeOwnerStub.USED_BITS + 3)
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
    override val name: String?,
    override val flags: Int,
) : MvAttributeOwnerStubBase<MvStruct>(parent, elementType), MvNamedStub {

    object Type : MvStubElementType<MvStructStub, MvStruct>("STRUCT") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): MvStructStub {
            val name = dataStream.readNameAsString()
            val flags = dataStream.readInt()
            return MvStructStub(parentStub, this, name, flags)
        }

        override fun serialize(stub: MvStructStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
                writeInt(stub.flags)
            }

        override fun createPsi(stub: MvStructStub): MvStruct =
            MvStructImpl(stub, this)

        override fun createStub(psi: MvStruct, parentStub: StubElement<*>?): MvStructStub {
            val attrs = QueryAttributes(psi.attrList.asSequence())
            val flags = MvAttributeOwnerStub.extractFlags(attrs)
            return MvStructStub(parentStub, this, psi.name, flags)
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

class MvModuleSpecStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    val moduleName: String?,
) : StubBase<MvModuleSpec>(parent, elementType) {

    object Type : MvStubElementType<MvModuleSpecStub, MvModuleSpec>("MODULE_SPEC") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            MvModuleSpecStub(
                parentStub,
                this,
                dataStream.readUTFFastAsNullable()
            )

        override fun serialize(stub: MvModuleSpecStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeUTFFastAsNullable(stub.moduleName)
            }

        override fun createPsi(stub: MvModuleSpecStub): MvModuleSpec =
            MvModuleSpecImpl(stub, this)

        override fun createStub(psi: MvModuleSpec, parentStub: StubElement<*>?): MvModuleSpecStub {
            return MvModuleSpecStub(parentStub, this, psi.fqModuleRef?.referenceName)
        }

        override fun indexStub(stub: MvModuleSpecStub, sink: IndexSink) = sink.indexModuleSpecStub(stub)
    }
}


fun factory(name: String): MvStubElementType<*, *> = when (name) {
    "MODULE" -> MvModuleStub.Type
    "FUNCTION" -> MvFunctionStub.Type
    "SPEC_FUNCTION" -> MvSpecFunctionStub.Type
    "STRUCT" -> MvStructStub.Type
    "SCHEMA" -> MvSchemaStub.Type
    "CONST" -> MvConstStub.Type
    "MODULE_SPEC" -> MvModuleSpecStub.Type

    else -> error("Unknown element $name")
}
