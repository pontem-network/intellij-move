package org.move.cli.runConfigurations

import com.intellij.util.execution.ParametersListUtil
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.move.cli.runConfigurations.test.AptosTestRunState
import org.move.utils.tests.MvTestBase
import java.nio.file.Path
import java.nio.file.Paths

@RunWith(Parameterized::class)
class AptosTestRunStatePatchArgsTest(
    private val input: String,
    private val expected: String
): MvTestBase() {
    private val wd: Path = Paths.get("/my-crate")

    @Test
    fun test() = Assert.assertEquals(
        ParametersListUtil.parse(expected),
        AptosTestRunState.patchArgs(
            AptosCommandLine("move test", ParametersListUtil.parse(input), wd)
        )
    )

    companion object {
        @Parameterized.Parameters(name = "{index}: {0}")
        @JvmStatic
        fun data(): Collection<Array<String>> = listOf(
            arrayOf("", "--format-json"),
            arrayOf("foo", "foo --format-json"),
            arrayOf("--filter test_name --format-json", "--filter test_name --format-json"),

//            arrayOf("", "--no-fail-fast -- --format=json -Z unstable-options --show-output"),
//            arrayOf("foo", "foo --no-fail-fast -- --format=json -Z unstable-options --show-output"),
//            arrayOf("foo bar", "foo bar --no-fail-fast -- --format=json -Z unstable-options --show-output"),
//            arrayOf("--", "--no-fail-fast -- --format=json -Z unstable-options --show-output"),
//
//            arrayOf("-- -Z unstable-options", "--no-fail-fast -- --format=json -Z unstable-options --show-output"),
//            arrayOf("-- --format=json", "--no-fail-fast -- --format=json -Z unstable-options --show-output"),
//            arrayOf("-- --format json", "--no-fail-fast -- --format json -Z unstable-options --show-output"),
//            arrayOf("-- --format pretty", "--no-fail-fast -- --format json -Z unstable-options --show-output"),
//            arrayOf("-- --format=pretty", "--no-fail-fast -- --format=json -Z unstable-options --show-output")
        )
    }
}