package org.move.utils.tests

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.impl.BaseFixture
import org.move.cli.settings.aptosCliPath
import org.move.cli.settings.moveSettings
import org.move.stdext.getCliFromPATH
import java.nio.file.Path

class AptosCliTestFixture(
    // This property is mutable to allow `com.intellij.testFramework.UsefulTestCase.clearDeclaredFields`
    // set null in `tearDown`
    private var project: Project
): BaseFixture() {

    var aptosPath: Path? = null

    override fun setUp() {
        super.setUp()

        var aptosSdkPath = project.aptosCliPath
        if (aptosSdkPath == null) {
            aptosSdkPath = getCliFromPATH("aptos")
            project.moveSettings.modifyTemporary(testRootDisposable) {
                it.aptosPath = aptosSdkPath.toString()
            }
        }
        this.aptosPath = aptosSdkPath

        setUpAllowedRoots()
    }

    private fun setUpAllowedRoots() {
        val aptosPath = aptosPath ?: return
        VfsRootAccess.allowRootAccess(testRootDisposable, aptosPath.toString())
        // actions-rs/toolchain on CI creates symlink at `~/.cargo` while setting up of Rust toolchain
        val canonicalCargoPath = aptosPath.toRealPath()
        if (aptosPath != canonicalCargoPath) {
            VfsRootAccess.allowRootAccess(testRootDisposable, canonicalCargoPath.toString())
        }
    }
}