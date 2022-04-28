package org.move.cli.runconfig

import org.move.cli.settings.ProjectType
import org.move.lang.core.psi.MvModuleDef
import org.move.utils.tests.RunConfigurationProducerTestBase
import org.move.utils.tests.SettingsPrivateKey
import org.move.utils.tests.SettingsProjectType

class PublishConfigurationProducerTest: RunConfigurationProducerTestBase("publish") {
    @SettingsProjectType(ProjectType.APTOS)
    @SettingsPrivateKey("@0x1122")
    fun `test aptos publish module`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move("main.move", """
                module 0x1::/*caret*/Main {}                    
                """)
            }
        }
        checkOnElement<MvModuleDef>()
    }

    @SettingsProjectType(ProjectType.APTOS)
    fun `test aptos no publish action if test_only`() {
        testProject {
            namedMoveToml("MyPackage")
            sources {
                move("main.move", """
                #[test_only]    
                module 0x1::/*caret*/Main {}                    
                """)
            }
        }
        checkNoConfigurationOnElement<MvModuleDef>()
    }


}
