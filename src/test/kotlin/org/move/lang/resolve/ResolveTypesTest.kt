package org.move.lang.resolve

import org.move.utils.tests.MoveV2
import org.move.utils.tests.resolve.ResolveTestCase

class ResolveTypesTest : ResolveTestCase() {
    fun `test resolve struct as function param type`() = checkByCode(
        """
        module M {
            struct MyStruct {}
                 //X
            
            fun call(s: MyStruct) {}
                      //^
        }
    """
    )

    fun `test resolve struct as return type`() = checkByCode(
        """
        module M {
            struct MyStruct {}
                 //X
            
            fun call(): MyStruct {}
                      //^
        }
    """
    )

    fun `test resolve struct as acquires type`() = checkByCode(
        """
        module M {
            struct MyStruct {}
                 //X
            
            fun call() acquires MyStruct {}
                              //^
        }
    """
    )

    fun `test resolve struct for struct literal`() = checkByCode(
        """
        module 0x1::m {
            struct MyStruct {}
                 //X
            
            fun call() {
                let a = MyStruct {};
                      //^
            }
        }
    """
    )

    fun `test cannot resolve struct for struct literal from another module`() = checkByCode(
        """
        module 0x1::s {
            struct MyStruct {}
        }
        module 0x1::m {
            use 0x1::s::MyStruct;
            fun call() {
                let a = MyStruct {};
                      //^ unresolved
            }
        }
    """
    )

    fun `test resolve struct from another module for import`() = checkByCode(
        """
        module 0x1::s {
            struct MyStruct {}
                    //X
        }
        module 0x1::m {
            use 0x1::s::MyStruct;
                        //^
        }
    """
    )

    fun `test resolve struct as struct pattern destructuring`() = checkByCode(
        """
        module M {
            struct MyStruct { val: u8 }
                 //X
            
            fun call() {
                let MyStruct { val } = get_struct();
                  //^
            }
        }
    """
    )

    fun `test resolve struct as type param`() = checkByCode(
        """
        module M {
            resource struct MyStruct {}
                          //X
            
            fun call() {
                let a = move_from<MyStruct>();
                                //^
            }
        }
    """
    )

    fun `test resolve struct type param`() = checkByCode(
        """
        module M {
            struct MyStruct<T> {
                          //X
                val: T
                   //^
            }
        }
    """
    )

    fun `test resolve struct type param inside vector`() = checkByCode(
        """
        module M {
            struct MyStruct<T> {
                          //X
                val: vector<T>
                          //^
            }
        }
    """
    )

    fun `test resolve struct type to struct`() = checkByCode(
        """
        module M {
            struct Native {}
                 //X
            fun main(n: Native): u8 {}
                      //^
        }
    """
    )

    fun `test resolve struct type with generics`() = checkByCode(
        """
        module M {
            struct Native<T> {}
                 //X
            fun main(n: Native<u8>): u8 {}
                      //^
        }
    """
    )

    fun `test resolve type from import`() = checkByCode(
        """
        address 0x1 {
            module Transaction {
                struct Sender {}
                     //X
            }
        }
        script {
            use 0x1::Transaction::Sender;
                                //^
        }
    """
    )

    fun `test resolve type from usage`() = checkByCode(
        """
        address 0x1 {
            module Transaction {
                struct Sender {}
                     //X
            }
        }
        script {
            use 0x1::Transaction::Sender;

            fun main(n: Sender) {}
                      //^
        }
    """
    )

    fun `test resolve type to alias`() = checkByCode(
        """
        module M {
            use 0x1::Transaction::Sender as MySender;
                                          //X
            fun main(n: MySender) {}
                      //^
        }
    """
    )

    fun `test resolve return type to alias`() = checkByCode(
        """
        module M {
            use 0x1::Transaction::Sender as MySender;
                                          //X
            fun main(): MySender {}
                      //^
        }
    """
    )

    fun `test function return type to type param`() = checkByCode(
        """
        module M {
            fun main<Token>()
                   //X
                : Token {}
                //^
        }
    """
    )

    fun `test function return type param to type param`() = checkByCode(
        """
        module M {
            struct Coin<Token> {}
            
            fun main<Token>()
                   //X
                    : Coin<Token> {}
                         //^
        }
    """
    )

    fun `test native function return type param to type param`() = checkByCode(
        """
        module M {
            struct Coin<Token> {}
            
            native fun main<Token>()
                          //X
                    : Coin<Token>;
                         //^
        }
    """
    )

    fun `test struct unresolved in name expr`() = checkByCode("""
        address 0x1 {
            module A {
                struct S {}
            }
            module B {
                use 0x1::A;
                fun call() {
                    A::S
                     //^ unresolved                   
                }
            }
        }
    """)

