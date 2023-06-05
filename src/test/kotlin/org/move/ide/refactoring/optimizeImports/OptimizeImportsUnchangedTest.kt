package org.move.ide.refactoring.optimizeImports

import org.intellij.lang.annotations.Language

class OptimizeImportsUnchangedTest : OptimizeImportsTestBase() {

    fun `test no change`() = checkNotChanged(
        """
        script {
            use 0x1::M::{call, MyStruct};
        
            fun main() {
                let a: MyStruct = call();
            }
        }
    """
    )

    fun `test won't remove braces if single import with alias with left comment`() = checkNotChanged(
        """
        module 0x1::m {
            use 0x1::getopts::{/*comment*/A as B};
        
            fun main() {
                let b: B;
            }
        }
    """
    )

    fun `test won't remove braces if single import with alias with right comment`() = checkNotChanged(
        """
        module 0x1::m {
            use 0x1::getopts::{A as B/*comment*/};
        
            fun main() {
                let b: B;
            }
        }
    """
    )

    fun `test won't remove braces if single import with alias with inner left comment`() = checkNotChanged(
        """
        module 0x1::m {
            use 0x1::getopts::{A /*comment*/as B};
        
            fun main() {
                let b: B;
            }
        }
    """
    )

    fun `test won't remove braces if single import with alias with inner right comment`() = checkNotChanged(
        """
        module 0x1::m {
            use 0x1::getopts::{A as/*comment*/ B};
        
            fun main() {
                let b: B;
            }
        }
    """
    )

    fun `test won't remove braces if import with left comment`() = checkNotChanged(
        """
        module 0x1::m {
            use 0x1::getopts::{/*comment*/A};
        
            fun main() {
                let a: A;
            }
        }
    """
    )

    fun `test won't remove braces if import with right comment`() = checkNotChanged(
        """
        module 0x1::m {
            use 0x1::getopts::{A/*comment*/};
        
            fun main() {
                let a: A;
            }
        }
    """
    )

    fun `test wont remove braces if multi import`() = checkNotChanged(
        """
        module 0x1::m {
            use 0x1::getopts::{A, B};
        
            fun main() {
                let a: A;
                let b: B;
            }
        }
    """
    )

    fun `test change into module ref for single Self`() = checkNotChanged(
        """
        module 0x1::m {
            use 0x1::getopts::Self;
        
            fun main() {
                let a: getopts::A;
            }
        }
    """
    )

    private fun checkNotChanged(@Language("Move") code: String) = doTest(code, code)
}
