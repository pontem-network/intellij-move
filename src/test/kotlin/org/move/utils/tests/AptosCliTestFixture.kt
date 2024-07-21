package org.move.utils.tests

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.impl.BaseFixture
import org.move.cli.settings.aptos.AptosExecType.LOCAL
import org.move.cli.settings.aptosExecPath
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

        var aptosSdkPath = project.aptosExecPath
        if (aptosSdkPath == null) {
            aptosSdkPath = getCliFromPATH("aptos")
            project.moveSettings.modifyTemporary(testRootDisposable) {
                it.aptosExecType = LOCAL
                it.localAptosPath = aptosSdkPath.toString()
            }
        }
        this.aptosPath = aptosSdkPath

        setUpAllowedRoots()
    }

    private fun setUpAllowedRoots() {
        val aptosPath = aptosPath ?: return
//        stdlib?.let { VfsRootAccess.allowRootAccess(testRootDisposable, it.path) }

//        val toolchain = toolchain!!
//        val cargoPath = (EnvironmentUtil.getValue("CARGO_HOME") ?: "~/.cargo")
//            .let { toolchain.expandUserHome(it) }
//            .let { toolchain.toLocalPath(it) }
//            .toPath()

        VfsRootAccess.allowRootAccess(testRootDisposable, aptosPath.toString())
        // actions-rs/toolchain on CI creates symlink at `~/.cargo` while setting up of Rust toolchain
        val canonicalCargoPath = aptosPath.toRealPath()
        if (aptosPath != canonicalCargoPath) {
            VfsRootAccess.allowRootAccess(testRootDisposable, canonicalCargoPath.toString())
        }
//        VfsRootAccess.allowRootAccess(testRootDisposable, RsPathManager.stdlibDependenciesDir().toString())
    }
}