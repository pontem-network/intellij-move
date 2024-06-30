package org.move.lang.types.compilerV2

import org.move.utils.tests.CompilerV2Feat.INDEXING
import org.move.utils.tests.CompilerV2Features
import org.move.utils.tests.types.TypificationTestCase

@CompilerV2Features(INDEXING)
class IndexExprTypes: TypificationTestCase() {
    @CompilerV2Features()
    fun `test unknown without enabled feature`() = testExpr(
        """
        module 0x1::m {
            fun test_vector() {
                let v = vector[true, true];
                (v[0]);
              //^ <unknown>   
            }
        }        
    """
    )

    @CompilerV2Features()
    fun `test spec index expr`() = testExpr(
        """
        module 0x1::m {
            spec module {
                let v = vector[false];
                let a = v[0];
                a;
              //^ bool  
            }
        }     
    """
    )

    fun `test vector index expr`() = testExpr(
        """
        module 0x1::m {
            fun test_vector() {
                let v = vector[true, true];
                (v[0]);
              //^ bool   
            }
        }        
    """
    )

    fun `test vector index expr u8`() = testExpr(
        """
        module 0x1::m {
            fun test_vector() {
                let v = vector[true, true];
                (v[0u8]);
              //^ bool   
            }
        }        
    """
    )

    fun `test vector struct index expr`() = testExpr(
        """
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            fun test_vector() {
                let x = X {
                    value: 2u8
                };
                let v = vector[x, x];
                v[0].value;
                    //^ u8
            }
        }        
    """
    )

    fun `test vector borrow mut 0`() = testExpr(
        """
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            struct Y<T> has copy, key, drop {
                field: T
            }
            fun test_vector_borrow_mut() {
                let x1 = X {
                    value: true
                };
                let x2 = X {
                    value: false
                };
                let y1 = Y {
                    field: x1
                };
                let y2 = Y {
                    field: x2
                };
                let v = vector[y1, y2];
                (v[0]);
              //^ 0x1::m::Y<0x1::m::X<bool>>  
            }
        }        
    """
    )

    fun `test vector borrow mut 1`() = testExpr(
        """
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            struct Y<T> has key, drop {
                field: T
            }
            fun test_vector_borrow_mut() {
                let x1 = X {
                    value: true
                };
                let x2 = X {
                    value: false
                };
                let y1 = Y {
                    field: x1
                };
                let y2 = Y {
                    field: x2
                };
                let v = vector[y1, y2];
                (&mut v[0]);
              //^ &mut 0x1::m::Y<0x1::m::X<bool>> 
            }
        }        
    """
    )

    fun `test vector borrow mut 2`() = testExpr(
        """
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            struct Y<T> has key, drop {
                field: T
            }
            fun test_vector_borrow_mut() {
                let x1 = X {
                    value: true
                };
                let x2 = X {
                    value: false
                };
                let y1 = Y {
                    field: x1
                };
                let y2 = Y {
                    field: x2
                };
                let v = vector[y1, y2];
                ((&mut v[0]).field.value);
              //^ bool  
            }
        }        
    """
    )

    fun `test vector borrow mut 3`() = testExpr(
        """
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            struct Y<T> has key, drop {
                field: T
            }
            fun test_vector_borrow_mut() {
                let x1 = X {
                    value: true
                };
                let x2 = X {
                    value: false
                };
                let y1 = Y {
                    field: x1
                };
                let y2 = Y {
                    field: x2
                };
                let v = vector[y1, y2];
                (&v[0]);
              //^ &0x1::m::Y<0x1::m::X<bool>>  
            }
        }        
    """
    )

    fun `test vector borrow mut 4`() = testExpr(
        """
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            struct Y<T> has key, drop {
                field: T
            }
            fun test_vector_borrow_mut() {
                let x1 = X {
                    value: true
                };
                let x2 = X {
                    value: false
                };
                let y1 = Y {
                    field: x1
                };
                let y2 = Y {
                    field: x2
                };
                let v = vector[y1, y2];
                ((&v[1]).field.value);
              //^ bool
            }
        }        
    """
    )


    fun `test integer type inference with vector index expr 1`() = testExpr("""
        module 0x1::m {
            fun main() {
                let a = 1;
                let v = vector[2u8];
                a == v[0];
                a;
              //^ u8  
            }
        }        
    """)

    fun `test integer type inference with vector index expr 2`() = testExpr("""
        module 0x1::m {
            fun main() {
                let a = 1u8;
                let v = vector[2];
                a == v[0];
                v;
              //^ vector<u8>  
            }
        }        
    """)

    fun `test resource index expr 0`() = testExpr("""
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            struct Y<T> has key, drop {
                field: T
            }
        
            fun test_resource_3() acquires R {
                (Y<X<bool>>[@0x1]);
              //^ 0x1::m::Y<0x1::m::X<bool>>  
            }
        }        
    """)

    fun `test resource index expr ref expr`() = testExpr("""
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            struct Y<T> has key, drop {
                field: T
            }
        
            fun test_resource_3() acquires R {
                Y<X<bool>>[@0x1];
              //^ 0x1::m::Y<0x1::m::X<bool>>  
            }
        }        
    """)

    fun `test resource index expr 1`() = testExpr("""
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            struct Y<T> has key, drop {
                field: T
            }
        
            fun test_resource_3() acquires R {
                ((Y<X<bool>>[@0x1]).field.value);
              //^ bool  
            }
        }        
    """)

    fun `test resource index expr inference`() = testExpr("""
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            struct Y<T> has key, drop {
                field: T
            }
        
            fun test_resource_3() acquires R {
                let a = 1; 
                a == Y<X<u8>>[@0x1].field.value;
                a;
              //^ u8  
            }
        }        
    """)

    fun `test resource index expr 2`() = testExpr("""
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            struct Y<T> has key, drop {
                field: T
            }
        
            fun test_resource_4() acquires R {
                let addr = @0x1;
                let y = &mut Y<X<bool>>[addr];
                y;
              //^ &mut 0x1::m::Y<0x1::m::X<bool>> 
            }
        }        
    """)

    fun `test resource index expr 3`() = testExpr("""
        module 0x1::m {
            struct X<M> has copy, drop, store {
                value: M
            }
            struct Y<T> has key, drop {
                field: T
            }
        
            fun test_resource_4() acquires R {
                let addr = @0x1;
                let y = &Y<X<bool>>[addr];
                y;
              //^ &0x1::m::Y<0x1::m::X<bool>>   
            }
        }        
    """)

    fun `test vector index expr do not disable inference`() = testExpr("""
        module 0x1::m {
            fun main() {
                let v = vector[1, 2];
                let a = 1;
                v[a];
                a + 2u8;
                a;
              //^ u8  
            }
        }        
    """)
}