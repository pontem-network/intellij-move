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
import com.intellij.util.ui.UIUtil
import org.intellij.lang.annotations.Language
import org.move.cli.moveProjectsService
import org.move.cli.settings.Blockchain
import org.move.cli.settings.moveSettings
import org.move.openapiext.toPsiDirectory
import org.move.openapiext.toPsiFile
import org.move.openapiext.toVirtualFile
import org.move.utils.tests.base.TestCase

abstract class MvProjectTestBase : CodeInsightFixtureTestCase<ModuleFixtureBuilder<*>>() {
    var _testProject: TestProject? = null

    override fun setUp() {
        super.setUp()

        val debugMode = this.findAnnotationInstance<DebugMode>()?.enabled ?: true
        val blockchain = this.findAnnotationInstance<WithBlockchain>()?.blockchain ?: Blockchain.APTOS
        // triggers projects refresh
        project.moveSettings.modify {
            it.debugMode = debugMode
            it.blockchain = blockchain
        }
    }

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

    /**
     * Tries to launches [action]. If it returns `false`, invokes [UIUtil.dispatchAllInvocationEvents] and tries again
     *
     * Can be used to wait file system refresh, for example
     */
    protected fun runWithInvocationEventsDispatching(
        errorMessage: String = "Failed to invoke `action` successfully",
        retries: Int = 1000,
        action: () -> Boolean
    ) {
        repeat(retries) {
            UIUtil.dispatchAllInvocationEvents()
            if (action()) {
                return
            }
            Thread.sleep(10)
        }
        error(errorMessage)
    }
}
