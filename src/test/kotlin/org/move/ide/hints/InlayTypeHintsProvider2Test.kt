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

    fun `test no inlay hint if explicit lambda param declared`() = checkByText("""
        module 0x1::m {
            fun callback(elem: u8, ident: |u8|u8): u8 { ident(elem) }
            fun main() {
                callback(10, |elem: u8| elem + 1);
            }
        }        
    """)

    fun `test no inlay hint if explicit let type`() = checkByText("""
        module 0x1::m {
            fun main() {
                let a: u8 = 1;
            }
        }        
    """)

    fun `test no inlay hints for struct pattern destructor`() = checkByText("""
        module 0x1::m {
            struct S { val: u8 }
            fun main(s: S) {
                let S { val: myval } = s;
            }
        }        
    """)

    fun `test inlay hints for struct pattern destructor shorthand`() = checkByText("""
        module 0x1::m {
            struct S { val: u8 }
            fun main(s: S) {
                let S { val/*<# : |u8 #>*/ } = s;
            }
        }        
    """)

    fun `test no inlay hint for const`() = checkByText("""
        module 0x1::m {
            const MY_CONST: u8 = 1;
        }        
    """)

    fun `test inlay type hint for complex field of enum type of resource index expr`() = checkByText(
        """
        module 0x1::m {
            struct BigOrderedMap<K: store, V: store> has store { }
            struct Any has drop, store, copy { }
            struct StoredPermission has store, copy, drop { }
            enum PermissionStorage has key {
                V1 {
                    perms: BigOrderedMap<Any, StoredPermission>
                }
            }
            fun main() {
                let storage1/*<# : |&mut |PermissionStorage #>*/ = borrow_global_mut<PermissionStorage>(@0x1);
                let s/*<# : |&mut |BigOrderedMap|<...> #>*/ = &mut storage1.perms;
                
                let storage2/*<# : |&mut |PermissionStorage #>*/ = &mut PermissionStorage[@0x1];
            }
        }    
    """
    )

    private fun checkByText(@Language("Move") code: String) {
        doTestProvider("main.move", code, MvTypeInlayHintsProvider2())
    }
}