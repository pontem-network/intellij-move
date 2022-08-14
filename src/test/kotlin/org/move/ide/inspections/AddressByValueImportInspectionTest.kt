package org.move.ide.inspections

import org.move.ide.inspections.AddressByValueImportInspection
import org.move.utils.tests.annotation.InspectionProjectTestBase

class AddressByValueImportInspectionTest : InspectionProjectTestBase(AddressByValueImportInspection::class) {
    fun `test no inspection if imported from the correct address name`() = checkByFileTree(
        {
            moveToml(
                """
        [package]
        name = "MyPackage"
        [addresses]
        std = "0x1"
        aptos_std = "0x1"
        """
            )
            sources {
                main(
                    """
                module aptos_std::main {
                    use std::debug/*caret*/;
                    fun main() {
                        debug::print();
                    }
                }    
                """
                )
                move(
                    "debug.move", """
            module std::debug { 
                public native fun print();
            }    
            """
                )
            }
        })

    fun `test no inspection if imported from the different address name with different value`() =
        checkByFileTree(
            {
                moveToml(
                    """
        [package]
        name = "MyPackage"
        [addresses]
        std = "0x1"
        aptos_std = "0x1"
        std2 = "0x2"
        """
                )
                sources {
                    main(
                        """
                module aptos_std::main {
                    use std2::debug/*caret*/;
                    fun main() {
                        debug::print();
                    }
                }    
                """
                    )
                    move(
                        "debug.move", """
            module std::debug { 
                public native fun print();
            }    
            """
                    )
                }
            })

    fun `test fail if imported from the different address name but same value`() = checkFixByFileTree("Change address to `std`",
        {
            moveToml(
                """
        [package]
        name = "MyPackage"
        [addresses]
        std = "0x1"
        aptos_std = "0x1"
        """
            )
            sources {
                main(
                    """
                module aptos_std::main {
                    use <weak_warning descr="Module is declared with a different address `std`">aptos_std::debug/*caret*/</weak_warning>;
                    fun main() {
                        debug::print();
                    }
                }    
                """
                )
                move(
                    "debug.move", """
            module std::debug { 
                public native fun print();
            }    
            """
                )
            }
        }, """
                module aptos_std::main {
                    use std::debug;
                    fun main() {
                        debug::print();
                    }
                }    
        """, checkWeakWarn = true)
}
