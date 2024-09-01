package org.move.lang.resolve

import org.move.utils.tests.MoveV2
import org.move.utils.tests.resolve.ResolveTestCase

@MoveV2()
class ResolveResourceAccessSpecifiersTest: ResolveTestCase() {
    fun `test resolve type for reads`() = checkByCode(
        """
        module 0x1::main {
            struct S { field: u8 }
                 //X
            fun main() reads S {}
                           //^
        }        
    """
    )

    fun `test resolve type for write`() = checkByCode(
        """
        module 0x1::main {
            struct S { field: u8 }
                 //X
            fun main() writes S {}
                            //^
        }        
    """
    )

    fun `test resolve type for acquires`() = checkByCode(
        """
        module 0x1::main {
            struct S { field: u8 }
                 //X
            fun main() acquires S {}
                              //^
        }        
    """
    )

    fun `test resolve address function in address specifier`() = checkByCode(
        """
        module 0x1::main {
            struct S { field: u8 }
            fun main(value: u8) reads *(make_up_address(value)) {}
                                       //^
            fun make_up_address(x: u8): address { @0x1 }
                  //X
        }        
    """
    )

    fun `test resolve module with wildcard`() = checkByCode(
        """
        module 0x1::main {
                   //X
            struct S { field: u8 }
            fun main(value: u8) reads 0x1::main::* {}
                                          //^
        }        
    """
    )

    fun `test resolve parameter for address function`() = checkByCode(
        """
        module 0x1::main {
            struct S { field: u8 }
            fun main(
                value: u8
                //X
            ) reads *(make_up_address(value)) {}
                                    //^
            fun make_up_address(x: u8): address { @0x1 }
        }        
    """
    )

    fun `test address function can be from another module`() = checkByCode(
        """
        module 0x1::signer {
            public native fun address_of(s: &signer);
                              //X
        }
        module 0x1::main {
            use 0x1::signer;
            struct S { field: u8 }
            fun main(s: &signer) reads *(signer::address_of(s)) {}
                                                 //^
        }        
    """
    )
}