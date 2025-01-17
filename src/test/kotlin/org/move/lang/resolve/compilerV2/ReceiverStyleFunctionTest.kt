package org.move.lang.resolve.compilerV2

import org.move.utils.tests.MoveV2
import org.move.utils.tests.resolve.ResolveTestCase

@MoveV2()
class ReceiverStyleFunctionTest: ResolveTestCase() {

    fun `test resolve receiver function`() = checkByCode("""
        module 0x1::main {
            struct S { x: u64 }
            fun receiver(self: S, y: u64): u64 {
               //X
                self.x + y
            }
            fun test_call_styles(s: S): u64 {
                s.receiver(1)
                  //^
            }
        }        
    """)

    fun `test resolve self parameter`() = checkByCode("""
        module 0x1::main {
            struct S { x: u64 }
            fun receiver(self: S, y: u64): u64 {
                         //X
                self.x + y
                //^
            }
        }        
    """)

    fun `test resolve receiver function with reference`() = checkByCode("""
        module 0x1::main {
            struct S { x: u64 }
            fun receiver_ref(self: &S, y: u64): u64 {
               //X
                self.x + y
            }
            fun test_call_styles(s: S): u64 {
                s.receiver_ref(1)
                  //^
            }
        }        
    """)

    fun `test resolve receiver function with mut reference`() = checkByCode("""
        module 0x1::main {
            struct S { x: u64 }
            fun receiver_mut_ref(self: &mut S, y: u64): u64 {
               //X
                self.x + y
            }
            fun test_call_styles(s: S): u64 {
                s.receiver_mut_ref(1)
                  //^
            }
        }        
    """)

    fun `test resolve receiver function with inline mut reference`() = checkByCode("""
        module 0x1::main {
            struct S { x: u64 }
            inline fun receiver_mut_ref(self: &mut S, y: u64): u64 {
                      //X
                self.x + y
            }
            fun test_call_styles(s: S): u64 {
                s.receiver_mut_ref(1)
                  //^
            }
        }        
    """)

    fun `test cannot be resolved if self has another type`() = checkByCode("""
        module 0x1::main {
            struct S { x: u64 }
            struct T { x: u64 }
            fun receiver(self: T, y: u64): u64 {
                self.x + y
            }
            fun test_call_styles(s: S): u64 {
                s.receiver(1)
                  //^ unresolved
            }
        }        
    """)

    fun `test cannot be resolved if self requires mutable reference`() = checkByCode("""
        module 0x1::main {
            struct S { x: u64 }
            fun receiver(self: &mut S, y: u64): u64 {
                self.x + y
            }
            fun test_call_styles(s: &S): u64 {
                s.receiver(1)
                  //^ unresolved
            }
        }        
    """)

    fun `test cannot be resolved if self requires no reference`() = checkByCode("""
        module 0x1::main {
            struct S { x: u64 }
            fun receiver(self: S, y: u64): u64 {
                self.x + y
            }
            fun test_call_styles(s: &mut S): u64 {
                s.receiver(1)
                  //^ unresolved
            }
        }        
    """)

    fun `test receiver resolvable if self requires reference but mut reference exists`() = checkByCode("""
        module 0x1::main {
            struct S { x: u64 }
            fun receiver(self: &S, y: u64): u64 {
                //X
                self.x + y
            }
            fun test_call_styles(s: &mut S): u64 {
                s.receiver(1)
                  //^
            }
        }        
    """)

    fun `test public receiver style from another module`() = checkByCode("""
        module 0x1::m {
            struct S { x: u64 }
            public fun receiver(self: S, y: u64): u64 {
                         //X
                self.x + y
            }
        }
        module 0x1::main {
            use 0x1::m::S;
            
            fun test_call_styles(s: S): u64 {
                s.receiver(1)
                  //^
            }
        }
    """)

    fun `test unresolved private receiver style from another module`() = checkByCode("""
        module 0x1::m {
            struct S { x: u64 }
            fun receiver(self: S, y: u64): u64 {
                self.x + y
            }
        }
        module 0x1::main {
            use 0x1::m::S;
            
            fun test_call_styles(s: S): u64 {
                s.receiver(1)
                  //^ unresolved
            }
        }
    """)

    fun `test resolve receiver style with generic argument`() = checkByCode("""
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T>(self: S<T>): T {
               //X
                self.field
            }
            fun main(s: S<u8>) {
                s.receiver()
                  //^
            }
        }        
    """)

    fun `test method cannot be resolved if self is not the first parameter`() = checkByCode("""
        module 0x1::main {
            struct S { x: u64 }
            fun receiver(y: u64, self: &S): u64 {
                self.x + y
            }
            fun test_call_styles(s: S): u64 {
                s.receiver(&s)
                  //^ unresolved
            }
        }                
    """)

    fun `test friend function method`() = checkByCode("""
        module 0x1::m {
            friend 0x1::main;
            struct S { x: u64 }
            public(friend) fun receiver(self: &S): u64 { self.x }
                               //X
        }
        module 0x1::main {
            use 0x1::m::S;
            fun main(s: S) {
                s.receiver();
                  //^
            }
        }        
    """)

    @MoveV2()
    fun `test public package method`() = checkByCode("""
        module 0x1::m {
            struct S { x: u64 }
            public(package) fun receiver(self: &S): u64 { self.x }
                                  //X
        }
        module 0x1::main {
            use 0x1::m::S;
            fun main(s: S) {
                s.receiver();
                  //^
            }
        }        
    """)

    fun `test friend function method unresolved`() = checkByCode("""
        module 0x1::m {
            struct S { x: u64 }
            public(friend) fun receiver(self: &S): u64 { self.x }
        }
        module 0x1::main {
            use 0x1::m::S;
            fun main(s: S) {
                s.receiver();
                  //^ unresolved
            }
        }        
    """)

    fun `test method with enum inner item`() = checkByCode("""
        module 0x1::m {
            enum Inner {
                Inner1{ x: u64 }
                Inner2{ x: u64, y: u64 }
            }
            struct Box has drop {
               x: u64
            }
            enum Outer {
                None,
                One{ i: Inner },
                Two { i: Inner, b: Box }
            }
            public fun is_inner1(self: &Inner): bool {
                        //X
                match (self) {
                    Inner1{x: _} => true,
                    _ => false
                }
            }
            public fun main(o: Outer) {
                match (o) {
                    None => false
                    One { i } if i.is_inner1() => true,
                                   //^
                }
            }
        }        
    """)

    @MoveV2
    fun `test enum with receiver function from a call expr`() = checkByCode("""
        module 0x1::m {
            enum Ordering has copy, drop {
                Less,
                Equal,
                Greater,
            }
            native public fun compare<T>(first: &T, second: &T): Ordering;
            public fun is_eq(self: &Ordering): bool {
                       //X
                self is Ordering::Equal
            }
            fun main() {
                compare(&1, &1).is_eq();
                               //^
            }
        }        
    """)
}