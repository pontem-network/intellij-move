package org.move.utils.tests

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.impl.BaseFixture
import org.move.cli.settings.Blockchain
import org.move.cli.settings.Blockchain.APTOS
import org.move.cli.settings.aptos.AptosExecType.LOCAL
import org.move.cli.settings.moveSettings

class AptosCliTestFixture(
    // This property is mutable to allow `com.intellij.testFramework.UsefulTestCase.clearDeclaredFields`
    // set null in `tearDown`
    private var project: Project
): BaseFixture() {

    val aptosPath = Blockchain.aptosCliFromPATH() ?: error("aptos is not available")

    override fun setUp() {
        super.setUp()

        setUpAllowedRoots()
        project.moveSettings.modifyTemporary(testRootDisposable) {
            it.blockchain = APTOS
            it.aptosExecType = LOCAL
            it.localAptosPath = aptosPath.toString()
        }
    }

    private fun setUpAllowedRoots() {
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