    fun `test resolve type param in native function in spec`() = checkByCode("""
    module 0x1::M {
        spec module {
            /// Native function which is defined in the prover's prelude.
            native fun serialize<MoveValue>(
                                    //X
                v: &MoveValue
                    //^
            ): vector<u8>;
        }
    }    
    """)

    fun `test resolve struct from use item`() = checkByCode("""
    module 0x1::M {
        struct MyStruct {}
               //X
    }    
    module 0x1::Main {
        use 0x1::M::{Self, MyStruct};
                          //^
    }
    """)

//    fun `test resolve type parameters in specs`() = checkByCode("""
//    module 0x1::main {
//        fun call<T>(a: u8, b: u8) {}
//               //X
//    }
//    spec 0x1::main {
//        spec call<T>(a: u8, b: u8) {}
//                //^
//    }
//    """)

    fun `test resolve type for local import`() = checkByCode("""
module 0x1::table {
    struct Table {}
           //X
}        
module 0x1::main {
    struct S<phantom T> has key {}
    fun main() {
        use 0x1::table::Table;
        
        assert!(exists<S<Table>>(@0x1), 1);
                         //^
    }
}        
    """)

    fun `test resolve type for local import in spec`() = checkByCode("""
module 0x1::table {
    struct Table {}
           //X
}        
module 0x1::main {
    struct S<phantom T> has key {}
    fun main() {}
}   
spec 0x1::main {
    spec main {
        use 0x1::table::Table;
        
        assert!(exists<S<Table>>(@0x1), 1);
                         //^
    }
}
    """)

    fun `test type parameter in return`() = checkByCode("""
module 0x1::m {
    public fun remove<K: copy + drop, V>(
                                    //X
        val: V
    ): V {
     //^
        val
    }
}        
    """)

    @MoveV2()
    fun `test resource index expr`() = checkByCode("""
        module 0x1::m {
            struct S has key {}
                 //X
            fun main() {
                S[@0x1];
              //^   
            }
        }        
    """)

    fun `test module resolution not available on type position`() = checkByCode("""
        module 0x1::Transaction {
            struct Type {
                val: u8                   
            }
        }
        module 0x1::M {
            fun main(a: 0x1::Transaction::Transaction) {
                                           //^ unresolved
            }
        }
    """)

