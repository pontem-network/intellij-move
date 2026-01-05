package org.move.utils.tests

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.util.SystemProperties
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.UIUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly
import org.move.cli.moveProjectsService
import org.move.openapiext.toPsiDirectory
import org.move.openapiext.toPsiFile
import org.move.openapiext.toVirtualFile
import org.move.utils.tests.base.TestCase
import java.lang.annotation.Inherited

@TestOnly
fun setRegistryKey(key: String, value: Boolean) = Registry.get(key).setValue(value)

@Inherited
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SkipOnProduct(val product: String)

fun MvProjectTestBase.handleSkipOnProductAnnotations() {
    val currentProduct = ApplicationNamesInfo.getInstance().fullProductName
    val skipOnProducts = this.findAnnotationInstances<SkipOnProduct>()
    for (skipOn in skipOnProducts) {
        if (skipOn.product == currentProduct) {
            this.skipTestWithReason = "Skip on ${skipOn.product}"
        }
    }
}

abstract class MvProjectTestBase: CodeInsightFixtureTestCase<ModuleFixtureBuilder<*>>() {

    //    var isProjectInitialized: Boolean = false
    var skipTestWithReason: String? = null

    override fun setUp() {
        super.setUp()

        val isDebugMode = this.findAnnotationInstance<DebugMode>()?.enabled ?: true
        setRegistryKey("org.move.debug.enabled", isDebugMode)

        val withAdvancedSettings = this.findAnnotationInstances<WithAdvancedSetting>()
        for (withAdvancedSetting in withAdvancedSettings) {
            // todo: could be done in generic way to support every type
            AdvancedSettings.setBoolean(withAdvancedSetting.id, withAdvancedSetting.value)
        }

        this.handleMoveV2Annotation(project)
        this.handleSkipOnProductAnnotations()
    }

    override fun tearDown() {
        val withAdvancedSettings = this.findAnnotationInstances<WithAdvancedSetting>()
        for (withAdvancedSetting in withAdvancedSettings) {
            AdvancedSettings.setBoolean(
                withAdvancedSetting.id,
                AdvancedSettings.getDefaultBoolean(withAdvancedSetting.id)
            )
        }
        try {
            super.tearDown()
            // suppress ThreadLeak error
        } catch (e: AssertionError) {
            addSuppressedException(e)
        }
    }

    override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
        val reason = this.skipTestWithReason
        if (reason != null) {
            System.err.println("SKIP \"$name\": $reason")
            return
        }
        super.runTestRunnable(testRunnable)
    }

//    override fun tearDown() {
////        isProjectInitialized = false
//        super.tearDown()
//    }

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }

    @Suppress("JUnitMalformedDeclaration")
    fun testProject(@Language("Move") code: String): TestProject {
        val fileTree = fileTreeFromText(code)
        return testProject(fileTree)
    }

    @Suppress("JUnitMalformedDeclaration")
    fun testProject(builder: FileTreeBuilder.() -> Unit): TestProject {
        val fileTree = fileTree(builder)
        return testProject(fileTree)
    }

    private fun testProject(fileTree: FileTree): TestProject {
        val rootDirectory = this.rootDirectory ?: error("myModule should exist")

        val testProject = fileTree.create(myFixture.project, rootDirectory)
        myFixture.configureFromFileWithCaret(testProject)

        SystemProperties.setProperty("user.home", testProject.rootDirectory.path)
        project.moveProjectsService.scheduleProjectsRefreshSync("from test project")

        return testProject
    }

    val rootDirectory: VirtualFile? get() = myModule?.rootManager?.contentRoots?.firstOrNull()

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
        PsiManagerEx.getInstanceEx(project)
            .setAssertOnFileLoadingFilter(VirtualFileFilter.ALL, testRootDisposable)
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
        val parts = FileUtil.splitPath(path, '/')
        var res = this.rootDirectory ?: error("no root")
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
