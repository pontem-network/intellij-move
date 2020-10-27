package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveStructFieldsTest: ResolveTestCase() {
    fun `test resolve reference to field from constructor`() = checkByCode(
        """
        module M {
            resource struct T {
                my_field: u8
              //X  
            }

            fun main() {
                let t = T { my_field: 1 };
                          //^
            }
        }
    """
    )

    fun `test resolve reference to field from shorthard`() = checkByCode(
        """
        module M {
            resource struct T {
                my_field: u8
              //X  
            }

            fun main() {
                let my_field = 1;
                let t = T { my_field };
                          //^
            }
        }
    """
    )

    fun `test resolve reference to field from pattern`() = checkByCode(
        """
        module M {
            resource struct T {
                my_field: u8
              //X  
            }

            fun main() {
                let T { my_field: my_field_1 } = call();
                      //^
            }
        }
    """
    )

    fun `test resolve reference to field from pattern shorthand`() = checkByCode(
        """
        module M {
            resource struct T {
                my_field: u8
              //X  
            }

            fun main() {
                let T { my_field } = call();
                      //^
            }
        }
    """
    )
}