package org.move.utils.tests

import com.intellij.openapi.project.rootManager
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.intellij.lang.annotations.Language
import org.move.cli.settings.moveSettings
import org.move.openapiext.findVirtualFile
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
                .resolve(testProject.fileWithCaret).findVirtualFile()
                ?: error("No file with //^ caret")
        this.configureFromExistingVirtualFile(fileWithCaret)
    }

    protected fun CodeInsightTestFixture.configureFromFileWithNamedElement(testProject: TestProject) {
        val fileWithNamedElement =
            testProject.rootDirectory.toNioPath()
                .resolve(testProject.fileWithNamedElement).findVirtualFile()
                ?: error("No file with //X caret")
        this.configureFromExistingVirtualFile(fileWithNamedElement)
    }


}
