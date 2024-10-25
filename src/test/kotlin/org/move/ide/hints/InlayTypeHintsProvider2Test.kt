package org.move.ide.hints

import com.intellij.testFramework.utils.inlays.declarative.DeclarativeInlayHintsProviderTestCase
import org.intellij.lang.annotations.Language
import org.move.ide.hints.type.MvTypeInlayHintsProvider2

class InlayTypeHintsProvider2Test: DeclarativeInlayHintsProviderTestCase() {
    fun `test primitive type`() = checkByText("""
        module 0x1::m {
            fun call(): bool { true }
            fun main() {
                let a/*<# : |bool #>*/ = call();
            }
        }        
    """)

    fun `test tuple type`() = checkByText("""
        module 0x1::m {
            fun call(): (bool, u8) { (true, 1) }
            fun main() {
                let t/*<# : |(|bool|, |u8|) #>*/ = call();
            }
        }        
    """)

    fun `test tuple type destructured`() = checkByText("""
        module 0x1::m {
            fun call(): (bool, u8) { (true, 1) }
            fun main() {
                let (a/*<# : |bool #>*/, b/*<# : |u8 #>*/) = call();
            }
        }        
    """)

    fun `test struct type`() = checkByText("""
        module 0x1::m {
            struct S {}
            fun call(): S {}
            fun main() {
                let s/*<# : |S #>*/ = call();
            }
        }        
    """)

    fun `test struct type with generics`() = checkByText("""
        module 0x1::m {
            struct S<R> {}
            fun call<R>(): S<R> {}
            fun main() {
                let s/*<# : |S|<|u8|> #>*/ = call<u8>();
            }
        }        
    """)

    fun `test vector type`() = checkByText("""
        module 0x1::m {
            fun main() {
                let a/*<# : |vector|[|integer|] #>*/ = vector[1];
            }
        }        
    """)

    fun `test uninferred vector type for unknown item type`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let a/*<# : |vector|[|?|] #>*/ = vector[unknown()];
            }
        }
    """
    )

    fun `test vector uninferred type`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let a/*<# : |vector|[|?|] #>*/ = vector[];
            }
        }
    """
    )

    fun `test no type hint for unknown type`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let a = unknown();
            }
        }
    """
    )

    fun `test no type hints for function parameters`() = checkByText(
        """
        module 0x1::m {
            fun main(a: u8) {
            }
        }
    """
    )

    fun `test no hints for private variables`() = checkByText("""
        module 0x1::m {
            fun main() {
                let _ = 1;
                let _a = 1;
                let (_a, _b) = (1, 1);
            }
        }
    """)

    fun `test let stmt without expr`() = checkByText("""
        module 0x1::m {
            fun main() {
                let a/*<# : |integer #>*/;
                a = 1;
            }
        }
    """)

    fun `test lambda expr`() = checkByText("""
        module 0x1::m {
            fun callback(elem: u8, ident: |u8|u8): u8 { ident(elem) }
            fun main() {
                callback(10, |elem/*<# : |u8 #>*/| elem + 1);
            }
        }        
    """)

    private fun checkByText(@Language("Move") code: String) {
        doTestProvider("main.move", code, MvTypeInlayHintsProvider2())
    }
}