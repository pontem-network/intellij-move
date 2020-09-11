package org.move.lang.core.stubs
//
//import com.intellij.lang.ASTNode
//import com.intellij.psi.PsiFile
//import com.intellij.psi.StubBuilder
//import com.intellij.psi.impl.source.tree.TreeUtil
//import com.intellij.psi.stubs.*
//import com.intellij.psi.tree.IStubFileElementType
//import org.move.lang.MoveElementTypes.CODE_BLOCK
//import org.move.lang.MoveElementTypes.FUNCTION_DEF
//import org.move.lang.MoveFile
//import org.move.lang.MoveLanguage
//import org.move.lang.core.psi.MoveFunctionDef
//import org.move.lang.core.psi.ext.isPublic
//import org.move.lang.core.psi.impl.MoveFunctionDefImpl
//import org.move.lang.core.psi.impl.MoveFunctionParameterListImpl
//
//class MoveFileStub(file: MoveFile) : PsiFileStubImpl<MoveFile>(file) {
//    override fun getType() = Type
//
//    object Type : IStubFileElementType<MoveFileStub>(MoveLanguage) {
//        // Bump this number if Stub structure changes
//        private const val STUB_VERSION = 1
//
//        override fun getStubVersion(): Int = STUB_VERSION
//
//        override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
//            override fun createStubForFile(file: PsiFile): StubElement<*> {
//                TreeUtil.ensureParsed(file.node)
//                return MoveFileStub(file as MoveFile)
//            }
//
//            override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, child: ASTNode): Boolean {
//                val elementType = child.elementType
//                return elementType == CODE_BLOCK && parent.elementType == FUNCTION_DEF
//            }
//        }
//
//        override fun getExternalId(): String = "Move.file"
//    }
//}
//
//fun factory(name: String): MoveStubElementType<*, *> = when (name) {
//    "FUNCTION_DEF" -> MoveFunctionDefStub.Type
//    "FUNCTION_PARAMETER_LIST" -> PlaceholderStub.Type(
//        "FUNCTION_PARAMETER_LIST", ::MoveFunctionParameterListImpl)
//    else -> error("Unknown element $name")
//}
//
//class MoveFunctionDefStub(
//    parent: StubElement<*>?, elementType: IStubElementType<*, *>,
//    override val name: String?,
//    val isPublic: Boolean,
//) : StubBase<MoveFunctionDef>(parent, elementType),
//    MoveNamedStub {
//
//    object Type : MoveStubElementType<MoveFunctionDefStub, MoveFunctionDef>("FUNCTION_DEF") {
//        override fun deserialize(
//            dataStream: StubInputStream,
//            parentStub: StubElement<*>?,
//        ): MoveFunctionDefStub =
//            MoveFunctionDefStub(parentStub, this, dataStream.readName()?.string, dataStream.readBoolean())
//
//        override fun serialize(stub: MoveFunctionDefStub, dataStream: StubOutputStream) =
//            with(dataStream) {
//                writeName(stub.name)
//                writeBoolean(stub.isPublic)
//            }
//
//        override fun createPsi(stub: MoveFunctionDefStub): MoveFunctionDef =
//            MoveFunctionDefImpl(stub, this)
//
//        override fun createStub(psi: MoveFunctionDef, parentStub: StubElement<*>?): MoveFunctionDefStub =
//            MoveFunctionDefStub(parentStub, this,
//                name = psi.name,
//                isPublic = psi.isPublic
//            )
//
//        override fun indexStub(stub: MoveFunctionDefStub, sink: IndexSink) = sink.indexFunctionDef(stub)
//    }
//}