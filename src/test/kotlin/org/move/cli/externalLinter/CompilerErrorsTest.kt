package org.move.cli.externalLinter

import org.move.ide.annotator.AptosCompilerMessage
import org.move.utils.tests.MvTestBase

class CompilerErrorsTest: MvTestBase() {
    fun `test parse compiler output no errors`() = doTest(
        """
Warning: compiler version `2.0-unstable` is experimental and should not be used in production
Warning: language version `2.0-unstable` is experimental and should not be used in production
Compiling, may take a little while to download git dependencies...
UPDATING GIT DEPENDENCY https://github.com/aptos-labs/aptos-core.git
INCLUDING DEPENDENCY AptosFramework
INCLUDING DEPENDENCY AptosStdlib
INCLUDING DEPENDENCY MoveStdlib
BUILDING move-test-location-example
{
  "Error": "Move compilation failed: exiting with checking errors"
}
    """, listOf()
    )

    fun `test parse compiler output 1`() = doTest(
        """
Warning: compiler version `2.0-unstable` is experimental and should not be used in production
Warning: language version `2.0-unstable` is experimental and should not be used in production
Compiling, may take a little while to download git dependencies...
UPDATING GIT DEPENDENCY https://github.com/aptos-labs/aptos-core.git
INCLUDING DEPENDENCY AptosFramework
INCLUDING DEPENDENCY AptosStdlib
INCLUDING DEPENDENCY MoveStdlib
BUILDING move-test-location-example
error: no function named `match` found
  ┌─ /home/mkurnikov/main/sources/main.move:6:9
  │
6 │         match (self);
  │         ^^^^^^^^^^^^

{
  "Error": "Move compilation failed: exiting with checking errors"
}        
    """, listOf(
            AptosCompilerMessage.forTest(
                message = "no function named `match` found",
                severityLevel = "error",
                filename = "/home/mkurnikov/main/sources/main.move",
                location = "[(6, 9), (6, 21)]"
            )
        )
    )

    fun `test parse compiler output 2`() = doTest(
        """
Warning: compiler version `2.0-unstable` is experimental and should not be used in production
Warning: language version `2.0-unstable` is experimental and should not be used in production
Compiling, may take a little while to download git dependencies...
INCLUDING DEPENDENCY AptosFramework
INCLUDING DEPENDENCY AptosStdlib
INCLUDING DEPENDENCY MoveStdlib
BUILDING move-test-location-example
error: missing acquires annotation for `S`
┌─ /home/mkurnikov/main/sources/main.move:8:9
│
8 │     fun main(s: S) {
│         ^^^^
9 │         s.receive();
│         ----------- acquired by the call to `receive`

{
"Error": "Move compilation failed: exiting with checking errors"
}
    """, listOf(
            AptosCompilerMessage.forTest(
                "missing acquires annotation for `S`",
                "error",
                "/home/mkurnikov/main/sources/main.move",
                "[(8, 9), (8, 13)]"
            )
        )
    )

    fun `test parse compiler output two errors`() = doTest(
        """
Warning: compiler version `2.0-unstable` is experimental and should not be used in production
Warning: language version `2.0-unstable` is experimental and should not be used in production
Compiling, may take a little while to download git dependencies...
INCLUDING DEPENDENCY AptosFramework
INCLUDING DEPENDENCY AptosStdlib
INCLUDING DEPENDENCY MoveStdlib
BUILDING move-test-location-example
error: missing acquires annotation for `S`
  ┌─ /home/mkurnikov/main/sources/main.move:8:9
  │
8 │     fun main(s: S) {
  │         ^^^^
9 │         s.receive();
  │         ----------- acquired by the call to `receive`

error: missing acquires annotation for `S`
  ┌─ /home/mkurnikov/main/sources/main2.move:8:9
  │
8 │     fun main(s: S) {
  │         ^^^^
9 │         s.receive();
  │         ----------- acquired by the call to `receive`

{
  "Error": "Move compilation failed: exiting with checking errors"
}        
    """, listOf(
            AptosCompilerMessage.forTest(
                "missing acquires annotation for `S`",
                "error",
                filename = "/home/mkurnikov/main/sources/main.move",
                location = "[(8, 9), (8, 13)]"
            ),
            AptosCompilerMessage.forTest(
                "missing acquires annotation for `S`",
                "error",
                filename = "/home/mkurnikov/main/sources/main2.move",
                location = "[(8, 9), (8, 13)]"
            ),
        )
    )

    fun `test type error`() = doTest(
        """
Compiling, may take a little while to download git dependencies...
INCLUDING DEPENDENCY AptosFramework
INCLUDING DEPENDENCY AptosStdlib
INCLUDING DEPENDENCY MoveStdlib
BUILDING move-test-location-example
error[E04007]: incompatible types
  ┌─ /home/mkurnikov/main/sources/main2.move:4:13
  │
4 │         1u8 ++ 1u32;
  │         --- ^^ ---- Found: 'u32'. It is not compatible with the other type.
  │         │   │  
  │         │   Incompatible arguments to '+'
  │         Found: 'u8'. It is not compatible with the other type.
}        
    """, listOf(
            AptosCompilerMessage.forTest(
                message = "[E04007] incompatible types",
                severityLevel = "error",
                filename = "/home/mkurnikov/main/sources/main2.move",
                location = "[(4, 13), (4, 15)]"
            ),
        )
    )

    fun `test type error 2`() = doTest(
        """
error[E04007]: incompatible types
  ┌─ /tmp/main/sources/main.move:2:22
  │
2 │     fun main() { 1u32+2u8
  │                  ----^--- Found: 'u8'. It is not compatible with the other type.
  │                  │   │ 
  │                  │   Incompatible arguments to '+'
  │                  Found: 'u32'. It is not compatible with the other type.

{
  "Error": "Move compilation failed: Compilation error"
}
    """, listOf(
            AptosCompilerMessage.forTest(
                message = "[E04007] incompatible types",
                severityLevel = "error",
                filename = "/tmp/main/sources/main.move",
                location = "[(2, 22), (2, 23)]"
            ),
        )
    )

    private fun doTest(compilerOutput: String, expectedMessages: List<AptosCompilerMessage>) {
        val messages = parseCompilerErrors(compilerOutput.trimIndent().lines())

        val messageTestStrings = messages.map { it.toTestString() }
        val expectedTestStrings = expectedMessages.map { it.toTestString() }

        check(messageTestStrings == expectedTestStrings) {
            "Compiler messages are invalid. " +
                    "\nExpected: $expectedTestStrings \n  Actual: $messageTestStrings"
        }
    }
}