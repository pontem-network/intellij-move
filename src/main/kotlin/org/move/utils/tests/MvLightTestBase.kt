package org.move.utils.tests

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.move.cli.settings.moveSettings

abstract class MvLightTestBase: BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()

        val isDebugMode = this.findAnnotationInstance<DebugMode>()?.enabled ?: true
        setRegistryKey("org.move.debug.enabled", isDebugMode)

        this.handleCompilerV2Annotations(project)
        this.handleNamedAddressAnnotations(project)
    }
}