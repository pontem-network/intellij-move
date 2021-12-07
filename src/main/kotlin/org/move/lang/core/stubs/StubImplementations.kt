package org.move.lang.core.stubs
//
//import com.intellij.psi.PsiFile
//import com.intellij.psi.StubBuilder
//import com.intellij.psi.impl.source.tree.TreeUtil
//import com.intellij.psi.stubs.*
//import com.intellij.psi.tree.IStubFileElementType
//import org.move.lang.MvFile
//import org.move.lang.MvLanguage
//import org.move.lang.core.psi.MvAddressDef
//import org.move.lang.core.psi.MvModuleDef
//import org.move.lang.core.psi.impl.MvAddressDefImpl
//import org.move.lang.core.psi.impl.MvModuleDefImpl
//
//class MvFileStub(file: MvFile) : PsiFileStubImpl<MvFile>(file) {
//
//    override fun getType() = Type
//
//    object Type : IStubFileElementType<MvFileStub>(MvLanguage) {
//        // Bump this number if Stub structure changes
//        private const val STUB_VERSION = 5002
//
//        override fun getStubVersion(): Int = STUB_VERSION
//
//        override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
//            override fun createStubForFile(file: PsiFile): StubElement<*> {
//                TreeUtil.ensureParsed(file.node)
//                return MvFileStub(file as MvFile)
//            }
//
////            override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, child: ASTNode): Boolean {
////                val elementType = child.elementType
////                return elementType == CODE_BLOCK && parent.elementType == FUNCTION_DEF
////            }
//        }
//
//        override fun getExternalId(): String = "Mv.file"
//    }
//}
//
//class MvAddressDefStub(
//    parent: StubElement<*>?,
//    elementType: IStubElementType<*, *>,
//) : StubBase<MvAddressDef>(parent, elementType) {
//
//    object Type : MvStubElementType<MvAddressDefStub, MvAddressDef>("ADDRESS_DEF") {
//
//        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
//            MvAddressDefStub(
//                parentStub, this,
//            )
//
//        override fun serialize(stub: MvAddressDefStub, dataStream: StubOutputStream) {}
//
//        override fun createPsi(stub: MvAddressDefStub): MvAddressDef =
//            MvAddressDefImpl(stub, this)
//
//        override fun createStub(psi: MvAddressDef, parentStub: StubElement<*>?) =
//            MvAddressDefStub(parentStub, this)
//
////        override fun indexStub(stub: MvAddressDefStub, sink: IndexSink) = sin
//    }
//}
//
//class MvModuleDefStub(
//    parent: StubElement<*>?,
//    elementType: IStubElementType<*, *>,
//    override val name: String?,
//) : StubBase<MvModuleDef>(parent, elementType),
//    MvNamedStub {
//
//    object Type : MvStubElementType<MvModuleDefStub, MvModuleDef>("MODULE_DEF") {
//
//        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
//            MvModuleDefStub(
//                parentStub, this,
//                dataStream.readNameAsString(),
//            )
//
//        override fun serialize(stub: MvModuleDefStub, dataStream: StubOutputStream) =
//            with(dataStream) {
//                writeName(stub.name)
//            }
//
//        override fun createPsi(stub: MvModuleDefStub): MvModuleDef =
//            MvModuleDefImpl(stub, this)
//
//        override fun createStub(psi: MvModuleDef, parentStub: StubElement<*>?) =
//            MvModuleDefStub(parentStub, this, psi.name)
//
////        override fun indexStub(stub: MvModuleDefStub, sink: IndexSink) = sink.indexNamedStub(stub)
//    }
//}
//
//
//fun factory(name: String): MvStubElementType<*, *> = when (name) {
//    "ADDRESS_DEF" -> MvAddressDefStub.Type
//    "MODULE_DEF" -> MvModuleDefStub.Type
////    "FUNCTION_DEF" -> MvFunctionDefStub.Type
////    "FUNCTION_PARAMETER_LIST" -> PlaceholderStub.Type(
////        "FUNCTION_PARAMETER_LIST", ::MvFunctionParameterListImpl)
//    else -> error("Unknown element $name")
//}
////
////class MvFunctionDefStub(
////    parent: StubElement<*>?, elementType: IStubElementType<*, *>,
////    override val name: String?,
////    val isPublic: Boolean,
////) : StubBase<MvFunctionDef>(parent, elementType),
////    MvNamedStub {
////
////    object Type : MvStubElementType<MvFunctionDefStub, MvFunctionDef>("FUNCTION_DEF") {
////        override fun deserialize(
////            dataStream: StubInputStream,
////            parentStub: StubElement<*>?,
////        ): MvFunctionDefStub =
////            MvFunctionDefStub(parentStub, this, dataStream.readName()?.string, dataStream.readBoolean())
////
////        override fun serialize(stub: MvFunctionDefStub, dataStream: StubOutputStream) =
////            with(dataStream) {
////                writeName(stub.name)
////                writeBoolean(stub.isPublic)
////            }
////
////        override fun createPsi(stub: MvFunctionDefStub): MvFunctionDef =
////            MvFunctionDefImpl(stub, this)
////
////        override fun createStub(psi: MvFunctionDef, parentStub: StubElement<*>?): MvFunctionDefStub =
////            MvFunctionDefStub(parentStub, this,
////                name = psi.name,
////                isPublic = psi.isPublic
////            )
////
////        override fun indexStub(stub: MvFunctionDefStub, sink: IndexSink) = sink.indexFunctionDef(stub)
////    }
////}
//
//private fun StubInputStream.readNameAsString(): String? = readName()?.string
