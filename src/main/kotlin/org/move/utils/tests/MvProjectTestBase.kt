package org.move.utils.tests

import com.intellij.openapi.project.rootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.intellij.lang.annotations.Language
import org.move.openapiext.toVirtualFile
import org.move.openapiext.toPsiDirectory
import org.move.openapiext.toPsiFile
import org.move.utils.tests.base.TestCase

abstract class MvProjectTestBase : CodeInsightFixtureTestCase<ModuleFixtureBuilder<*>>() {
    var testProject: TestProject? = null

    override fun setUp() {
        super.setUp()
//        val privateKey = this.findAnnotationInstance<SettingsPrivateKey>()?.privateKey
//        if (privateKey != null) {
//            project.moveSettings.modifyTemporary(testRootDisposable) {
//                it.privateKey = privateKey
//            }
//        }
    }

    override fun tearDown() {
        testProject = null
        super.tearDown()
    }

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }

    protected fun testProjectFromFileTree(@Language("Move") code: String): TestProject {
        val fileTree = fileTreeFromText(code)
        val rootDirectory = myModule.rootManager.contentRoots.first()
        return fileTree.prepareTestProject(myFixture.project, rootDirectory)
    }

    protected fun testProjectFromFileTree(builder: FileTreeBuilder.() -> Unit): TestProject {
        val fileTree = fileTree(builder)
        val rootDirectory = myModule.rootManager.contentRoots.first()
        return fileTree.prepareTestProject(myFixture.project, rootDirectory)
    }

    fun testProject(builder: FileTreeBuilder.() -> Unit) {
        val testProject = testProjectFromFileTree(builder)
        this.testProject = testProject
        myFixture.configureFromFileWithCaret(testProject)
    }

    protected fun CodeInsightTestFixture.configureFromFileWithCaret(testProject: TestProject) {
        val fileWithCaret =
            testProject.rootDirectory.toNioPath()
                .resolve(testProject.fileWithCaret).toVirtualFile()
                ?: error("No file with //^ caret")
        this.configureFromExistingVirtualFile(fileWithCaret)
    }

    protected fun CodeInsightTestFixture.configureFromFileWithNamedElement(testProject: TestProject) {
        val fileWithNamedElement =
            testProject.rootDirectory.toNioPath()
                .resolve(testProject.fileWithNamedElement).toVirtualFile()
                ?: error("No file with //X caret")
        this.configureFromExistingVirtualFile(fileWithNamedElement)
    }

    protected fun findPsiFile(path: String): PsiFile {
        val vFile = findVirtualFile(path)
        return vFile.toPsiFile(this.project) ?: error("$path is not a file")
    }

    protected fun findPsiDirectory(path: String): PsiDirectory {
        val vFile = findVirtualFile(path)
        return vFile.toPsiDirectory(this.project) ?: error("$path is not a directory")
    }

    private fun findVirtualFile(path: String): VirtualFile {
        val rootDirectory = this.testProject?.rootDirectory ?: error("no root")
        val parts = FileUtil.splitPath(path, '/')
        var res = rootDirectory
        for (part in parts) {
            res = res.findChild(part) ?: error("cannot find $path")
        }
        return res
    }
}
