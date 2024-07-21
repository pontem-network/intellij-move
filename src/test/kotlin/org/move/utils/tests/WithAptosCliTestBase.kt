package org.move.utils.tests

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.common.runAll
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.UIUtil

abstract class WithAptosCliTestBase: MvProjectTestBase() {

    protected lateinit var rustupFixture: AptosCliTestFixture

    protected val cargoProjectDirectory: VirtualFile get() = myFixture.findFileInTempDir(".")

    private val earlyTestRootDisposable = Disposer.newDisposable()

//    protected fun FileTree.create(): TestProject =
//        create(project, cargoProjectDirectory)
//            .apply {
////            rustupFixture.toolchain
////                ?.rustc()
////                ?.getStdlibPathFromSysroot(cargoProjectDirectory.pathAsPath)
////                ?.let { VfsRootAccess.allowRootAccess(testRootDisposable, it) }
//
////                refreshWorkspace()
//            }

//    protected fun refreshWorkspace() {
//        project.moveProjectsService.scheduleProjectsRefresh("from test")
////        project.testCargoProjects.discoverAndRefreshSync()
//    }

    override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
        val aptosPath = rustupFixture.aptosPath
        if (aptosPath == null) {
            System.err.println("SKIP \"$name\": Aptos SDK not available")
            return
        }

//        val reason = checkRustcVersionRequirements {
//            val rustcVersion = rustupFixture.toolchain!!.rustc().queryVersion()?.semver
//            if (rustcVersion != null) RsResult.Ok(rustcVersion) else RsResult.Err("\"$name\": failed to query Rust version")
//        }
//        if (reason != null) {
//            System.err.println("SKIP $reason")
//            return
//        }
        super.runTestRunnable(testRunnable)
    }

    override fun setUp() {
        super.setUp()
        rustupFixture = AptosCliTestFixture(project)
        rustupFixture.setUp()
    }

    override fun tearDown() {
        runAll(
            {
                // Fixes flaky tests
                (ProjectLevelVcsManagerEx.getInstance(project) as ProjectLevelVcsManagerImpl).waitForInitialized()
            },
            { Disposer.dispose(earlyTestRootDisposable) },
            { rustupFixture.tearDown() },
            { super.tearDown() },
        )
    }

    override fun getTestRootDisposable(): Disposable {
        return if (myFixture != null) myFixture.testRootDisposable else super.getTestRootDisposable()
    }

//    protected fun buildProject(builder: FileTreeBuilder.() -> Unit): TestProject =
//        fileTree { builder() }.create()

    /**
     * Tries to launches [action]. If it returns `false`, invokes [UIUtil.dispatchAllInvocationEvents] and tries again
     *
     * Can be used to wait file system refresh, for example
     */
//    protected fun runWithInvocationEventsDispatching(
//        errorMessage: String = "Failed to invoke `action` successfully",
//        retries: Int = 1000,
//        action: () -> Boolean
//    ) {
//        repeat(retries) {
//            UIUtil.dispatchAllInvocationEvents()
//            if (action()) {
//                return
//            }
//            Thread.sleep(10)
//        }
//        error(errorMessage)
//    }
}