package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveStructFieldsTest : ResolveTestCase() {
    fun `test resolve reference to field from constructor`() = checkByCode(
        """
        module 0x1::M {
            struct T {
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
        module 0x1::M {
            struct T {
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
        module 0x1::M {
            struct T {
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
        module 0x1::M {
            struct Option<Element> has copy, drop, store {
                vec: vector<Element>
              //X  
            }
            
            public fun is_none<Element>(t: &Option<Element>) {
                &t.vec;
                  //^
            }
        }    
    """
    )

    fun `test resolve fields from dot access to struct mutable reference`() = checkByCode(
        """
        module 0x1::M {
            struct Option<Element> has copy, drop, store {
                vec: vector<Element>
              //X  
            }
            
            public fun is_none<Element>(t: &mut Option<Element>): bool {
                &t.vec;
                  //^
            }
        }    
    """
    )

    fun `test resolve field for borrow_global_mut`() = checkByCode(
        """
        module 0x1::M {
            struct CapState<phantom Feature> has key { delegates: vector<address> }
                                                     //X
            fun m() acquires CapState {
                borrow_global_mut<CapState<u8>>(@0x1).delegates;
                                                     //^
            }
        }    
        """
    )

    fun `test resolve field for parameter type`() = checkByCode(
        """
        module 0x1::M {
            struct Cap<phantom Feature> has key { root: address }
                                                 //X
            fun m<Feature>(cap: Cap<Feature>) {
                cap.root;
                  //^          
            }
        }    
        """
    )

    fun `test unresolved field for struct in a different module`() = checkByCode("""
        module 0x1::M1 {
            struct S { val: u8 }
            public fun get_s(): S { S { val: 10 } }
        }        
        module 0x1::M {
            use 0x1::M1;
            fun main() {
                M1::get_s().val
                           //^ unresolved
            }            
        } 
    """)

    fun `test resolve field in test`() = checkByCode("""
        module 0x1::M {
            struct S<K, V> { val: u8 }
                           //X
            fun get_s<K, V>(): S<K, V> { S<u8, u8> { val: 10} }
            #[test]
            fun test_s() {
                let s = get_s();
                s.val;
                 //^
            }
        } 
    """)

    fun `test resolve field for vector inferred type`() = checkByCode(
        """
    module 0x1::M {
        struct ValidatorInfo { field: u8 }
                              //X
        native public fun vector_empty<El>(): vector<El>;
        native public fun vector_push_back<PushElement>(v: &mut vector<PushElement>, e: PushElement);
        native public fun vector_borrow_mut<BorrowElement>(v: &mut vector<BorrowElement>, i: u64): &mut BorrowElement;
        fun call() {
            let v = vector_empty();
            let item = ValidatorInfo { field: 10 };
            vector_push_back(&mut v, item);
            vector_borrow_mut(&mut v, 10).field;
                                          //^
        }
    }        
    """
    )

    fun `test access fields of imported structs in specs`() = checkByCode("""
        module 0x1::coin {
            struct Coin { value: u64 }
                         //X
            public fun get_coin(): Coin { Coin { value: 10 } }
        }        
        module 0x1::m {
            use 0x1::coin::get_coin;
            
            spec module {
                get_coin().value;
                             //^
            } 
        }
    """)

    fun `test struct from another module can be created in specs`() = checkByCode("""
        module 0x1::m {
            struct Coin { val: u8 }
                  //X
        }
        module 0x1::main {
            use 0x1::m;
            spec module {
                let _ = m::Coin { val: 10 };
                           //^
            }
        }
    """)

    fun `test resolve field from include schema`() = checkByCode(
        """
        module 0x1::m {
            struct S { val: u8 }
                      //X
            spec schema MySchema {
                schema_val: u8;
            }
            spec module {
                let s = S { val: 10 };
                include MySchema {
                    schema_val: s.val
                                 //^
                };
            }
        }        
    """
    )
}
