package org.move.lang.completion

import org.intellij.lang.annotations.Language
import org.move.utils.tests.completion.CompletionTestCase

class CompletionPrioritiesTest : CompletionTestCase() {
    fun `test local before builtin before unimported`() = checkCompletionsOrder(
        listOf("borrow_local", "borrow_global", "borrow_global_mut", "borrow"),
        """
module 0x1::M {
    public fun borrow() {}
}            
module 0x1::Main {
    fun borrow_local() {}
    fun call() {
        bor/*caret*/
    }
}        
    """
    )

    fun `test assert! before unimported assert`() = checkCompletionsOrder(
        listOf("assert!", "assert_true"),
        """
module 0x1::M {
    public fun assert_true() {}
}            
module 0x1::Main {
    fun call() {
        ass/*caret*/
    }
}
        """
    )

    fun `test unimported module before unimported type for expr positions`() = doFirstCompletion(
        """
module 0x1::Coin {
    struct Coin {}
}            
module 0x1::Main {
    fun call() {
        Coi/*caret*/
    }
}        
    """, """
module 0x1::Coin {
    struct Coin {}
}            
module 0x1::Main {
    use 0x1::Coin;

    fun call() {
        Coin/*caret*/
    }
}        
    """,
    )

    fun `test unimported type before unimported module for type positions`() = doFirstCompletion(
        """
module 0x1::Coin {
    struct Coin {}
}            
module 0x1::Main {
    fun call(m: Co/*caret*/) {}
}        
    """, """
module 0x1::Coin {
    struct Coin {}
}            
module 0x1::Main {
    use 0x1::Coin::Coin;

    fun call(m: Coin</*caret*/>) {}
}        
    """,
    )

    fun checkCompletionsOrder(listStart: List<String>, @Language("Move") code: String) {
        val variants = completionFixture.invokeCompletion(code)
        val lookupStrings = variants.map { it.lookupString }
        check(lookupStrings.subList(0, listStart.size) == listStart) {
            "Expected variants \n    $listStart\n ain't a prefix of actual \n    $lookupStrings"
        }
    }
}
