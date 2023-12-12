package org.move.utils.tests

import com.intellij.openapi.project.rootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.intellij.lang.annotations.Language
import org.move.cli.moveProjectsService
import org.move.openapiext.toPsiDirectory
import org.move.openapiext.toPsiFile
import org.move.openapiext.toVirtualFile
import org.move.utils.tests.base.TestCase

abstract class MvProjectTestBase : CodeInsightFixtureTestCase<ModuleFixtureBuilder<*>>() {
    var _testProject: TestProject? = null

    override fun tearDown() {
        _testProject = null
        super.tearDown()
    }

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }

    fun testProject(@Language("Move") code: String): TestProject {
        val fileTree = fileTreeFromText(code)
        return testProject(fileTree)
    }

    fun testProject(builder: FileTreeBuilder.() -> Unit): TestProject {
        val fileTree = fileTree(builder)
        return testProject(fileTree)
    }

    private fun testProject(fileTree: FileTree): TestProject {
        val rootDirectory = myModule.rootManager.contentRoots.first()
        val testProject = fileTree.toTestProject(myFixture.project, rootDirectory)
        this._testProject = testProject
        myFixture.configureFromFileWithCaret(testProject)

        System.setProperty("user.home", testProject.rootDirectory.path)
        project.moveProjectsService.scheduleProjectsRefresh()
        return testProject
    }

    protected fun CodeInsightTestFixture.configureFromFileWithCaret(testProject: TestProject) {
        val fileWithCaret =
            testProject.rootDirectory.toNioPath()
                .resolve(testProject.fileWithCaret).toVirtualFile()
                ?: error("No file with //^ caret")
        this.configureFromExistingVirtualFile(fileWithCaret)
    }

    protected fun checkAstNotLoaded(fileFilter: VirtualFileFilter) {
        PsiManagerEx.getInstanceEx(project).setAssertOnFileLoadingFilter(fileFilter, testRootDisposable)
    }

    protected fun checkAstNotLoaded() {
        PsiManagerEx.getInstanceEx(project).setAssertOnFileLoadingFilter(VirtualFileFilter.ALL, testRootDisposable)
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
        val rootDirectory = this._testProject?.rootDirectory ?: error("no root")
        val parts = FileUtil.splitPath(path, '/')
        var res = rootDirectory
        for (part in parts) {
            res = res.findChild(part) ?: error("cannot find $path")
        }
        return res
    }
}
