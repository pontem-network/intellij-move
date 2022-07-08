package org.move.lang.completion

import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MvNamedElement
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

    fun `test type before module in type position`() = checkFqCompletionsOrder(
        listOf("0x1::Coin::Coin", "0x1::Coin"),
        """
module 0x1::Coin {
    struct Coin<CoinType> {}
}            
module 0x1::Main {
    fun call(m: Coi/*caret*/) {}
}
        """
    )

    fun `test function returning correct type first in let`() = checkCompletionsOrder(
        listOf("get_b", "get_a"),
        """
module 0x1::Main {
    fun get_a(): u8 {}
    fun get_b(): bool {}
    fun main() {
        let a: bool = get/*caret*/;
    }
}            
        """
    )

    fun `test function returning correct type first in function param`() = checkCompletionsOrder(
        listOf("get_b", "get_a"),
        """
module 0x1::Main {
    fun get_a(): u8 {}
    fun get_b(): bool {}
    fun call(a: bool) {}
    fun main() {
        call(get/*caret*/)
    }
}            
        """
    )

    fun `test return before local items`() = checkCompletionsOrder(
        listOf("return", "return_local"),
        """
            module 0x1::Main {
                fun return_local() {}
                fun call() {
                    ret/*caret*/
                }
            }    
        """
    )

//    fun `test resource before non-resource in borrow_global`() = checkCompletionsOrder(
//        listOf("Coin", "Cat"),
//        """
//module 0x1::Main {
//    struct Cat {}
//    struct Coin<CoinType> has key {}
//    fun call() {
//        borrow_global<C/*caret*/>(@0x1);
//    }
//}
//        """
//    )

    fun checkCompletionsOrder(listStart: List<String>, @Language("Move") code: String) {
        val variants = completionFixture.invokeCompletion(code)
        val lookupStrings = variants.map { it.lookupString }
        checkValidPrefix(listStart, lookupStrings)
    }

    fun checkFqCompletionsOrder(listStart: List<String>, @Language("Move") code: String) {
        val variants = completionFixture.invokeCompletion(code)
        val lookupStrings = variants.map { (it.psiElement as? MvNamedElement)?.fqName ?: it.lookupString }
        checkValidPrefix(listStart, lookupStrings)
    }

    private fun checkValidPrefix(prefix: List<String>, lookups: List<String>) {
        check(lookups.subList(0, prefix.size) == prefix) {
            "Expected variants \n    $prefix\n ain't a prefix of actual \n    $lookups"
        }
    }
}
