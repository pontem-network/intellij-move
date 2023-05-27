package org.move.lang.completion

import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MvQualNamedElement
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

    fun `test correct type const first`() = checkCompletionsOrder(
        listOf("CONST_2u8", "CONST_1u64"), """
    module 0x1::Main {
        const CONST_1u64: u64 = 2;
        const CONST_2u8: u8 = 1;
        fun call() {
            let a: u8 = CO/*caret*/;
        }
    }    
    """
    )

    fun `test resource before non-resource in borrow_global`() = checkCompletionsOrder(
        listOf("Coin", "Cat"),
        """
module 0x1::Main {
    struct Cat {}
    struct Coin<phantom CoinType> has key {}
    fun call() {
        borrow_global<C/*caret*/>(@0x1);
    }
}
        """
    )

    fun `test vector literal inferred type`() = checkCompletionsOrder(
        listOf("liq_nft_1", "liq_nft_2", "liq_nfts"),
        """
            module 0x1::m {
                fun main() {
                    let liq_nfts = vector[1, 2, 3];
                    let liq_nft_1 = 1;
                    let liq_nft_2 = 2;
                    vector[liq_nft_1, liq/*caret*/];
                }
            }
        
        """
    )

    fun checkCompletionsOrder(listStart: List<String>, @Language("Move") code: String) {
        val variants = completionFixture.invokeCompletion(code)
        val lookupStrings = variants.map { it.lookupString }
        checkValidPrefix(listStart, lookupStrings)
    }

    fun checkFqCompletionsOrder(listStart: List<String>, @Language("Move") code: String) {
        val variants = completionFixture.invokeCompletion(code)
        val lookupStrings =
            variants.map { (it.psiElement as? MvQualNamedElement)?.qualName?.editorText() ?: it.lookupString }
        checkValidPrefix(listStart, lookupStrings)
    }

    private fun checkValidPrefix(prefix: List<String>, lookups: List<String>) {
        check(lookups.subList(0, prefix.size) == prefix) {
            "Expected variants \n    $prefix\n ain't a prefix of actual \n    $lookups"
        }
    }
}
