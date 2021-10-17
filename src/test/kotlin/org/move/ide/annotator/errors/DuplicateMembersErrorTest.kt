package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class DuplicateMembersErrorTest : AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test duplicate function in script`() = checkErrors(
        """
        script {
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
        }
    """
    )

    fun `test duplicate function in module`() = checkErrors(
        """
        module M {
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
            native fun <error descr="Duplicate definitions with name `main`">main</error>();
        }
    """
    )

    fun `test duplicate module definition`() = checkErrors(
        """
        address 0x0 {
            module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
            module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
        }
    """
    )

    fun `test duplicate module definition at the toplevel`() = checkErrors(
        """
        module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
        module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
    """
    )

    fun `test duplicate struct`() = checkErrors(
        """
        module M {
            struct <error descr="Duplicate definitions with name `MyStruct`">MyStruct</error> {}
            struct <error descr="Duplicate definitions with name `MyStruct`">MyStruct</error> {}
        }
    """
    )

    fun `test duplicate struct field`() = checkErrors(
        """
        module M {
            struct MyStruct {
                <error descr="Duplicate definitions with name `field`">field</error>: u8,
                <error descr="Duplicate definitions with name `field`">field</error>: u8,
            }
        }
    """
    )

//    fun `test duplicate acquires`() = checkErrors(
//        """
//        module M {
//            fun main<T>() acquires
//                <error descr="Duplicate definitions with name `T`">T</error>,
//                <error descr="Duplicate definitions with name `T`">T</error> {}
//        }
//    """
//    )

    fun `test duplicate consts`() = checkErrors(
        """
        module M {
            const <error descr="Duplicate definitions with name `MY_CONST`">MY_CONST</error>: u8 = 1;
            const <error descr="Duplicate definitions with name `MY_CONST`">MY_CONST</error>: u8 = 1;
        }
    """
    )

    fun `test no duplicate modules if defined with different addresses`() = checkErrors(
        """
        module 0x1::M {}
        module 0x2::M {}
    """
    )

    fun `test duplicate modules on the same addresses`() = checkErrors(
        """
        module 0x1::<error descr="Duplicate definitions with name `M`">M</error> {}
        module 0x1::<error descr="Duplicate definitions with name `M`">M</error> {}
    """
    )
}
