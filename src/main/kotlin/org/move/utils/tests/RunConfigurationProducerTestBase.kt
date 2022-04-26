package org.move.utils.tests

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.psi.PsiElement
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.TestDataProvider
import org.jdom.Element
import org.move.cli.runconfig.MoveRunConfiguration
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.openapiext.toXmlString
import org.move.utils.tests.base.TestCase

abstract class RunConfigurationProducerTestBase(val testDir: String) : MvProjectTestBase() {
//    protected fun <T : PsiElement> doTestFromFileTree(
//        psiClass: Class<T>,
//        builder: FileTreeBuilder.() -> Unit
//    ) {
//        val testProject = testProj(builder)
//
//        val element = myFixture.file
//            .findElementAt(myFixture.caretOffset)
//            ?.ancestorOfClass(psiClass) ?: error("No element at caret position")
//
//        val configurationContext = ConfigurationContext(element)
//        val configurations =
//            configurationContext.configurationsFromContext.orEmpty().map { it.configuration }
//
//        val testDataPath = "${TestCase.testResourcesPath}/org/move/cli/producers.fixtures/$testDir"
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
//        assertSameLinesWithFile(
//            "$testDataPath/${getTestName(true)}.xml", rootXml
//        )
//    }

    protected fun checkOnLeaf() = checkOnElement<PsiElement>()

    protected inline fun <reified T : PsiElement> checkOnTopLevel() {
        checkOnElement<T>()
        checkOnElement<PsiElement>()
    }

    protected fun checkOnFiles(vararg files: PsiElement) {
        TestApplicationManager.getInstance().setDataProvider(object : TestDataProvider(project) {
            override fun getData(dataId: String): Any? =
                if (LangDataKeys.PSI_ELEMENT_ARRAY.`is`(dataId)) files else super.getData(dataId)
        }, testRootDisposable)

        val dataContext = DataManager.getInstance().getDataContext(myFixture.editor.component)
        val configurationContext = ConfigurationContext.getFromContext(dataContext, ActionPlaces.UNKNOWN)
        check(configurationContext)
    }

    protected inline fun <reified T : PsiElement> checkOnElement() {
        val element = myFixture.file.findElementAt(myFixture.caretOffset)
            ?.ancestorOrSelf<T>()
            ?: error("Failed to find element of `${T::class.simpleName}` class at caret")
        val configurationContext = ConfigurationContext(element)
        check(configurationContext)
    }

    protected fun check(configurationContext: ConfigurationContext) {
        val configurations =
            configurationContext.configurationsFromContext.orEmpty().map { it.configurationSettings }

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
