package org.move.lang.core.stubs

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IStubFileElementType
import org.move.lang.MoveFile
import org.move.lang.MoveLanguage

class MoveFileStub(file: MoveFile) : PsiFileStubImpl<MoveFile>(file) {
    override fun getType() = Type

    object Type : IStubFileElementType<MoveFileStub>(MoveLanguage) {
        // Bump this number if Stub structure changes
        override fun getStubVersion(): Int {
            return 5002
        }
//        override fun getStubVersion(): Int = 5002

        override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
            override fun createStubForFile(file: PsiFile): StubElement<*> {
                return MoveFileStub(file as MoveFile)
            }

//            override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, child: ASTNode): Boolean {
//                val elementType = child.elementType
//                return elementType == CODE_BLOCK && parent.elementType == FUNCTION_DEF
//            }
        }

        override fun getExternalId(): String = "Move.file"
    }
}
