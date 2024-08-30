package org.move.lang.core.stubs

import com.intellij.psi.stubs.*
import com.intellij.util.BitUtil
import org.move.cli.MoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.impl.*
import org.move.lang.core.types.StubAddress
import org.move.lang.core.types.deserializeStubAddress
import org.move.lang.core.types.psiStubAddress
import org.move.lang.core.types.serializeStubAddress
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

    val isVerifyOnly: Boolean

    companion object {
        val ATTRS_MASK: Int = makeBitMask(0)
        val TEST_ONLY_MASK: Int = makeBitMask(1)
        val VERIFY_ONLY_MASK: Int = makeBitMask(2)
        const val USED_BITS: Int = 3

        fun extractFlags(query: QueryAttributes): Int {
            var hasAttrs = false
            var testOnly = false
            var verifyOnly = false
            for (attrItem in query.attrItems) {
                hasAttrs = true
                if (attrItem.referenceName == "test_only") {
                    testOnly = true
                }
                if (attrItem.referenceName == "verify_only") {
                    verifyOnly = true
                }
            }

            var flags = 0
            flags = BitUtil.set(flags, ATTRS_MASK, hasAttrs)
            flags = BitUtil.set(flags, TEST_ONLY_MASK, testOnly)
            flags = BitUtil.set(flags, VERIFY_ONLY_MASK, verifyOnly)
            return flags
        }
    }
}

abstract class MvAttributeOwnerStubBase<T: MvElement>(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>
): StubBase<T>(parent, elementType),
   MvAttributeOwnerStub {

    override val hasAttrs: Boolean
        get() = BitUtil.isSet(flags, MvAttributeOwnerStub.ATTRS_MASK)

    override val isTestOnly: Boolean
        get() = BitUtil.isSet(flags, MvAttributeOwnerStub.TEST_ONLY_MASK)

    override val isVerifyOnly: Boolean
        get() = BitUtil.isSet(flags, MvAttributeOwnerStub.VERIFY_ONLY_MASK)

    protected abstract val flags: Int
}

class MvModuleStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?,
    override val flags: Int,
    val address: StubAddress,
): MvAttributeOwnerStubBase<MvModule>(parent, elementType), MvNamedStub {

    object Type: MvStubElementType<MvModuleStub, MvModule>("MODULE") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): MvModuleStub {
            val name = dataStream.readNameAsString()
            val flags = dataStream.readInt()

            val stubAddress = dataStream.deserializeStubAddress()

            return MvModuleStub(parentStub, this, name, flags, stubAddress)
        }

        override fun serialize(stub: MvModuleStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
                writeInt(stub.flags)
                serializeStubAddress(stub.address)
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
    val visibility: VisKind?,
    val address: StubAddress,
    val moduleName: String?,
): MvAttributeOwnerStubBase<MvFunction>(parent, elementType), MvNamedStub {

    val isTest: Boolean get() = BitUtil.isSet(flags, TEST_MASK)
    val isEntry: Boolean get() = BitUtil.isSet(flags, IS_ENTRY_MASK)
    val isView: Boolean get() = BitUtil.isSet(flags, IS_VIEW_MASK)

    val unresolvedQualName: String?
        get() {
            val addressText = when (address) {
                is StubAddress.Value -> address.value
                is StubAddress.Named -> address.name
                else -> return null
            }
            val moduleName = this.moduleName ?: return null
            val itemName = this.name ?: return null
            return "$addressText::$moduleName::$itemName"
        }

    fun resolvedQualName(moveProject: MoveProject): String? {
        val addressText = when (address) {
            is StubAddress.Value -> address.value
            is StubAddress.Named -> moveProject.getValueOfDeclaredNamedAddress(address.name) ?: return null
            else -> return null
        }
        val moduleName = this.moduleName ?: return null
        val itemName = this.name ?: return null
        return "$addressText::$moduleName::$itemName"
    }

    object Type: MvStubElementType<MvFunctionStub, MvFunction>("FUNCTION") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): MvFunctionStub {
            val name = dataStream.readNameAsString()
            val flags = dataStream.readInt()

            val visSyntax = dataStream.readUTFFastAsNullable()
            val visibility = visSyntax?.let { kw ->
                VisKind.entries.find { it.keyword == kw } ?: error("Invalid vis keyword $kw")
            }

            val stubAddress = dataStream.deserializeStubAddress()
            val moduleName = dataStream.readUTFFastAsNullable()

            return MvFunctionStub(
                parentStub,
                this,
                name,
                flags,
                visibility,
                stubAddress,
                moduleName
            )
        }

        override fun serialize(stub: MvFunctionStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
                writeInt(stub.flags)
                writeUTFFastAsNullable(stub.visibility?.keyword)
                serializeStubAddress(stub.address)
                writeUTFFastAsNullable(stub.moduleName)
            }

        override fun createPsi(stub: MvFunctionStub): MvFunction =
            MvFunctionImpl(stub, this)

        override fun createStub(psi: MvFunction, parentStub: StubElement<*>?): MvFunctionStub {
            val attrs = QueryAttributes(psi.attrList.asSequence())

            var flags = MvAttributeOwnerStub.extractFlags(attrs)
            flags = BitUtil.set(flags, TEST_MASK, attrs.isTest)
            flags = BitUtil.set(flags, IS_ENTRY_MASK, psi.isEntry)
            flags = BitUtil.set(flags, IS_VIEW_MASK, psi.isView)

            val moduleStub = parentStub as? MvModuleStub
            val moduleAddress = moduleStub?.address ?: StubAddress.Unknown
            val moduleName = moduleStub?.name

            return MvFunctionStub(
                parentStub,
                this,
                psi.name,
                flags,
                visibility = psi.visibilityModifier?.stubVisKind,
                address = moduleAddress,
                moduleName = moduleName,
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
): StubBase<MvSpecFunction>(parent, elementType), MvNamedStub {

    object Type: MvStubElementType<MvSpecFunctionStub, MvSpecFunction>("SPEC_FUNCTION") {
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
): MvAttributeOwnerStubBase<MvStruct>(parent, elementType), MvNamedStub {

    object Type: MvStubElementType<MvStructStub, MvStruct>("STRUCT") {
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

class MvEnumStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?,
    override val flags: Int,
): MvAttributeOwnerStubBase<MvEnum>(parent, elementType), MvNamedStub {

    object Type: MvStubElementType<MvEnumStub, MvEnum>("ENUM") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): MvEnumStub {
            val name = dataStream.readNameAsString()
            val flags = dataStream.readInt()
            return MvEnumStub(parentStub, this, name, flags)
        }

        override fun serialize(stub: MvEnumStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
                writeInt(stub.flags)
            }

        override fun createPsi(stub: MvEnumStub): MvEnum =
            MvEnumImpl(stub, this)

        override fun createStub(psi: MvEnum, parentStub: StubElement<*>?): MvEnumStub {
            val attrs = QueryAttributes(psi.attrList.asSequence())
            val flags = MvAttributeOwnerStub.extractFlags(attrs)
            return MvEnumStub(parentStub, this, psi.name, flags)
        }

        override fun indexStub(stub: MvEnumStub, sink: IndexSink) = sink.indexEnumStub(stub)
    }
}

class MvEnumVariantStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?,
    override val flags: Int,
): MvAttributeOwnerStubBase<MvEnumVariant>(parent, elementType), MvNamedStub {

    object Type: MvStubElementType<MvEnumVariantStub, MvEnumVariant>("ENUM_VARIANT") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): MvEnumVariantStub {
            val name = dataStream.readNameAsString()
            val flags = dataStream.readInt()
            return MvEnumVariantStub(parentStub, this, name, flags)
        }

        override fun serialize(stub: MvEnumVariantStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
                writeInt(stub.flags)
            }

        override fun createPsi(stub: MvEnumVariantStub): MvEnumVariant =
            MvEnumVariantImpl(stub, this)

        override fun createStub(psi: MvEnumVariant, parentStub: StubElement<*>?): MvEnumVariantStub {
            val attrs = QueryAttributes(psi.attrList.asSequence())
            val flags = MvAttributeOwnerStub.extractFlags(attrs)
            return MvEnumVariantStub(parentStub, this, psi.name, flags)
        }

        override fun indexStub(stub: MvEnumVariantStub, sink: IndexSink) = sink.indexEnumVariantStub(stub)
    }
}

class MvSchemaStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?
): StubBase<MvSchema>(parent, elementType), MvNamedStub {

    object Type: MvStubElementType<MvSchemaStub, MvSchema>("SCHEMA") {
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
): StubBase<MvConst>(parent, elementType), MvNamedStub {

    object Type: MvStubElementType<MvConstStub, MvConst>("CONST") {
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
): StubBase<MvModuleSpec>(parent, elementType) {

    object Type: MvStubElementType<MvModuleSpecStub, MvModuleSpec>("MODULE_SPEC") {
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
            return MvModuleSpecStub(parentStub, this, psi.path?.referenceName)
        }

        override fun indexStub(stub: MvModuleSpecStub, sink: IndexSink) = sink.indexModuleSpecStub(stub)
    }
}


fun factory(name: String): MvStubElementType<*, *> = when (name) {
    "MODULE" -> MvModuleStub.Type
    "FUNCTION" -> MvFunctionStub.Type
    "SPEC_FUNCTION" -> MvSpecFunctionStub.Type
    "STRUCT" -> MvStructStub.Type
    "ENUM" -> MvEnumStub.Type
    "ENUM_VARIANT" -> MvEnumVariantStub.Type
    "SCHEMA" -> MvSchemaStub.Type
    "CONST" -> MvConstStub.Type
    "MODULE_SPEC" -> MvModuleSpecStub.Type

    else -> error("Unknown element $name")
}
