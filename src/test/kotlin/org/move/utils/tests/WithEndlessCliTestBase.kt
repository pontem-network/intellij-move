package org.move.utils.tests

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.common.runAll
import com.intellij.util.ThrowableRunnable
import org.move.cli.settings.moveSettings
import java.nio.file.Path

abstract class WithEndlessCliTestBase(val localEndlessPath: Path? = null): MvProjectTestBase() {

    protected lateinit var endlessCliFixture: EndlessCliTestFixture

    protected val cargoProjectDirectory: VirtualFile get() = myFixture.findFileInTempDir(".")

    private val earlyTestRootDisposable = Disposer.newDisposable()

    override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
        val endlessPath = endlessCliFixture.endlessPath
        if (endlessPath == null) {
            System.err.println("SKIP \"$name\": Endless SDK not available")
            return
        }
        super.runTestRunnable(testRunnable)
    }

    override fun setUp() {
        super.setUp()

        if (localEndlessPath != null) {
            project.moveSettings.modifyTemporary(testRootDisposable) {
                it.endlessPath = localEndlessPath.toString()            }
        }

        endlessCliFixture = EndlessCliTestFixture(project)
        endlessCliFixture.setUp()
    }

    override fun tearDown() {
        runAll(
            { Disposer.dispose(earlyTestRootDisposable) },
            { endlessCliFixture.tearDown() },
            { super.tearDown() },
        )
    }

    override fun getTestRootDisposable(): Disposable {
        return myFixture?.testRootDisposable ?: super.getTestRootDisposable()
    }
}