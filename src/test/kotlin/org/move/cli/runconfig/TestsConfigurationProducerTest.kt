package org.move.cli.runconfig

import com.intellij.psi.PsiElement
import org.move.cli.runconfig.producers.TestRunConfigurationProducer
import org.move.lang.core.psi.MvFunction
import org.move.cli.settings.ProjectType
import org.move.lang.core.psi.MvModuleDef
import org.move.utils.tests.RunConfigurationProducerTestBase
import org.move.utils.tests.SettingsProjectType

class TestsConfigurationProducerTest : RunConfigurationProducerTestBase("test") {
    @SettingsProjectType(ProjectType.DOVE)
    fun `test dove test run for function`() {
        testProject {
            moveToml(
                """
            [package]
            name = "MyPackage"
            """
            )
            tests {
                move(
                    "MoveTests.move", """
            #[test_only]
            module 0x1::MoveTests {
                #[test]
                fun /*caret*/test_add() {
                    1 + 1;
                }
                #[test]
                fun test_mul() {
                    1 * 1;
                }
            }
            """
                )
            }
        }
        checkOnElement<MvFunction>()

        val ctx1 = myFixture.findElementByText("+", PsiElement::class.java)
        val ctx2 = myFixture.findElementByText("*", PsiElement::class.java)
        doTestRemembersContext(TestRunConfigurationProducer(), ctx1, ctx2)
    }

    @SettingsProjectType(ProjectType.DOVE)
    fun `test dove no test run if no test functions`() {
        testProject {
            moveToml("""""")
            sources {
                move("main.move", """
                #[test_only]    
                module 0x1::/*caret*/M {
                    fun call() {}
                }    
                """)
            }
        }
        checkNoConfigurationOnElement<MvModuleDef>()
    }

    @SettingsProjectType(ProjectType.DOVE)
    fun `test dove test run for module with test functions inside sources`() {
        testProject {
            moveToml("""
            [package]
            name = "MyPackage"    
            """)
            sources {
                move("main.move", """
                #[test_only]    
                module 0x1::/*caret*/Main {
                    #[test]
                    fun test_some_action() {}
                }    
                """)
            }
        }
        checkOnElement<MvModuleDef>()
    }
}
//    }

