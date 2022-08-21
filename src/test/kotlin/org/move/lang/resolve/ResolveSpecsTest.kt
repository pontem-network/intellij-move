package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveSpecsTest: ResolveTestCase() {
    fun `test resolve spec to function`() = checkByCode("""
    module 0x1::M {
        fun length(): u8 { 1 }
           //X
        spec length {
             //^   
        }
    }
    """)

    fun `test resolve spec to struct`() = checkByCode("""
    module 0x1::M {
        struct S {}
             //X
        spec S {
           //^   
        }
    }
    """)

    fun `test resolve schema element to schema`() = checkByCode("""
    module 0x1::M {
        spec module {
            include MySchema;
                     //^
        }
        spec schema MySchema {}
                    //X
    }    
    """)

    fun `test resolve spec variable to function param`() = checkByCode("""
    module 0x1::M {
        fun call(count: u8) {}
                //X
        spec call {
            requires count > 1;
                    //^
        }
    }    
    """)

    fun `test resolve spec type param to function type param`() = checkByCode("""
    module 0x1::M {
        fun m<CoinType>() {}
             //X
        spec m {
            ensures exists<CoinType>(@0x1);
                           //^
        }
    }    
    """)

    fun `test spec call to spec fun`() = checkByCode("""
    module 0x1::M {
        fun call() {}
        spec call {
            ensures spec_exists();
                    //^
        }
        spec fun spec_exists() { true }
                //X
    }    
    """)

    fun `test spec fun call to spec fun`() = checkByCode("""
    module 0x1::M {
        spec fun call() {
            spec_exists();
                //^
        }
        spec fun spec_exists() { true }
                //X
    }    
    """)

    fun `test spec fun parameters`() = checkByCode("""
    module 0x1::M {
        spec fun call(val: num) {
                     //X
            val;
           //^ 
        }
    }    
    """)

    fun `test spec fun type parameters`() = checkByCode("""
    module 0x1::M {
        spec fun call<TypeParam>() {
                        //X
            spec_exists<TypeParam>();
                       //^
        }
        spec fun spec_exists<TypeParam>() { true }
    }    
    """)

    fun `test spec call to module fun`() = checkByCode("""
    module 0x1::M {
        fun mod_exists(): bool { true }
           //X
        fun call() {}
        spec call {
            ensures mod_exists();
                    //^
        }
    }    
    """)

    fun `test resolve spec variable to let statement`() = checkByCode("""
    module 0x1::M {
        fun call() {}
        spec call {
            let count = 1;
               //X
            requires count > 1;
                    //^
        }
    }
    """)

    fun `test resolve spec variable to let statement in any order`() = checkByCode("""
    module 0x1::M {
        fun call() {}
        spec call {
            requires count > 1;
                    //^
            let count = 1;
               //X
        }
    }
    """)

    fun `test resolve from let statement to another let statement`() = checkByCode("""
    module 0x1::M {
        fun call() {}
        spec call {
            let count = 1;
               //X
            let count2 = count + 1;
                       //^
        }
    }
    """)

    fun `test resolve let post sees pre`() = checkByCode("""
    module 0x1::M {
        fun call() {}
        spec call {
            let count = 1;
               //X
            let post count2 = count + 1;
                             //^ 
        }
    }
    """)

    fun `test resolve let post sees post`() = checkByCode("""
    module 0x1::M {
        fun call() {}
        spec call {
            let post count = 1;
                    //X
            let post count2 = count + 1;
                             //^ 
        }
    }
    """)


    fun `test resolve let pre does not see let post`() = checkByCode("""
    module 0x1::M {
        fun call() {}
        spec call {
            let post count = 1;
            let count2 = count + 1;
                          //^ unresolved
        }
    }
    """)

    fun `test unresolved if let declared after the first let`() = checkByCode("""
    module 0x1::M {
        fun call() {}
        spec call {
            let count2 = count + 1;
                        //^ unresolved
            let count = 1;
        }
    }
    """)

    fun `test schema vars in schema`() = checkByCode("""
    module 0x1::M {
        spec schema MySchema {
            var1: address;
            //X
            var1;
           //^  
        }
    }    
    """)

    fun `test schema vars in schema reverse order`() = checkByCode("""
    module 0x1::M {
        spec schema MySchema {
            var1;
           //^  
            var1: address;
            //X
        }
    }    
    """)

    fun `test resolve schema from another module`() = checkByCode("""
    module 0x1::M {
        spec schema MySchema {}
                   //X
    }
    module 0x1::M2 {
        use 0x1::M;
        
        spec module {
            include M::MySchema;
                      //^
        }
    }
    """)

    fun `test resolve spec fun from another module`() = checkByCode("""
    module 0x1::M {
        spec fun myfun(): bool { true }
               //X
    }
    module 0x1::M2 {
        use 0x1::M;
        spec module {
            M::myfun();
              //^
        }
    }
    """)

    fun `test another module consts are accessible from msl`() = checkByCode("""
    module 0x1::M {
        const MY_CONST: u8 = 1;
              //X
    }    
    module 0x1::M2 {
        use 0x1::M;
        spec module {
            M::MY_CONST;
                 //^            
        }
    }
    """)

    fun `test resolve schema parameters`() = checkByCode("""
    module 0x1::M {
        spec module {
            let a = @0x1;
            include MySchema { addr: a };
                              //^
        }
        
        spec schema MySchema {
            addr: address;
            //X
        }
    }    
    """)

    fun `test dot access resolution for spec parameters`() = checkByCode("""
    module 0x1::M {
        struct S { val: u8 }
                 //X
        spec schema SS {
            s: S;
            s.val;
            //^
        }
    }    
    """)

    fun `test spec struct resolves to field`() = checkByCode("""
    module 0x1::M {
        struct S { val: u8 }
                  //X
        spec S {
            invariant val > 1;
                     //^ 
        }
    }    
    """)

    fun `test forall binding`() = checkByCode("""
    module 0x1::M {
        spec module {
            invariant forall ind in 0..len(bytes)
                           //X
                : ind != 0;
                //^
        }
    }    
    """)

    fun `test exists binding`() = checkByCode("""
    module 0x1::M {
        spec module {
            invariant exists addr: address
                           //X
                : addr != @0x1;
                //^
        }
    }    
    """)

    fun `test resolve schema in apply`() = checkByCode("""
    module 0x1::M {
        spec schema MySchema {}
                    //X
        spec module {
            apply MySchema to *;
                  //^
        }
    }    
    """)

    fun `test resolve schema type parameter in apply`() = checkByCode("""
    module 0x1::M {
        spec schema SS<Type> {}
        spec module {
            apply SS<Type>
                     //^
                to *<Type>;
                     //X
        }
    }    
    """)

    fun `test fun can be defined right in spec`() = checkByCode(
        """
    module 0x1::M {
        fun m() {}
        spec m {
            fun call() {}
               //X
            call();
            //^
        }
    }    
    """
    )

    fun `test fun can be defined in spec module`() = checkByCode("""
    module 0x1::M {
        fun call() {}
        spec call {
            spec_fun()
            //^
        }
        spec module {
            fun spec_fun() {}
               //X
        }
    }    
    """)

    fun `test spec fun from spec module reachable in spec module`() = checkByCode("""
    module 0x1::M {
        spec module {
            fun call() {}
               //X
            fun m() {
                call();
                //^
            }
        }
    }    
    """)

    fun `test schema lit with imply operator`() = checkByCode("""
    module 0x1::M {
        spec schema MySchema {}
                     //X
        spec module {
            include true ==> MySchema;
                              //^
        }
    }    
    """)

    fun `test resolve module name in spec module`() = checkByCode("""
    module 0x1::Module {
                //X        
    }    
    spec 0x1::Module {
                //^
    }
    """)

    fun `test resolve function name in spec module`() = checkByCode("""
    module 0x1::Module {
        public fun call() {}
                   //X
    }    
    spec 0x1::Module {
        spec call {}
            //^
    }
    """)

    fun `test resolve struct name in spec module`() = checkByCode("""
    module 0x1::Module {
        struct MyStruct {}
              //X
    }    
    spec 0x1::Module {
        spec MyStruct {}
            //^
    }
    """)

    fun `test spec module has access to all items of module`() = checkByCode("""
    module 0x1::Module {
        fun address_of(addr: address) {}
           //X
    }    
    spec 0x1::Module {
        spec schema MySchema {
            let a = address_of(@0x1);  
                    //^
        }
    }
    """)

    fun `test spec module has access to module imports`() = checkByCode("""
    module 0x1::signer {
              //X
        fun address_of(addr: address) {}
    }     
    module 0x1::Module {
        use 0x1::signer;
    }    
    spec 0x1::Module {
        spec schema MySchema {
            let a = signer::;  
                   //^
        }
    }
    """)

    fun `test spec module has access to item imports`() = checkByCode("""
    module 0x1::signer {
              //X
        fun address_of(addr: address) {}
    }     
    module 0x1::Module {
        use 0x1::signer;
    }    
    spec 0x1::Module {
        spec schema MySchema {
            let a = signer::;  
                   //^
        }
    }
    """)

    fun `test resolve schema from current scope`() = checkByCode("""
    module 0x1::Module {}    
    spec 0x1::Module {
        spec schema MySchema {
                   //X
        }
        spec schema MySchema2 {
            include MySchema;
                     //^
        }
    }
    """)
}
