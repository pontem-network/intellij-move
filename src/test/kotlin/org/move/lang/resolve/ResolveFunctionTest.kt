package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveFunctionTest: ResolveTestCase() {
    fun `test resolve reference to function`() = checkByCode(
        """
        module 0x1::m {
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
        module 0x1::m {
            native fun call(): u8;
                     //X
            
            fun main() {
                call();
              //^
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

    fun `test resolve function to the same module full path`() = checkByCode(
        """
        address 0x1 {
        module M {
            public fun call() {}
                     //X
            
            fun main() {
                0x1::M::call();
                      //^
            }
        }    
        }
    """
    )

    fun `test resolve function to another module full path`() = checkByCode(
        """
        address 0x1 {
        module Original {
            public fun call() {}
                     //X
        }
        
        module M {
            fun call() {}
            
            fun main() {
                0x1::Original::call();
                             //^
            }
        }    
        }
    """
    )

    fun `test resolve function to another module in the same address block`() = checkByCode(
        """
        address 0x1 {
        module Original {
            public fun call() {}
                     //X
        }
        
        module M {
            use 0x1::Original;
            
            fun call() {}
            
            fun main() {
                Original::call();
                        //^
            }
        }    
        }
    """
    )

    fun `test resolve function from import`() = checkByCode(
        """
        address 0x1 {
        module Original {
            public fun call() {}
                     //X
        }
        
        module M {
            use 0x1::Original::call;
                             //^
        }    
        }
    """
    )

    fun `test cannot resolve private function from import`() = checkByCode(
        """
        address 0x1 {
        module Original {
            fun call() {}
        }
        
        module M {
            use 0x1::Original::call;
                             //^ unresolved
        }    
        }
    """
    )

    fun `test cannot resolve const from item`() = checkByCode(
        """
        address 0x1 {
        module Original {
            const MY_CONST: u8 = 1;
        }
        
        module M {
            use 0x1::Original::MY_CONST;
            fun main() {
                MY_CONST;
                //^ unresolved
            }
        }    
        }
    """
    )

    fun `test resolve through member import`() = checkByCode(
        """
        address 0x1 {
            module Original {
                public fun call() {}
                         //X
            }
        }
        address 0x2 {
            module M {
                use 0x1::Original::call;
                
                fun main() {
                    call();
                  //^  
                }
            }
        }
    """
    )

    fun `test resolve function to import alias`() = checkByCode(
        """
        module 0x1::original {
            public fun call() {}
        }    
        module 0x1::m {
            use 0x1::original::call as mycall;
                                     //X
            fun main() {
                mycall();
              //^  
            }
        }    
    """
    )

    fun `test resolve function qualed with module`() = checkByCode(
        """
        address 0x1 {
            module Original {
                public fun call() {}
                         //X
            }
        }
        address 0x2 {
            module M {
                use 0x1::Original;
                
                fun main() {
                    Original::call();
                            //^  
                }
            }
        }
    """
    )

    fun `test resolve reference to function via Self`() = checkByCode(
        """
        module 0x1::m {
            fun call(): u8 {
              //X
                1
            }
            
            fun main() {
                Self::call();
                    //^
            }
        }
    """
    )

    fun `test resolve friend function`() = checkByCode(
        """
        address 0x1 {
        module Original {
            friend 0x1::M;
            public(friend) fun call() {}
                             //X
        }
        
        module M {
            use 0x1::Original;
            fun main() {
                Original::call();
                        //^
            }
        }    
        }
    """
    )

    fun `test script function is unresolved in friend modules`() = checkByCode(
        """
        address 0x1 {
        module Original {
            friend 0x1::M;
            public(script) fun call() {}
        }
        
        module M {
            use 0x1::Original;
            fun main() {
                Original::call();
                        //^ unresolved
            }
        }    
        }
    """
    )

    fun `test resolve script function`() = checkByCode(
        """
        address 0x1 {
        module Original {
            public(script) fun call() {}
                             //X
        }
        }
        
        script {
            use 0x1::Original;
            fun main() {
                Original::call();
                        //^
            }
        }    
    """
    )

    fun `test friend function is unresolved in scripts`() = checkByCode(
        """
        address 0x1 {
        module Original {
            friend 0x1::M;
            public(friend) fun call() {}
        }
        
        module M {}    
        }
        
        script {
            use 0x1::Original;
            fun main() {
                Original::call();
                        //^ unresolved
            }
        } 
    """
    )

    fun `test public(script) function can call public(script) from another module`() = checkByCode(
        """
        address 0x1 {
        module Original {
            public(script) fun call() {}
                             //X
        }
        
        module M {
            use 0x1::Original;
            public(script) fun main() {
                Original::call();
                        //^
            }
        }
        }
    """
    )

    fun `test resolve fully qualified path from the same module`() = checkByCode(
        """
    module 0x1::M {
        struct Loan {}
        public fun call() acquires Loan {}
                 //X
        fun main() {
            0x1::M::call();
                    //^  
        }
    }
    """
    )

    fun `test public script function available in test`() = checkByCode(
        """
    module 0x1::M {
        public(script) fun call() {}
                           //X
    }    
    #[test_only]
    module 0x1::Tests {
        use 0x1::M::call;
        
        #[test]
        public(script) fun test_1() {
            call();
            //^
        }
    }
    """
    )

    fun `test resolve fun in test_only module from another test_only`() = checkByCode(
        """
    #[test_only] 
    module 0x1::M1 {
        public fun call() {}
                  //X
    }   
    #[test_only] 
    module 0x1::M2 {
        use 0x1::M1::call;
        
        fun main() {
            call();
            //^
        }
    }
    """
    )

    fun `test test_only function is not accessible from non test_only module`() = checkByCode(
        """
    module 0x1::M1 {
        #[test_only]
        public fun call() {}
    }        
    module 0x1::M2 {
        use 0x1::M1;
        fun call() {
            M1::call();
               //^ unresolved             
        }
    }
    """
    )

    fun `test test_only module is not accessible from non_test_only module`() = checkByCode(
        """
    #[test_only]
    module 0x1::M1 {
        public fun call() {}
    }        
    module 0x1::M2 {
        use 0x1::M1;
               //^ unresolved 
        fun call() {
            M1::call();
        }
    }
    """
    )

    fun `test unittest functions are not accessible as items`() = checkByCode(
        """
    #[test_only]    
    module 0x1::M {
        #[test]
        fun test_a() {}
        fun main() {
            test_a();
           //^ unresolved 
        }
    }    
    """
    )

    fun `test unittest functions are not accessible as module items`() = checkByCode(
        """
    #[test_only]    
    module 0x1::M1 {
        #[test]
        entry fun test_a() {}
    }    
    #[test_only]
    module 0x1::M2 {
        use 0x1::M1; 
        
        entry fun main() {
            M1::test_a();
               //^ unresolved    
        }
    }    
    """
    )

    fun `test module and spec module blocks share the same namespace`() = checkByCode(
        """
    module 0x1::caller {
        public fun call() {}
                  //X
    }
    module 0x1::main {
        public fun main() {
            call();
            //^
        }
    }    
    spec 0x1::main {
        use 0x1::caller::call;
    }
    """
    )

    fun `test friend function from another module with spaces after public`() = checkByCode(
        """
    module 0x1::A {
        friend 0x1::B; 
        public ( friend) fun call_a() {}
                            //X
    }        
    module 0x1::B {
        use 0x1::A;
        
        fun main() {
            A::call_a();
                  //^
        }
    }
    """
    )

    fun `test use item inside function block`() = checkByCode(
        """
module 0x1::string {
    public fun utf8() {}
              //X
}
module 0x1::main {
    fun main() {
        use 0x1::string::utf8;
        utf8();
        //^
    }
}        
    """
    )

    fun `test resolve use item`() = checkByCode(
        """
module 0x1::string {
    public fun utf8() {}
              //X
}
module 0x1::main {
    use 0x1::string::utf8;
                   //^
}        
    """
    )

    fun `test resolve with self alias`() = checkByCode(
        """
module 0x1::string {
    public fun utf8() {}
              //X
}
module 0x1::main {
    use 0x1::string::Self as mystring;
    fun main() {
        mystring::utf8();
                //^
    }
}        
    """
    )

    fun `test resolve inline function from same module`() = checkByCode(
        """
module 0x1::string {
    inline fun foreach<Element>(v: vector<Element>, f: |Element|) {}
              //X
    fun main() {
        foreach(vector[1, 2, 3], |e| print(e))
        //^
    }
}        
    """
    )

    fun `test resolve inline function from different module`() = checkByCode(
        """
module 0x1::string {
    public inline fun foreach<Element>(v: vector<Element>, f: |Element|) {}
                      //X
}
module 0x1::main {
    use 0x1::string::foreach;
    fun main() {
        foreach(vector[1, 2, 3], |e| print(e))
        //^
    }
}
    """
    )

    fun `test inline function lambda variable`() = checkByCode(
        """
module 0x1::m {
    public inline fun for_each<Element>(o: Element, f: |Element|) {}
    fun main() {
        for_each(1, |value|
                     //X
            value
            //^
        )
    }
}        
    """
    )

    fun `test inline function lambda two variables`() = checkByCode(
        """
module 0x1::m {
    public inline fun for_each<Element>(o: Element, f: |Element|) {}
    fun main() {
        for_each(1, |value1, value2|
                           //X
            value2
            //^
        )
    }
}        
    """
    )

    fun `test resolve lambda function call expr`() = checkByCode(
        """
module 0x1::mod {
    public inline fun fold<Accumulator, Element>(elem: Element, func: |Element| Accumulator): Accumulator {
                                                               //X
        func(elem);
        //^
    }
}        
    """
    )

    fun `test cannot resolve function to parameter`() = checkByCode(
        """
module 0x1::mod {
    public inline fun fold<Accumulator, Element>(elem: Element, func: |Element| Accumulator): Accumulator {
        elem(1);
        //^ unresolved
    }
}        
    """
    )

    fun `test functions have separate namespace from variables`() = checkByCode(
        """
module 0x1::mod {
    fun name() {}
       //X
    fun main() {
        let name = 1;
        name();
         //^       
    }
}        
    """
    )

    fun `test resolve local function when module with same name is imported`() = checkByCode(
        """
        module 0x1::royalty {}
        module 0x1::m {
            use 0x1::royalty;
            public fun royalty() {}
                        //X
            public fun main() {
                royalty();
                //^
            }
        }        
    """
    )

    fun `test resolve local function when module imported with the same alias`() = checkByCode(
        """
        module 0x1::myroyalty {}
        module 0x1::m {
            use 0x1::myroyalty as royalty;
            public fun royalty() {}
                        //X
            public fun main() {
                royalty();
                //^
            }
        }        
    """
    )

    fun `test resolve local function when module with same name is imported as Self`() = checkByCode(
        """
        module 0x1::royalty {}
        module 0x1::m {
            use 0x1::royalty::Self;
            public fun royalty() {}
                        //X
            public fun main() {
                royalty();
                //^
            }
        }        
    """
    )

    fun `test cannot resolve local function when duplicate function is imported`() = checkByCode(
        """
        module 0x1::royalty {
            public fun royalty() {}
        }
        module 0x1::m {
            use 0x1::royalty::royalty;
            public fun royalty() {}
            public fun main() {
                royalty();
                //^ unresolved
            }
        }        
    """
    )

    fun `test resolve spec fun defined in module spec`() = checkByCode(
        """
        module 0x1::m {
        }        
        spec 0x1::m {
            spec module {
                fun spec_sip_hash();
                    //X
            }
        }
        module 0x1::main {
            use 0x1::m::spec_sip_hash;
            spec fun main(): u128 {
                spec_sip_hash(); 1
                       //^
            }
        }
    """
    )

    fun `test verify_only function accessible in spec`() = checkByCode(
        """
        module 0x1::m {
            #[verify_only]
            fun call(): u8 { 1 }
               //X
            fun main() {}
            spec main {
                let _ = call();
                       //^
            }
        }        
    """
    )

    fun `test verify_only function accessible in other verify_only function`() = checkByCode(
        """
        module 0x1::m {
            #[verify_only]
            fun call(): u8 { 1 }
               //X
            #[verify_only]
            fun main() {
                let _ = call();
                       //^
            }
        }        
    """
    )

    fun `test verify_only not accessible in the regular code`() = checkByCode(
        """
        module 0x1::m {
            #[verify_only]
            fun call(): u8 { 1 }
            fun main() {
                let _ = call();
                       //^ unresolved
            }
        }        
    """
    )

    fun `test cannot resolve non-test-only import into test-only item`() = checkByCode(
        """
        module 0x1::m {
            #[test_only]
            public fun call() {}
        }        
        module 0x1::main {
            use 0x1::m::call;
                        //^ unresolved
        }
    """
    )

    fun `test resolve local test-only import into test-only item`() = checkByCode(
        """
        module 0x1::m {
            #[test_only]
            public fun call() {}
                      //X
        }        
        module 0x1::main {
            #[test]                        
            fun main() {
            use 0x1::m::call;
                        //^
            }
        }
    """
    )

    fun `test resolve use item spec fun defined in module spec verify scope always available`() = checkByCode(
        """
        module 0x1::m {
        }        
        spec 0x1::m {
            spec module {
                fun spec_sip_hash();
                    //X
            }
        }
        module 0x1::main {
            use 0x1::m::spec_sip_hash;
                       //^
            spec fun main(): u128 {
                spec_sip_hash(); 1
            }
        }
    """
    )

    fun `test resolve function to another module with module alias`() = checkByCode(
        """
    module 0x1::m {
        public fun call() {}
                 //X
    }
    
    module 0x1::main {
        use 0x1::m as m_alias;
        
        fun main() {
            m_alias::call();
                    //^
        }
    }    
    """
    )

    fun `test resolve function to another module with local module alias`() = checkByCode(
        """
    module 0x1::m {
        public fun call() {}
                 //X
    }
    
    module 0x1::main {
        fun main() {
            use 0x1::m as m_alias;
            m_alias::call();
                    //^
        }
    }    
    """
    )

    fun `test function with module alias cannot use original module name`() = checkByCode(
            """
    module 0x1::m {
        public fun call() {}
    }
    
    module 0x1::main {
        use 0x1::m as m_alias;
        
        fun main() {
            m::call();
             //^ unresolved
        }
    }    
    """
        )

    fun `test test function is not available in name resolution`() = checkByCode(
        """
    module 0x1::main {
        public fun call() { test_main(); }
                              //^ unresolved
        #[test]
        fun test_main() {}
    }
    """
    )

    fun `test resolve function from use group in use`() = checkByCode("""
        module 0x1::m {
            public fun call() {}
                      //X
        }        
        module 0x1::main {
            use 0x1::m::{call};
                        //^
        }
    """)

    fun `test resolve function from use group`() = checkByCode("""
        module 0x1::m {
            public fun call() {}
                      //X
        }        
        module 0x1::main {
            use 0x1::m::{call};
            public fun main() {
                call();
                //^
            }
        }
    """)
}
