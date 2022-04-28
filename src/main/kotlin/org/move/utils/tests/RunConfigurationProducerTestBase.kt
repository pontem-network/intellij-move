package org.move.utils.tests

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.jdom.Element
import org.move.cli.runconfig.MoveRunConfiguration
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.openapiext.toXmlString
import org.move.utils.tests.base.TestCase

abstract class RunConfigurationProducerTestBase(val testDir: String) : MvProjectTestBase() {
    protected fun checkOnFsItem(fsItem: PsiFileSystemItem) {
        val configurationContext = ConfigurationContext(fsItem)
        check(configurationContext)
    }

    protected fun checkNoConfigurationOnFsItem(fsItem: PsiFileSystemItem) {
        val configurationContext = ConfigurationContext(fsItem)
        val configurations = configurationContext.configurationsFromContext.orEmpty()
        check(configurations.isEmpty()) { "Found unexpected run configurations" }
    }

    protected inline fun <reified T : PsiElement> checkOnElement() {
        val element = myFixture.file.findElementAt(myFixture.caretOffset)
            ?.ancestorOrSelf<T>()
            ?: error("Failed to find element of `${T::class.simpleName}` class at caret")
        val configurationContext = ConfigurationContext(element)
        check(configurationContext)
    }

    protected inline fun <reified T : PsiElement> checkNoConfigurationOnElement() {
        val element = myFixture.file.findElementAt(myFixture.caretOffset)
            ?.ancestorOrSelf<T>()
            ?: error("Failed to find element of `${T::class.simpleName}` class at caret")
        val configurationContext = ConfigurationContext(element)
        val configurations = configurationContext.configurationsFromContext.orEmpty()
        check(configurations.isEmpty()) { "Found unexpected run configurations" }
    }

    protected fun check(configurationContext: ConfigurationContext) {
        val configurations =
            configurationContext.configurationsFromContext.orEmpty().map { it.configurationSettings }
        check(configurations.isNotEmpty()) { "No configurations found" }

        val root = Element("configurations")
        configurations.forEach {
            val confSettings = it as RunnerAndConfigurationSettingsImpl
            val content = confSettings.writeScheme()
            root.addContent(content)
        }
        val testProject = this.testProject ?: error("testProject not initialized")
        val testId = testProject.rootDirectory.name
        val transformedXml = root.toXmlString().replace(testId, "unitTest_ID")

        val testDataPath = "${TestCase.testResourcesPath}/org/move/cli/producers.fixtures/$testDir"
        assertSameLinesWithFile(
            "$testDataPath/${getTestName(true)}.xml", transformedXml
        )
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
