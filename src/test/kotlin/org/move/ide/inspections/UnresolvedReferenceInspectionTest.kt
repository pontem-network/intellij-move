package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionsTestCase

class UnresolvedReferenceInspectionTest : InspectionsTestCase(UnresolvedReferenceInspection::class) {
    fun `test unresolved variable`() = checkByText("""
        module M {
            fun main() {
                <error descr="Unresolved reference: `x`">x</error>;
            }
        }
    """)

    fun `test unresolved function call`() = checkByText("""
        module M {
            fun main() {
                <error descr="Unresolved reference: `call`">call</error>();
            }
        }
    """)

    fun `test test unresolved module member`() = checkByText("""
        script {
            use 0x1::MyModule::call;

            fun main() {
                <error descr="Unresolved reference: `call`">call</error>();
            }
        }
    """)

    fun `test test no unresolved reference for builtin`() = checkByText("""
        script {
            fun main() {
                assert(false, 1);
            }
        }
    """)

    fun `test test no unresolved reference for primitive type`() = checkByText("""
        script {
            fun main(s: &signer) {
            }
        }
    """)

    fun `test unresolved reference to variable in struct constructor field`() = checkByText(
        """
        module M {
            struct T {
                my_field: u8
            }

            fun main() {
                let t = T { my_field: <error descr="Unresolved reference: `my_unknown_field`">my_unknown_field</error> };
            }
        }
    """
    )

    fun `test unresolved reference to variable in struct shorthand`() = checkByText(
        """
        module M {
            struct T {
                my_field: u8
            }

            fun main() {
                let t = T { <error descr="Unresolved reference: `my_field`">my_field</error> };
            }
        }
    """
    )

    fun `test unresolved reference to field in struct constructor`() = checkByText(
        """
        module M {
            struct T {
                my_field: u8
            }

            fun main() {
                let t = T { <error descr="Unresolved field: `my_unknown_field`">my_unknown_field</error>: 1 };
            }
        }
    """
    )

    fun `test unresolved reference to field in struct pat`() = checkByText(
        """
        module M {
            struct T {
                my_field: u8
            }

            fun main() {
                let T { <error descr="Unresolved field: `my_unknown_field`">my_unknown_field</error>: _ } = T { };
            }
        }
    """
    )

    fun `test unresolved reference to field in struct pat shorthand`() = checkByText(
        """
        module M {
            struct T {
                my_field: u8
            }

            fun main() {
                let T { <error descr="Unresolved field: `my_unknown_field`">my_unknown_field</error> } = T { };
            }
        }
    """
    )

    fun `test unresolved reference to module`() = checkByText(
        """
        module M {
            fun main() {
                let t = <error descr="Unresolved module reference: `Transaction`">Transaction</error>::create();
            }
        }
    """
    )

    fun `test no unresolved reference for fully qualified module`() = checkByText(
        """
        module M {
            fun main() {
                0x1::Debug::print(1);
            }
        }
    """
    )

    fun `test unresolved reference for method of another module`() = checkByText(
        """
    address 0x1 {
        module Other {}
        module M {
            use 0x1::Other;
            fun main() {
                Other::<error descr="Unresolved reference: `emit`">emit</error>();
            }
        }
    }
        """
    )

    fun `test unresolved reference to type in generic`() = checkByText(
        """
        module M {
            fun deposit<Token> () {}

            fun main() {
                deposit<<error descr="Unresolved type: `PONT`">PONT</error>>()
            }
        }    
        """
    )

    fun `test no unresolved reference inspection inside spec`() = checkByText("""
    module 0x1::M {
        spec module {
            fun m(e: EventHandle) {}
        }
    }    
    """)
}
