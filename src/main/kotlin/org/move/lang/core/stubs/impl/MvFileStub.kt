package org.move.lang.core.stubs.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.tree.IStubFileElementType
import org.move.lang.MoveFile
import org.move.lang.MoveLanguage
import org.move.lang.MoveParserDefinition

class MvFileStub(file: MoveFile?) : PsiFileStubImpl<MoveFile>(file) {

    override fun getType() = Type

    object Type : IStubFileElementType<MvFileStub>(MoveLanguage) {
        private const val STUB_VERSION = 12

        // Bump this number if Stub structure changes
        override fun getStubVersion(): Int = MoveParserDefinition.PARSER_VERSION + STUB_VERSION

        override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
            override fun createStubForFile(file: PsiFile): StubElement<*> {
                TreeUtil.ensureParsed(file.node) // profiler hint
                check(file is MoveFile)
                return MvFileStub(file)
            }
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) = MvFileStub(null)

        override fun getExternalId(): String = "Move.file"

//        // Uncomment to find out what causes switch to the AST
//        private val PARSED = com.intellij.util.containers.ContainerUtil.newConcurrentSet<String>()
//
//        override fun doParseContents(chameleon: ASTNode, psi: com.intellij.psi.PsiElement): ASTNode? {
//            val path = psi.containingFile?.virtualFile?.path
//            if (path != null && PARSED.add(path)) {
//                println("Parsing (${PARSED.size}) $path")
//                val trace = java.io.StringWriter().also { writer ->
//                    Exception().printStackTrace(java.io.PrintWriter(writer))
//                    writer.toString()
//                }
//                println(trace)
//                println()
//            }
//            return super.doParseContents(chameleon, psi)
//        }
    }
}
