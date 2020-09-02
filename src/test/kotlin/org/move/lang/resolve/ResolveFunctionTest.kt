package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveFunctionTest : ResolveTestCase() {
    fun `test resolve reference to function`() = checkByCode(
        """
        module M {
            fun call(): u8 {
              //X
                1
            }
            
            fun main() {
                call();
              //^
            }
        }
    """
    )

    fun `test resolve reference to native function`() = checkByCode(
        """
        module M {
            native fun call(): u8;
                     //X
            
            fun main() {
                call();
              //^
            }
        }
    """
    )

    fun `test resolve from spec fun to function`() = checkByCode(
        """
        module M {
            fun call(): u8 {}
              //X

            spec fun call {
                   //^
                assert true;
            }
        }
    """
    )

    fun `test resolve type param in param pos`() = checkByCode(
        """
        module M {
            fun call<T>
                   //X
                    (val: T) {}
                        //^
        }
    """
    )

    fun `test resolve type param in return pos`() = checkByCode(
        """
        module M {
            fun call<T>
                   //X 
                    (): T {}
                      //^
        }
    """
    )

    fun `test resolve type param in acquires`() = checkByCode(
        """
        module M {
            fun call<T>
                   //X 
                    () acquires T {}
                              //^
        }
    """
    )

    fun `test type params used in call expr`() = checkByCode(
        """
        module M {
            fun convert<T>() {
                      //X
                call<T>()
                   //^
            }
        }
    """
    )
}