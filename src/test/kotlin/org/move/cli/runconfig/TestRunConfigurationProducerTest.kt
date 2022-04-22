package org.move.cli.runconfig

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.jdom.Element
import org.move.cli.runconfig.test.TestRunConfigurationProducer
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.openapiext.toXmlString
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.base.TestCase

class TestRunConfigurationProducerTest : MvProjectTestBase() {
    fun `test test producer works for annotated functions`() {
        val testProject = testProjectFromFileTree(
            """
        //- code/Move.toml
        [package]
        name = "package"
        version = "0.1.0"
        //- code/tests/MoveTests.move
        module 0x1::MoveTests {
            #[test]
            fun /*caret*/test_add() { 1 + 1; }
            #[test]
            fun test_mul() { 1 * 1; }
        }    
        """
        )
        myFixture.configureFromFileWithCaret(testProject)

        val element = myFixture.file
            .findElementAt(myFixture.caretOffset)
            ?.ancestorOrSelf<MvFunction>() ?: error("No function at caret position")

        val context = ConfigurationContext(element)
        val configurations = context.configurationsFromContext.orEmpty().map { it.configuration }

        val serialized = configurations.map { config ->
            Element("configuration").apply {
                setAttribute("name", config.name)
                setAttribute("class", config.javaClass.simpleName)
                config.writeExternal(this)
            }
        }

        val root = Element("configurations")
        serialized.forEach { root.addContent(it) }

        val rootPath = testProject.rootDirectory.toNioPath().toString()
        val rootXml = root.toXmlString().replace(rootPath, "/my-package")

        val testDataPath = "${TestCase.testResourcesPath}/org/move/runconfig/producers.fixtures"
        assertSameLinesWithFile(
            "$testDataPath/${getTestName(true)}.xml", rootXml)

        val ctx1 = myFixture.findElementByText("+", PsiElement::class.java)
        val ctx2 = myFixture.findElementByText("*", PsiElement::class.java)
        doTestRemembersContext(TestRunConfigurationProducer(), ctx1, ctx2)
    }

    fun `test test producer works for annotated functions direcotry`() {
        val testProject = testProjectFromFileTree(
            """
        //- code/Move.toml
        [package]
        name = "package"
        version = "0.1.0"
        //- code/tests/MoveTests.move
        module 0x1::MoveTests {
            #[test]
            fun /*caret*/test_add() { 1 + 1; }
            #[test]
            fun test_mul() { 1 * 1; }
        }    
        """
        )
        myFixture.configureFromFileWithCaret(testProject)

        val element = myFixture.file
            .findElementAt(myFixture.caretOffset)
            ?.ancestorOrSelf<MvFunction>() ?: error("No function at caret position")
        val dir = element.containingFile.parent as PsiDirectory

        val context = ConfigurationContext(dir)
        val configurations = context.configurationsFromContext.orEmpty().map { it.configuration }

        val serialized = configurations.map { config ->
            Element("configuration").apply {
                setAttribute("name", config.name)
                setAttribute("class", config.javaClass.simpleName)
                config.writeExternal(this)
            }
        }

        val root = Element("configurations")
        serialized.forEach { root.addContent(it) }

        val rootPath = testProject.rootDirectory.toNioPath().toString()
        val rootXml = root.toXmlString().replace(rootPath, "/my-package")

        val testDataPath = "${TestCase.testResourcesPath}/org/move/runconfig/producers.fixtures"
        assertSameLinesWithFile(
            "$testDataPath/${getTestName(true)}.xml", rootXml)

        val ctx1 = myFixture.findElementByText("+", PsiElement::class.java)
        val ctx2 = myFixture.findElementByText("*", PsiElement::class.java)
        doTestRemembersContext(TestRunConfigurationProducer(), ctx1, ctx2)
    }