    fun `test resolve enum type`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
               //X
            fun main(one: S::One) {
                        //^
            }
        }        
    """)

    fun `test resolve enum type from module`() = checkByCode("""
        module 0x1::m { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            use 0x1::m;
            fun main(one: m::S) {
                           //^
            }
        }        
    """)

    fun `test resolve enum type from module with variant`() = checkByCode("""
        module 0x1::m { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            use 0x1::m;
            fun main(one: m::S::One) {
                           //^
            }
        }        
    """)

    fun `test resolve enum type from module with the same name`() = checkByCode("""
        module 0x1::S { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            use 0x1::S;
            fun main(one: S::S::One) {
                           //^
            }
        }        
    """)

    fun `test resolve enum type from fully qualified`() = checkByCode("""
        module 0x1::m { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            fun main(one: 0x1::m::S) {
                                //^
            }
        }        
    """)

    fun `test resolve enum type from fully qualified with variant`() = checkByCode("""
        module 0x1::m { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            fun main(one: 0x1::m::S::One) {
                                //^
            }
        }        
    """)

    fun `test resolve enum type from item import`() = checkByCode("""
        module 0x1::m { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            use 0x1::m::S;
            fun main(one: S::One) {
                        //^
            }
        }        
    """)

    fun `test resolve enum variant`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                   //X
            fun main(one: S::One) {
                            //^
            }
        }        
    """)

    fun `test resolve enum variant from module`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                   //X
        }
        module 0x1::main {
            use 0x1::m;
            fun main(one: m::S::One) {
                               //^
            }
        }        
    """)

    fun `test resolve enum variant from module fully qualified`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                   //X
        }
        module 0x1::main {
            fun main(one: 0x1::m::S::One) {
                                   //^
            }
        }        
    """)

    fun `test resolve types with shadowing`() = checkByCode("""
        module 0x1::m {
            struct VestingContract {}
            fun main<VestingContract>() {
                       //X
                let t: VestingContract;
                          //^
            }
        }        
    """)

    fun `test match on unqualified enum variant`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                    //X
            fun main(s: S): bool {
                match (s) {
                    One => true
                   //^ 
                }
            }
        }        
    """)

    fun `test match on unqualified enum variant ref`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                    //X
            fun main(s: &S): bool {
                match (s) {
                    One => true
                   //^ 
                }
            }
        }        
    """)

    fun `test match on unqualified enum variant mut ref`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                    //X
            fun main(s: &mut S): bool {
                match (s) {
                    One => true
                   //^ 
                }
            }
        }        
    """)

    fun `test match on qualified enum variant`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                    //X
            fun main(s: S): bool {
                match (s) {
                    S::One => true
                      //^ 
                }
            }
        }        
    """)

    fun `test match on unqualified enum variant with fields`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                    //X
            fun main(s: S): bool {
                match (s) {
                    One { field: _ } => true
                   //^ 
                }
            }
        }        
    """)

    fun `test match with guard`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
            fun consume() {}
               //X
            fun main(s: S): bool {
                match (s) {
                    S::One if consume() => true
                              //^
                }
            }
        }        
    """)

    fun `test resolve match expr enum variant with presence of another of the same name 1`() = checkByCode("""
        module 0x1::m {
            enum S1 { One, Two }
                     //X
            enum S2 { One, Two }
            fun main(s: S1) {
                match (s) {
                    One => true,
                    //^ 
                }
            }
        }        
    """)

    fun `test resolve match expr enum variant with presence of another of the same name 2`() = checkByCode("""
        module 0x1::m {
            enum S1 { One { field: u8 }, Two }
                     //X
            enum S2 { One { field: u8 }, Two }
            fun main(s: S1) {
                match (s) {
                    One { field } => field,
                    //^ 
                }
            }
        }        
    """)

    fun `test unresolved enum variant if argument type does not have this variant 1`() = checkByCode("""
        module 0x1::m {
            enum S1 { One, Two }
            enum S2 {}
            fun main(s: S2) {
                match (s) {
                    One => true,
                    //^ unresolved
                }
            }
        }        
    """)

    fun `test unresolved enum variant if argument type does not have this variant 2`() = checkByCode("""
        module 0x1::m {
            enum S1 { One { field: u8 }, Two }
            enum S2 {}
            fun main(s: S2) {
                match (s) {
                    One { field } => true,
                    //^ unresolved
                }
            }
        }        
    """)

    fun `test resolve match variant with reference`() = checkByCode("""
        module 0x1::m {
            enum Outer { None }
                        //X
        
            public fun non_exhaustive(o: &Outer) {
                match (o) {
                    None => {}
                    //^
                }
            }
        }        
    """)

    fun `test resolve match variant with reference struct pat`() = checkByCode("""
        module 0x1::m {
            enum Outer { None { i: u8 } }
                        //X
        
            public fun non_exhaustive(o: &Outer) {
                match (o) {
                    None { _ } => {}
                    //^
                }
            }
        }        
    """)

    fun `test resolve match variant with reference and nested struct pat field decl`() = checkByCode("""
        module 0x1::m {
            struct Inner { field: u8 }
                             //X
            enum Outer { One { inner: Inner } }
            
            public fun non_exhaustive(o: &Outer) {
                match (o) {
                    One { inner: Inner { field: myfield } } => myfield
                                        //^
                }
            }
        }        
    """)

    fun `test resolve match variant with reference and nested struct pat field variable`() = checkByCode("""
        module 0x1::m {
            struct Inner { field: u8 }
            enum Outer { One { inner: Inner } }
            
            public fun non_exhaustive(o: &Outer) {
                match (o) {
                    One { inner: Inner { field: myfield } }
                                               //X
                        => myfield
                            //^
                }
            }
        }        
    """)

    fun `test resolve match variant with reference and nested struct pat struct`() = checkByCode("""
        module 0x1::m {
            struct Inner { field: u8 }
                  //X
            enum Outer { One { inner: Inner } }
            
            public fun non_exhaustive(o: &Outer) {
                match (o) {
                    One { inner: Inner { field } } => field
                                 //^
                }
            }
        }        
    """)

    fun `test resolve match variant with reference and nested enum`() = checkByCode("""
        module 0x1::m {
            enum Inner { Inner1, Inner2 }
                  //X
            enum Outer { One { inner: Inner } }
            
            public fun non_exhaustive(o: &Outer) {
                match (o) {
                    One { inner: Inner::Inner1 } => field
                                 //^
                }
            }
        }        
    """)

    fun `test resolve match variant with reference and nested enum variant`() = checkByCode("""
        module 0x1::m {
            enum Inner { Inner1, Inner2 }
                        //X
            enum Outer { One { inner: Inner } }
            
            public fun non_exhaustive(o: &Outer) {
                match (o) {
                    One { inner: Inner::Inner1 } => field
                                        //^
                }
            }
        }        
    """)

    fun `test resolve match variant with reference and nested enum variant no qualifier`() = checkByCode("""
        module 0x1::m {
            enum Inner { Inner1, Inner2 }
                        //X
            enum Outer { One { inner: Inner } }
            
            public fun non_exhaustive(o: &Outer) {
                match (o) {
                    One { inner: Inner1 } => Inner1
                                 //^
                }
            }
        }        
    """)

    fun `test enum variant cannot be resolved on the right side of match arm`() = checkByCode("""
        module 0x1::m {
            enum Inner { Inner1, Inner2 }
                        //X
            enum Outer { One { inner: Inner } }
            
            public fun non_exhaustive(o: &Outer) {
                match (o) {
                    One { inner: Inner1 } => Inner1
                                             //^ unresolved
                }
            }
        }        
    """)

    fun `test resolve expr with enum variant`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                   //X
            fun main() {
                let a: S = One;
                          //^
            }
        }        
    """)

    fun `test resolve expr with enum variant no expected type`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
            fun main() {
                let a = One;
                       //^ unresolved
            }
        }        
    """)

    fun `test resolve expr with enum variant fq`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                   //X
            fun main() {
                let a: S = S::One;
                             //^
            }
        }        
    """)

    fun `test resolve expr with enum variant with explicit type but ambiguous`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                   //X
            enum T { One, Two }
            fun main() {
                let a: S = One;
                          //^
            }
        }        
    """)

    fun `test resolve struct lit with enum variant`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                   //X
            fun main() {
                let a: S = One { field: 1 };
                         //^
            }
        }        
    """)

    fun `test resolve struct lit with enum variant fq`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                   //X
            fun main() {
                let a: S = S::One { field: 1 };
                             //^
            }
        }        
    """)

    fun `test resolve struct lit with enum variant ambiguous`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                   //X
            enum T { One { field: u8 }, Two }
            fun main() {
                let a: S = One { field: 1 };
                          //^
            }
        }        
    """)

    fun `test resolve type of field with alias`() = checkByCode("""
        module 0x1::m {
            struct S { field: u8 }
        }
        module 0x1::main {
            use 0x1::m::S as MyS;
                            //X
            struct R { field: MyS }
                             //^
        }        
    """)

    fun `test resolve tuple struct as a name`() = checkByCode("""
        module 0x1::m {
            struct S(u8);
                 //X
            fun main() {
                S(1);
              //^  
            }
        }        
    """)

    fun `test resolve enum variant tuple struct`() = checkByCode("""
        module 0x1::m {
            enum S { One(u8), Two }
                    //X
            fun main() {
                S::One(1);
                  //^  
            }
        }        
    """)

    fun `test resolve enum variant in call position even it is not a tuple struct`() = checkByCode("""
        module 0x1::m {
            enum S { One(u8), Two }
                             //X
            fun main() {
                S::Two(1);
                  //^
            }
        }        
    """)

    fun `test resolve enum variant in is expr`() = checkByCode("""
        module 0x1::m {
            enum S1 { One, Two }
                     //X  
            enum S2 { One, Two }
            fun main(s: S1) {
                if (s is One) true;
                        //^
            }
        }        
    """)

    fun `test resolve enum variant in is expr with or`() = checkByCode("""
        module 0x1::m {
            enum S1 { One, Two }
                          //X  
            enum S2 { One, Two }
            fun main(s: S1) {
                s is One | Two;
                          //^
            }
        }        
    """)

    fun `test cannot resolve enum variant in is expr for different enum type`() = checkByCode("""
        module 0x1::m {
            enum S1 { One, Two }
            enum S2 {  }
            fun main(s: S2) {
                s is One;
                    //^ unresolved
            }
        }        
    """)

    fun `test resolve enum variant in let expr if explicit type is provided`() = checkByCode("""
        module 0x1::m {
            enum S1 { One, Two }
                     //X  
            enum S2 { One, Two }
            fun main(_: S1) {
                let s: S1 = One;
                           //^
            }
        }        
    """)

    fun `test cannot resolve enum variant in let expr for different enum type`() = checkByCode("""
        module 0x1::m {
            enum S1 { One, Two }
            enum S2 { }
            fun main(_: S1) {
                let s: S2 = One;
                           //^ unresolved
            }
        }        
    """)

    fun `test resolve enum item of resource index expr`() = checkByCode("""
        module 0x1::m {
            enum Ss has key { Empty }
               //X
            fun main() {
                &mut Ss[@0x1];
                   //^
            }
        }        
    """)

    fun `test resolve struct item of resource index expr`() = checkByCode("""
        module 0x1::m {
            struct Ss has key { val: u8 }
                  //X
            fun main() {
                &mut Ss[@0x1];
                   //^
            }
        }        
    """)

    fun `test cannot resolve enum value by itself with no type guidance in expr`() = checkByCode("""
        module 0x1::m {
            enum Ss { One }
            fun main() {
                One;
              //^ unresolved  
            }
        }        
    """)

    fun `test cannot resolve enum value by itself with no type guidance in type`() = checkByCode("""
        module 0x1::m {
            enum Ss { One }
            fun main(s: One) {
                        //^ unresolved  
            }
        }        
    """)
}
