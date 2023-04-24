package org.move.cli.runConfigurations

import com.intellij.execution.actions.ConfigurationContext
import org.move.cli.runConfigurations.aptos.FunctionCallConfigurationBase
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.utils.tests.RunConfigurationProducerTestBase

class FunctionCallConfigurationProducerTest : RunConfigurationProducerTestBase("function_call") {
    fun `test run command for entry function call with no arguments`() {
        testProject {
            _aptos_config_yaml_with_profiles(listOf("default"))
            namedMoveToml("MyPackage")
            sources {
                main(
                    """
                    module 0x1::m {
                        public entry fun /*caret*/main(account: &signer) {}
                    }                    
                """
                )
            }
        }
        checkOnFunction()
    }

    fun `test run command for entry function call with one argument`() {
        testProject {
            _aptos_config_yaml_with_profiles(listOf("default"))
            namedMoveToml("MyPackage")
            sources {
                main(
                    """
                    module 0x1::m {
                        public entry fun /*caret*/main(account: &signer, balance: u64) {}
                    }                    
                """
                )
            }
        }
        checkOnFunction(shouldOpenEditor = true)
    }

    fun `test view command for view function call with no arguments`() {
        testProject {
            _aptos_config_yaml_with_profiles(listOf("default"))
            namedMoveToml("MyPackage")
            sources {
                main(
                    """
                    module 0x1::m {
                        #[view]
                        public fun /*caret*/main() {}
                    }                    
                """
                )
            }
        }
        checkOnFunction()
    }

    fun `test view command for view function call with one argument`() {
        testProject {
            _aptos_config_yaml_with_profiles(listOf("default"))
            namedMoveToml("MyPackage")
            sources {
                main(
                    """
                    module 0x1::m {
                        #[view]
                        public fun /*caret*/main(balance: u64) {}
                    }                    
                """
                )
            }
        }
        checkOnFunction(shouldOpenEditor = true)
    }

    fun `test no configuration on non entry non view function`() {
        testProject {
            _aptos_config_yaml_with_profiles(listOf("default"))
            namedMoveToml("MyPackage")
            sources {
                main(
                    """
                    module 0x1::m {
                        public fun /*caret*/main(balance: u64) {}
                    }                    
                """
                )
            }
        }
        checkNoConfigurationOnElement<MvFunction>()
    }

    private fun checkOnFunction(shouldOpenEditor: Boolean = false) {
        val element = myFixture.file.findElementAt(myFixture.caretOffset)
            ?.ancestorOrSelf<MvFunction>()
            ?: error("Failed to find element of `${MvFunction::class.simpleName}` class at caret")
        val configurationContext = ConfigurationContext(element)

        val runConfiguration = configurationContext
            .configurationsFromContext
            ?.firstOrNull()?.configuration as? FunctionCallConfigurationBase
        if (runConfiguration != null) {
            val actualShouldOpenEditor = runConfiguration.firstRunShouldOpenEditor()
            check(actualShouldOpenEditor == shouldOpenEditor) {
                "Actual editor open: $actualShouldOpenEditor, expected: $shouldOpenEditor"
            }
        }

        check(configurationContext)
    }

}
