package org.move.lang.core.stubs
//
//import com.intellij.psi.PsiFile
//import com.intellij.psi.StubBuilder
//import com.intellij.psi.impl.source.tree.TreeUtil
//import com.intellij.psi.stubs.*
//import com.intellij.psi.tree.IStubFileElementType
//import org.move.lang.MoveFile
//import org.move.lang.MoveLanguage
//import org.move.lang.core.psi.MoveAddressDef
//import org.move.lang.core.psi.MoveModuleDef
//import org.move.lang.core.psi.impl.MoveAddressDefImpl
//import org.move.lang.core.psi.impl.MoveModuleDefImpl
//
//class MoveFileStub(file: MoveFile) : PsiFileStubImpl<MoveFile>(file) {
//
//    override fun getType() = Type
//
//    object Type : IStubFileElementType<MoveFileStub>(MoveLanguage) {
//        // Bump this number if Stub structure changes
//        private const val STUB_VERSION = 5002
//
//        override fun getStubVersion(): Int = STUB_VERSION
//
//        override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
//            override fun createStubForFile(file: PsiFile): StubElement<*> {
//                TreeUtil.ensureParsed(file.node)
//                return MoveFileStub(file as MoveFile)
//            }
//
////            override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, child: ASTNode): Boolean {
////                val elementType = child.elementType
////                return elementType == CODE_BLOCK && parent.elementType == FUNCTION_DEF
////            }
//        }
//
//        override fun getExternalId(): String = "Move.file"
//    }
//}
//
//class MoveAddressDefStub(
//    parent: StubElement<*>?,
//    elementType: IStubElementType<*, *>,
//) : StubBase<MoveAddressDef>(parent, elementType) {
//
//    object Type : MoveStubElementType<MoveAddressDefStub, MoveAddressDef>("ADDRESS_DEF") {
//
//        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
//            MoveAddressDefStub(
//                parentStub, this,
//            )
//
//        override fun serialize(stub: MoveAddressDefStub, dataStream: StubOutputStream) {}
//
//        override fun createPsi(stub: MoveAddressDefStub): MoveAddressDef =
//            MoveAddressDefImpl(stub, this)
//
//        override fun createStub(psi: MoveAddressDef, parentStub: StubElement<*>?) =
//            MoveAddressDefStub(parentStub, this)
//
////        override fun indexStub(stub: MoveAddressDefStub, sink: IndexSink) = sin
//    }
//}
//
//class MoveModuleDefStub(
//    parent: StubElement<*>?,
//    elementType: IStubElementType<*, *>,
//    override val name: String?,
//) : StubBase<MoveModuleDef>(parent, elementType),
//    MoveNamedStub {
//
//    object Type : MoveStubElementType<MoveModuleDefStub, MoveModuleDef>("MODULE_DEF") {
//
//        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
//            MoveModuleDefStub(
//                parentStub, this,
//                dataStream.readNameAsString(),
//            )
//
//        override fun serialize(stub: MoveModuleDefStub, dataStream: StubOutputStream) =
//            with(dataStream) {
//                writeName(stub.name)
//            }
//
//        override fun createPsi(stub: MoveModuleDefStub): MoveModuleDef =
//            MoveModuleDefImpl(stub, this)
//
//        override fun createStub(psi: MoveModuleDef, parentStub: StubElement<*>?) =
//            MoveModuleDefStub(parentStub, this, psi.name)
//
////        override fun indexStub(stub: MoveModuleDefStub, sink: IndexSink) = sink.indexNamedStub(stub)
//    }
//}
//
//
//fun factory(name: String): MoveStubElementType<*, *> = when (name) {
//    "ADDRESS_DEF" -> MoveAddressDefStub.Type
//    "MODULE_DEF" -> MoveModuleDefStub.Type
////    "FUNCTION_DEF" -> MoveFunctionDefStub.Type
////    "FUNCTION_PARAMETER_LIST" -> PlaceholderStub.Type(
////        "FUNCTION_PARAMETER_LIST", ::MoveFunctionParameterListImpl)
//    else -> error("Unknown element $name")
//}
////
////class MoveFunctionDefStub(
////    parent: StubElement<*>?, elementType: IStubElementType<*, *>,
////    override val name: String?,
////    val isPublic: Boolean,
////) : StubBase<MoveFunctionDef>(parent, elementType),
////    MoveNamedStub {
////
////    object Type : MoveStubElementType<MoveFunctionDefStub, MoveFunctionDef>("FUNCTION_DEF") {
////        override fun deserialize(
////            dataStream: StubInputStream,
////            parentStub: StubElement<*>?,
////        ): MoveFunctionDefStub =
////            MoveFunctionDefStub(parentStub, this, dataStream.readName()?.string, dataStream.readBoolean())
////
////        override fun serialize(stub: MoveFunctionDefStub, dataStream: StubOutputStream) =
////            with(dataStream) {
////                writeName(stub.name)
////                writeBoolean(stub.isPublic)
////            }
////
////        override fun createPsi(stub: MoveFunctionDefStub): MoveFunctionDef =
////            MoveFunctionDefImpl(stub, this)
////
////        override fun createStub(psi: MoveFunctionDef, parentStub: StubElement<*>?): MoveFunctionDefStub =
////            MoveFunctionDefStub(parentStub, this,
////                name = psi.name,
////                isPublic = psi.isPublic
////            )
////
////        override fun indexStub(stub: MoveFunctionDefStub, sink: IndexSink) = sink.indexFunctionDef(stub)
////    }
////}
//
//private fun StubInputStream.readNameAsString(): String? = readName()?.string