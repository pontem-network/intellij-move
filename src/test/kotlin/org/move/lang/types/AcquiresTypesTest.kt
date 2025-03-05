package org.move.lang.types

import org.intellij.lang.annotations.Language
import org.move.ide.inspections.AcquiresTypeContext
import org.move.ide.inspections.GetFunctionAcquiresTypes
import org.move.ide.presentation.fullnameNoArgs
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.types.infer.inference
import org.move.utils.getResults
import org.move.utils.tests.InlineFile
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.base.findElementInEditor

class AcquiresTypesTest: MvTestBase() {
    fun `test no acquired types on function without storage access`() = testFunction(
        """
            module 0x1::m {
                fun main() {}
                   //^
            }          
        """,
        emptyList()
    )

    fun `test one type`() = testFunction(
        """
            module 0x1::m {
                struct S has key {}
                fun main() acquires S {
                   //^
                    borrow_global<S>(@0x1);
                }
            }          
        """,
        listOf("0x1::m::S")
    )

    fun `test one type inline function`() = testFunction(
        """
            module 0x1::m {
                struct S has key {}
                inline fun main() {
                         //^
                    borrow_global<S>(@0x1);
                }
            }          
        """,
        listOf("0x1::m::S")
    )

    fun `test one type nested inline function`() = testFunction(
        """
            module 0x1::m {
                struct S has key {}
                inline fun main() {
                         //^
                    borrow();
                }
                inline fun borrow() {
                    borrow_global<S>(@0x1);
                }
            }          
        """,
        listOf("0x1::m::S")
    )

    fun `test three types`() = testFunction(
        """
            module 0x1::m {
                struct S has key {}
                struct T has key {}
                struct U has key {}
                inline fun main() {
                         //^
                    borrow_global<S>(@0x1);
                    borrow_global<T>(@0x1);
                    borrow_global<U>(@0x1);
                }
            }          
        """,
        listOf("0x1::m::S", "0x1::m::T", "0x1::m::U")
    )

    fun `test type acquired inside nested inline function`() = testFunction(
        """
            module 0x1::m {
                struct S has key {}
                inline fun main() {
                          //^
                   inner();
                }
                inline fun inner() {
                    borrow();
                }
                inline fun borrow() {
                    borrow_global<S>(@0x1);
                }
            }          
        """,
        listOf("0x1::m::S")
    )

    fun `test borrow_global call expr`() = testCallExpr(
        """
        module 0x1::m {
            struct S has key {}
            fun main() {
                borrow_global<S>(@0x1);
                //^ 
            }
        }        
    """,
        listOf("0x1::m::S")
    )

    fun `test function call expr`() = testCallExpr(
        """
        module 0x1::m {
            struct S has key {}
            fun main() {
                call();
                //^ 
            }
            fun call() acquires S {
                borrow_global<S>(@0x1);
            }
        }        
    """,
        listOf("0x1::m::S")
    )

    fun `test inline function call expr`() = testCallExpr(
        """
        module 0x1::m {
            struct S has key {}
            fun main() {
                call();
                //^ 
            }
            inline fun call() {
                borrow_global<S>(@0x1);
            }
        }        
    """,
        listOf("0x1::m::S")
    )

    fun `test function with inline function has empty acquires`() = testFunction(
        """
        module 0x1::m {
            struct S has key {}
            fun main() {
                //^
                call();
            }
            inline fun call() {
                borrow_global<S>(@0x1);
            }
        }        
    """,
        listOf()
    )

    fun `test function has explicit acquires defined`() = testFunction(
        """
        module 0x1::m {
            struct S has key {}
            fun main() acquires S {
                //^
                call();
            }
            inline fun call() {
                borrow_global<S>(@0x1);
            }
        }        
    """,
        listOf("0x1::m::S")
    )

    fun `test nested inline function call expr`() = testCallExpr(
        """
        module 0x1::m {
            struct S has key {}
            fun main() {
                call();
                //^ 
            }
            inline fun call() {
                borrow();
            }
            inline fun borrow() {
                borrow_global<S>(@0x1);
            }
        }        
    """,
        listOf("0x1::m::S")
    )

    fun `test parametrized inline function call expr`() = testCallExpr(
        """
        module 0x1::m {
            struct S has key {}
            struct T has key {}
            struct U has key {}
            fun main() {
                call();
                //^
            }
            inline fun call() {
                borrow<S>();
                borrow<T>();
                borrow<U>();
            }
            inline fun borrow<Element>() {
                borrow_global<Element>(@0x1);
            }
        }        
    """,
        listOf("0x1::m::S", "0x1::m::T", "0x1::m::U")
    )

    fun `test inline acquires with a different module item`() = testCallExpr(
        """
        module 0x1::item {
            struct Item has key {}
        }
        module 0x1::main {
            use 0x1::item::Item;
            fun main() {
                get_item<Item>();
                //^ 
            }
            inline fun get_item<Element>() {
                borrow_global<Element>(@0x1);
            } 
        }                
    """, listOf("0x1::item::Item")
    )

    fun `test inline acquires with generic`() = testFunction(
        """
        module 0x1::item {
            struct Item has key {}
        }
        module 0x1::main {
            use 0x1::item::Item;
            inline fun get_item<Element>() {
                      //^
                borrow_global<Element>(@0x1);
            } 
        }                
    """, listOf("Element")
    )

    // TODO: test
//    fun `test recursive inline function`() = testFunction("""
//        module 0x1::main {
//            inline fun get_item<Element>(i: u8, addr: address) {
//                       //^
//                borrow_global<Element>(addr);
//                if (i != 0) {
//                    get_item<Element>(i - 1, addr);
//                }
//            }
//        }
//    """, listOf("Element"))

    // TODO: test
//    fun `test transitively recursive inline function`() = testFunction("""
//        module 0x1::main {
//            inline fun get_item<Element>(i: u8, addr: address) {
//                       //^
//                borrow_global<Element>(addr);
//                if (i != 0) {
//                    get_another_item<Element>(i - 1, addr);
//                }
//            }
//            inline fun get_another_item<Element>(i: u8, addr: address) {
//                get_item<Element>(i - 1, addr);
//            }
//        }
//    """, listOf("Element"))


    private fun testFunction(@Language("Move") code: String, expectedTypes: List<String>) {
        InlineFile(myFixture, code, "main.move")

        val function = myFixture.findElementInEditor<MvFunction>()
        val actualTypes = GetFunctionAcquiresTypes(function).getResults().map { it.fullname }
        if (expectedTypes.isEmpty()) {
            check(actualTypes.isEmpty()) { "Expected empty list" }
        }

        check(actualTypes == expectedTypes) {
            "Expected $expectedTypes, actual $actualTypes"
        }
    }

    private fun testCallExpr(@Language("Move") code: String, expectedTypes: List<String>) {
        InlineFile(myFixture, code, "main.move")

        val callExpr = myFixture.findElementInEditor<MvCallExpr>()
        val inference = callExpr.inference(false) ?: error("No inference")
        val actualTypes =
            AcquiresTypeContext().getAcquiredTypesInCall(callExpr, inference).map { it.fullname }
        if (expectedTypes.isEmpty()) {
            check(actualTypes.isEmpty()) { "Expected empty list" }
        }

        check(actualTypes == expectedTypes) {
            "Expected $expectedTypes, actual $actualTypes"
        }
    }
}
