package org.move.cli.runconfig

import org.move.lang.core.psi.MvModule
import org.move.utils.tests.RunConfigurationProducerTestBase

class PublishConfigurationProducerTest : RunConfigurationProducerTestBase("publish") {
    fun `test publish module`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "main.move", """
                module 0x1::/*caret*/Main {}                    
                """
                )
            }
        }
        checkOnElement<MvModule>()
    }

    fun `test no publish action if test_only`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move(
                    "main.move", """
                #[test_only]    
                module 0x1::/*caret*/Main {}                    
                """
                )
            }
        }
        checkNoConfigurationOnElement<MvModule>()
    }


}