//    @SettingsProjectKind(ProjectKind.DOVE)
//    fun `test producer works for annotated functions directory`() {
//        val testProject = testProjectFromFileTree(
//            """
//        //- code/Move.toml
//        [package]
//        name = "MyPackage"
//        version = "0.1.0"
//        //- code/tests/MoveTests.move
//        #[test_only]
//        module 0x1::MoveTests {
//            #[test]
//            fun /*caret*/test_add() { 1 + 1; }
//            #[test]
//            fun test_mul() { 1 * 1; }
//        }
//        """
//        )
//        myFixture.configureFromFileWithCaret(testProject)
//
//        project.setKind(ProjectKind.DOVE, testRootDisposable)
//
//        val element = myFixture.file
//            .findElementAt(myFixture.caretOffset)
//            ?.ancestorOrSelf<MvFunction>() ?: error("No function at caret position")
//        val dir = element.containingFile.parent as PsiDirectory
//
//        val context = ConfigurationContext(dir)
//        val configurations = context.configurationsFromContext.orEmpty().map { it.configuration }
//
//        val serialized = configurations.map { config ->
//            Element("configuration").apply {
//                setAttribute("name", config.name)
//                setAttribute("class", config.javaClass.simpleName)
//                config.writeExternal(this)
//            }
//        }
//
//        val root = Element("configurations")
//        serialized.forEach { root.addContent(it) }
//
//        val rootPath = testProject.rootDirectory.toNioPath().toString()
//        val rootXml = root.toXmlString().replace(rootPath, "/my-package")
//
//        val testDataPath = "${TestCase.testResourcesPath}/org/move/runconfig/producers.fixtures"
//        assertSameLinesWithFile(
//            "$testDataPath/${getTestName(true)}.xml", rootXml
//        )
//
//        val ctx1 = myFixture.findElementByText("+", PsiElement::class.java)
//        val ctx2 = myFixture.findElementByText("*", PsiElement::class.java)
//        doTestRemembersContext(TestRunConfigurationProducer(), ctx1, ctx2)
//    }
//
//    @SettingsProjectKind(ProjectKind.DOVE)
//    fun `test producer works for annotated functions file`() {
//        val testProject = testProjectFromFileTree(
//            """
//        //- code/Move.toml
//        [package]
//        name = "MyPackage"
//        version = "0.1.0"
//        //- code/tests/MoveTests.move
//        #[test_only]
//        module 0x1::MoveTests {
//            #[test]
//            fun /*caret*/test_add() { 1 + 1; }
//            #[test]
//            fun test_mul() { 1 * 1; }
//        }
//        """
//        )
//        myFixture.configureFromFileWithCaret(testProject)
//
//        val element = myFixture.file
//            .findElementAt(myFixture.caretOffset)
//            ?.ancestorOrSelf<MvModuleDef>() ?: error("No function at caret position")
//        val file = element.containingFile
//        val context = ConfigurationContext(file)
//        val configurations = context.configurationsFromContext.orEmpty().map { it.configuration }
//
//        val serialized = configurations.map { config ->
//            Element("configuration").apply {
//                setAttribute("name", config.name)
//                setAttribute("class", config.javaClass.simpleName)
//                config.writeExternal(this)
//            }
//        }
//
//        val root = Element("configurations")
//        serialized.forEach { root.addContent(it) }
//
//        val rootPath = testProject.rootDirectory.toNioPath().toString()
//        val rootXml = root.toXmlString().replace(rootPath, "/my-package")
//
//        val testDataPath = "${TestCase.testResourcesPath}/org/move/runconfig/producers.fixtures"
//        assertSameLinesWithFile(
//            "$testDataPath/${getTestName(true)}.xml", rootXml
//        )
//
//        val ctx1 = myFixture.findElementByText("+", PsiElement::class.java)
//        val ctx2 = myFixture.findElementByText("*", PsiElement::class.java)
//        doTestRemembersContext(TestRunConfigurationProducer(), ctx1, ctx2)
//    }
//
//    @SettingsProjectKind(ProjectKind.DOVE)
//    fun `test producer works for annotated functions module`() {
//        val testProject = testProjectFromFileTree(
//            """
//        //- code/Move.toml
//        [package]
//        name = "MyPackage"
//        version = "0.1.0"
//        //- code/tests/MoveTests.move
//        #[test_only]
//        module 0x1::MoveTests {
//            #[test]
//            fun /*caret*/test_add() { 1 + 1; }
//            #[test]
//            fun test_mul() { 1 * 1; }
//        }
//        """
//        )
//        myFixture.configureFromFileWithCaret(testProject)
//
//        val element = myFixture.file
//            .findElementAt(myFixture.caretOffset)
//            ?.ancestorOrSelf<MvModuleDef>() ?: error("No function at caret position")
//
//        val context = ConfigurationContext(element)
//        val configurations = context.configurationsFromContext.orEmpty().map { it.configuration }
//
//        val serialized = configurations.map { config ->
//            Element("configuration").apply {
//                setAttribute("name", config.name)
//                setAttribute("class", config.javaClass.simpleName)
//                config.writeExternal(this)
//            }
//        }
//
//        val root = Element("configurations")
//        serialized.forEach { root.addContent(it) }
//
//        val rootPath = testProject.rootDirectory.toNioPath().toString()
//        val rootXml = root.toXmlString().replace(rootPath, "/my-package")
//
//        val testDataPath = "${TestCase.testResourcesPath}/org/move/runconfig/producers.fixtures"
//        assertSameLinesWithFile(
//            "$testDataPath/${getTestName(true)}.xml", rootXml
//        )
//
//        val ctx1 = myFixture.findElementByText("+", PsiElement::class.java)
//        val ctx2 = myFixture.findElementByText("*", PsiElement::class.java)
//        doTestRemembersContext(TestRunConfigurationProducer(), ctx1, ctx2)
//    }
//}
