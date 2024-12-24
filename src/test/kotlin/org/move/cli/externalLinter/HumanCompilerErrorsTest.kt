package org.move.cli.externalLinter

import org.move.ide.annotator.externalLinter.HumanAptosCompilerError
import org.move.ide.annotator.externalLinter.parseHumanCompilerErrors
import org.move.utils.tests.MvTestBase

class HumanCompilerErrorsTest: MvTestBase() {
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
            HumanAptosCompilerError.forTest(
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
            HumanAptosCompilerError.forTest(
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
            HumanAptosCompilerError.forTest(
                "missing acquires annotation for `S`",
                "error",
                filename = "/home/mkurnikov/main/sources/main.move",
                location = "[(8, 9), (8, 13)]"
            ),
            HumanAptosCompilerError.forTest(
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
            HumanAptosCompilerError.forTest(
                message = "incompatible types",
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
            HumanAptosCompilerError.forTest(
                message = "incompatible types",
                severityLevel = "error",
                filename = "/tmp/main/sources/main.move",
                location = "[(2, 22), (2, 23)]"
            ),
        )
    )

    fun `test ability not satisfied`() = doTest("""
Compiling, may take a little while to download git dependencies...
UPDATING GIT DEPENDENCY https://github.com/aptos-labs/aptos-core.git
INCLUDING DEPENDENCY AptosFramework
INCLUDING DEPENDENCY AptosStdlib
INCLUDING DEPENDENCY MoveStdlib
BUILDING move-test-location-example
error[E05001]: ability constraint not satisfied
  ┌─ /tmp/main/sources/main.move:5:9
  │
2 │     struct S has key { field: u8 }
  │            - To satisfy the constraint, the 'drop' ability would need to be added here
3 │ 
4 │     fun test_assert_false(s: S) {
  │                              - The type '0x1::main2::S' does not have the ability 'drop'
5 │         s;
  │         ^ Cannot ignore values without the 'drop' ability. The value must be used

{
  "Error": "Move compilation failed: Compilation error"
}
    """, listOf(
        HumanAptosCompilerError.forTest(
            message = "ability constraint not satisfied",
            severityLevel = "error",
            filename = "/tmp/main/sources/main.move",
            location = "[(5, 9), (5, 10)]"
        )
    )
    )

    fun `test ability not satisfied compiler v2`() = doTest("""
Warning: compiler version `2.0-unstable` is experimental and should not be used in production
Compiling, may take a little while to download git dependencies...
UPDATING GIT DEPENDENCY https://github.com/aptos-labs/aptos-core.git
INCLUDING DEPENDENCY AptosFramework
INCLUDING DEPENDENCY AptosStdlib
INCLUDING DEPENDENCY MoveStdlib
BUILDING move-test-location-example
error: value of type `main2::S` does not have the `drop` ability
  ┌─ /tmp/main/sources/main2.move:5:9
  │
5 │         s;
  │         ^ implicitly dropped here since it is no longer used

{
  "Error": "Move compilation failed: exiting with stackless-bytecode analysis errors"
}
    """, listOf(
        HumanAptosCompilerError.forTest(
            message = "value of type `main2::S` does not have the `drop` ability",
            severityLevel = "error",
            filename = "/tmp/main/sources/main2.move",
            location = "[(5, 9), (5, 10)]"
        )
    )
    )

    fun `test too many params`() = doTest("""
Warning: compiler version `2.0-unstable` is experimental and should not be used in production
Warning: language version `2.0-unstable` is experimental and should not be used in production
Compiling, may take a little while to download git dependencies...
INCLUDING DEPENDENCY AptosFramework
INCLUDING DEPENDENCY AptosStdlib
INCLUDING DEPENDENCY MoveStdlib
BUILDING move-test-location-example
error: the function takes 0 arguments but 2 were provided
  ┌─ /home/mkurnikov/code/move-test-location-example/sources/main2.move:7:9
  │
7 │         test_assert_false(1, 2);
  │         ^^^^^^^^^^^^^^^^^^^^^^^

{
  "Error": "Move compilation failed: exiting with checking errors"
}
    """, listOf(
        HumanAptosCompilerError.forTest(
            message = "the function takes 0 arguments but 2 were provided",
            severityLevel = "error",
            filename = "/home/mkurnikov/code/move-test-location-example/sources/main2.move",
            location = "[(7, 9), (7, 32)]"
        )
    )
    )

    private fun doTest(compilerOutput: String, expectedMessages: List<HumanAptosCompilerError>) {
        val messages = parseHumanCompilerErrors(compilerOutput.trimIndent().lines())

        val messageTestStrings = messages.map { it.toTestString() }
        val expectedTestStrings = expectedMessages.map { it.toTestString() }

        check(messageTestStrings == expectedTestStrings) {
            "Compiler messages are invalid. " +
                    "\nExpected: $expectedTestStrings \n  Actual: $messageTestStrings"
        }
    }
}