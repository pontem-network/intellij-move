package org.move.lang.completion

import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.types.fqName
import org.move.utils.tests.MoveV2
import org.move.utils.tests.completion.CompletionTestCase

class CompletionPrioritiesTest: CompletionTestCase() {
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

    fun `test field type`() = checkCompletionsOrder(
        listOf("field2", "field1"),
        """
            module 0x1::main {
                struct S { field1: u8, field2: u16 }
                fun get_member1(self: &S): u8 { s.field1 }
                fun get_member2(self: &S): u16 { s.field2 }
                fun main(s: S) {
                    let a: u16 = s.fi/*caret*/
                }                
            }            
        """
    )

    @MoveV2()
    fun `test method return type`() = checkCompletionsOrder(
        listOf("get_member2", "get_member1"),
        """
            module 0x1::main {
                struct S { field1: u8, field2: u16 }
                fun get_member1(self: &S): u8 { s.field1 }
                fun get_member2(self: &S): u16 { s.field2 }
                fun main(s: S) {
                    let a: u16 = s.get_m/*caret*/
                }                
            }            
        """
    )

    @MoveV2()
    fun `test method return type with ref`() = checkCompletionsOrder(
        listOf("borrow", "borrow_with_default", "borrow_buckets"),
        """
            module 0x1::main {
                struct S<T> { field: T }
                fun borrow<T>(self: &S<T>): &T {}
                fun borrow_buckets<T>(self: &S<T>): &vector<T> {}
                fun borrow_with_default<T>(self: &S<T>): &T {}
                fun main<T>(s: S<T>): &T {
                    s.b/*caret*/
                }                
            }            
        """
    )

    fun `test field type with ref`() = checkCompletionsOrder(
        listOf("borrow", "borrow_with_default", "borrow_buckets"),
        """
            module 0x1::main {
                struct S<T> { 
                    borrow: T,
                    borrow_buckets: vector<T>,
                    borrow_with_default: T,
                }
                fun main<T>(s: S<T>): T {
                    s.b/*caret*/
                }                
            }            
        """
    )

    @MoveV2
    fun `test match keyword should be before the main function`() = checkCompletionsOrder(
        listOf("match", "main"),
        """
            module 0x1::mod {
                fun main() {
                    m/*caret*/                    
                }
            }                
        """
    )

    fun `test use binary op types for completion sorting`() = checkCompletionsOrder(
        listOf("def_val_2", "def_val"), """
        module std::modules {
            struct Ss { def_val: u8, def_val_2: u16 }
            fun main(s: Ss) {
                1u16 + s.de/*caret*/;
            }
        }
    """.trimIndent()
    )

    private fun checkCompletionsOrder(listStart: List<String>, @Language("Move") code: String) {
        val variants = completionFixture.invokeCompletion(code)
        val lookupStrings = variants.map { it.lookupString }
        checkCompletionListStartsWith(listStart, lookupStrings)
    }

    private fun checkFqCompletionsOrder(listStart: List<String>, @Language("Move") code: String) {
        val variants = completionFixture.invokeCompletion(code)
        val lookupStrings =
            variants.map { (it.psiElement as? MvNamedElement)?.fqName()?.identifierText() ?: it.lookupString }
        checkCompletionListStartsWith(listStart, lookupStrings)
    }

    private fun checkCompletionListStartsWith(prefix: List<String>, completions: List<String>) {
        check(completions.size >= prefix.size) {
            "Completions list is smaller than expected prefix. \n" +
                    "    expected prefix: $prefix \n" +
                    "    actual completions: $completions"
        }
        check(completions.subList(0, prefix.size) == prefix) {
            "Wrong order of completions. \n" +
                    "    expected prefix: $prefix \n" +
                    "    actual completions: $completions"
        }
    }
}