    fun `test test producer works for annotated functions file`() {
        val testProject = testProjectFromFileTree(
            """
        //- code/Move.toml
        [package]
        name = "package"
        version = "0.1.0"
        //- code/tests/MoveTests.move
        module 0x1::MoveTests {
            #[test]
            fun /*caret*/test_add() { 1 + 1; }
            #[test]
            fun test_mul() { 1 * 1; }
        }    
        """
        )
        myFixture.configureFromFileWithCaret(testProject)

        val element = myFixture.file
            .findElementAt(myFixture.caretOffset)
            ?.ancestorOrSelf<MvModuleDef>() ?: error("No function at caret position")
        val file = element.containingFile
        val context = ConfigurationContext(file)
        val configurations = context.configurationsFromContext.orEmpty().map { it.configuration }

        val serialized = configurations.map { config ->
            Element("configuration").apply {
                setAttribute("name", config.name)
                setAttribute("class", config.javaClass.simpleName)
                config.writeExternal(this)
            }
        }

        val root = Element("configurations")
        serialized.forEach { root.addContent(it) }

        val rootPath = testProject.rootDirectory.toNioPath().toString()
        val rootXml = root.toXmlString().replace(rootPath, "/my-package")

        val testDataPath = "${TestCase.testResourcesPath}/org/move/runconfig/producers.fixtures"
        assertSameLinesWithFile(
            "$testDataPath/${getTestName(true)}.xml", rootXml)

        val ctx1 = myFixture.findElementByText("+", PsiElement::class.java)
        val ctx2 = myFixture.findElementByText("*", PsiElement::class.java)
        doTestRemembersContext(TestRunConfigurationProducer(), ctx1, ctx2)
    }

    fun `test test producer works for annotated functions module`() {
        val testProject = testProjectFromFileTree(
            """
        //- code/Move.toml
        [package]
        name = "package"
        version = "0.1.0"
        //- code/tests/MoveTests.move
        module 0x1::MoveTests {
            #[test]
            fun /*caret*/test_add() { 1 + 1; }
            #[test]
            fun test_mul() { 1 * 1; }
        }    
        """
        )
        myFixture.configureFromFileWithCaret(testProject)

        val element = myFixture.file
            .findElementAt(myFixture.caretOffset)
            ?.ancestorOrSelf<MvModuleDef>() ?: error("No function at caret position")

        val context = ConfigurationContext(element)
        val configurations = context.configurationsFromContext.orEmpty().map { it.configuration }

        val serialized = configurations.map { config ->
            Element("configuration").apply {
                setAttribute("name", config.name)
                setAttribute("class", config.javaClass.simpleName)
                config.writeExternal(this)
            }
        }

        val root = Element("configurations")
        serialized.forEach { root.addContent(it) }

        val rootPath = testProject.rootDirectory.toNioPath().toString()
        val rootXml = root.toXmlString().replace(rootPath, "/my-package")

        val testDataPath = "${TestCase.testResourcesPath}/org/move/runconfig/producers.fixtures"
        assertSameLinesWithFile(
            "$testDataPath/${getTestName(true)}.xml", rootXml)

        val ctx1 = myFixture.findElementByText("+", PsiElement::class.java)
        val ctx2 = myFixture.findElementByText("*", PsiElement::class.java)
        doTestRemembersContext(TestRunConfigurationProducer(), ctx1, ctx2)
    }

    protected fun doTestRemembersContext(
        producer: RunConfigurationProducer<MoveRunConfiguration>,
        ctx1: PsiElement,
        ctx2: PsiElement
    ) {
        val contexts = listOf(ConfigurationContext(ctx1), ConfigurationContext(ctx2))
        val configsFromContext = contexts.map { it.configurationsFromContext!!.single() }
        configsFromContext.forEach { check(it.isProducedBy(producer.javaClass)) }
        val configs = configsFromContext.map { it.configuration as MoveRunConfiguration }
        for (i in 0..1) {
            check(producer.isConfigurationFromContext(configs[i], contexts[i])) {
                "Configuration created from context does not believe it"
            }

            check(!producer.isConfigurationFromContext(configs[i], contexts[1 - i])) {
                "Configuration wrongly believes it is from another context"
            }
        }
    }
}
