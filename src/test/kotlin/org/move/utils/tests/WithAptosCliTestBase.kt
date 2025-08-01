package org.move.utils.tests

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.common.runAll
import com.intellij.util.ThrowableRunnable
import org.move.cli.settings.moveSettings
import java.nio.file.Path

abstract class WithAptosCliTestBase(val localAptosPath: Path? = null): MvProjectTestBase() {

    protected lateinit var aptosCliFixture: AptosCliTestFixture

    protected val cargoProjectDirectory: VirtualFile get() = myFixture.findFileInTempDir(".")

    private val earlyTestRootDisposable = Disposer.newDisposable()

    override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
        val aptosPath = aptosCliFixture.aptosPath
        if (aptosPath == null) {
            System.err.println("SKIP \"$name\": Aptos SDK not available")
            return
        }
        super.runTestRunnable(testRunnable)
    }

    override fun setUp() {
        super.setUp()

        if (localAptosPath != null) {
            project.moveSettings.modifyTemporary(testRootDisposable) {
                it.aptosPath = localAptosPath.toString()            }
        }

        aptosCliFixture = AptosCliTestFixture(project)
        aptosCliFixture.setUp()
    }

    override fun tearDown() {
        runAll(
            { Disposer.dispose(earlyTestRootDisposable) },
            { aptosCliFixture.tearDown() },
            { super.tearDown() },
        )
    }

    override fun getTestRootDisposable(): Disposable {
        return myFixture?.testRootDisposable ?: super.getTestRootDisposable()
    }
}