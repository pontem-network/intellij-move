package org.move.utils.tests

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.impl.BaseFixture
import org.move.cli.settings.endlessCliPath
import org.move.cli.settings.moveSettings
import org.move.stdext.getCliFromPATH
import java.nio.file.Path

class EndlessCliTestFixture(
    // This property is mutable to allow `com.intellij.testFramework.UsefulTestCase.clearDeclaredFields`
    // set null in `tearDown`
    private var project: Project
): BaseFixture() {

    var endlessPath: Path? = null

    override fun setUp() {
        super.setUp()

        var endlessSdkPath = project.endlessCliPath
        if (endlessSdkPath == null) {
            endlessSdkPath = getCliFromPATH("endless")
            project.moveSettings.modifyTemporary(testRootDisposable) {
                it.endlessPath = endlessSdkPath.toString()
            }
        }
        this.endlessPath = endlessSdkPath

        setUpAllowedRoots()
    }

    private fun setUpAllowedRoots() {
        val endlessPath = endlessPath ?: return
        VfsRootAccess.allowRootAccess(testRootDisposable, endlessPath.toString())
        // actions-rs/toolchain on CI creates symlink at `~/.cargo` while setting up of Rust toolchain
        val canonicalEndlessPath = endlessPath.toRealPath()
        if (endlessPath != canonicalEndlessPath) {
            VfsRootAccess.allowRootAccess(testRootDisposable, canonicalEndlessPath.toString())
        }
    }
}