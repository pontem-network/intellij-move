package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveStructFieldsTest : ResolveTestCase() {
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

    fun `test resolve fields from dot access to struct reference`() = checkByCode(
        """
        module M {
            struct Option<Element> has copy, drop, store {
                vec: vector<Element>
              //X  
            }
            
            public fun is_none<Element>(t: &Option<Element>): bool {
                Vector::is_empty(&t.vec)
                                  //^
            }
        }    
    """
    )

    fun `test resolve fields from dot access to struct mutable reference`() = checkByCode(
        """
        module M {
            struct Option<Element> has copy, drop, store {
                vec: vector<Element>
              //X  
            }
            
            public fun is_none<Element>(t: &mut Option<Element>): bool {
                Vector::is_empty(&t.vec)
                                  //^
            }
        }    
    """
    )
}
