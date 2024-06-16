package org.move.utils.tests

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.move.cli.settings.moveSettings

abstract class MvLightTestBase: BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()

        val isDebugMode = this.findAnnotationInstance<DebugMode>()?.enabled ?: true
        setRegistryKey("org.move.debug.enabled", isDebugMode)

        val isResourceAccess = this.findAnnotationInstance<EnableResourceAccessControl>() != null
        project.moveSettings.modifyTemporary(testRootDisposable) {
            it.enableResourceAccessControl = isResourceAccess
        }
    }
}