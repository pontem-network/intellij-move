package org.move.ide.docs

import org.move.utils.tests.MoveV2
import org.move.utils.tests.MvDocumentationProviderTestCase

class MvDocumentationProviderTest : MvDocumentationProviderTestCase() {
    fun `test show docs for move_from`() = doTest("""
    module 0x1::M {
        fun m() {
            move_from();
          //^  
        }
    }
    """, expected = """
<div class='definition'><pre>0x0::builtins
<span style="...">native</span> <span style="...">fun</span> <span style="...">move_from</span>&lt;<span style="...">T</span>: <span style="...">key</span>&gt;(addr: <span style="...">address</span>): <span style="...">T</span></pre></div>
<div class='content'><p>Removes <code>T</code> from address and returns it. 
Aborts if address does not hold a <code>T</code>.</p></div>
        """)

    fun `test show doc comment for module`() = doTest("""
    /// module docstring
    module 0x1::M {}
              //^   
    """, expected = """
<div class='definition'><pre><span style="...">module</span> 0x1::M</pre></div>
<div class='content'><p>module docstring</p></div>
        """
    )

    fun `test show doc comment for const`() = doTest("""
    module 0x1::M {
        /// const docstring
        const ERR_COLLECTION_IS_ALREADY_EXISTS: u64 = 0x1;
            //^
    }    
    """, expected = """
<div class='definition'><pre>0x1::M
<span style="...">const</span> <span style="...">ERR_COLLECTION_IS_ALREADY_EXISTS</span>: <span style="...">u64</span> = <span style="...">0x1</span></pre></div>
<div class='content'><p>const docstring</p></div>
    """)

    fun `test show doc comments and signature for function`() = doTest("""
    module 0x1::M {
        /// Adds two numbers.
        /// Returns their sum.
        fun add(a: u8, b: u8): u8 {
          //^
          a + b
        }
    }
    """, expected = """
<div class='definition'><pre>0x1::M
<span style="...">fun</span> <span style="...">add</span>(a: <span style="...">u8</span>, b: <span style="...">u8</span>): <span style="...">u8</span></pre></div>
<div class='content'><p>Adds two numbers.</p><p>Returns their sum.</p></div>
    """)

    fun `test show signature for function parameter`() = doTest("""
    module 0x1::M {
        fun add(a: u8, b: u8): u8 {
          a
        //^  
        }
    }
    """, expected = """
        <div class='definition'><pre><span style="...">value parameter</span> a: <span style="...">u8</span></pre></div>
    """)

    fun `test show signature for type parameter`() = doTest("""
    module 0x1::M {
        fun move_r<R: store + drop>(res: R): R {
                                       //^
        }
    }
    """, expected = """
<div class='definition'><pre><span style="...">type parameter</span> R: <span style="...">store</span> + <span style="...">drop</span></pre></div>
    """)

    fun `test show signature for simple let variable`() = doTest("""
    module 0x1::M {
        fun m() {
          let a: vector<u8>;
          a;
        //^ 
        }
    }
    """, expected = """
<div class='definition'><pre><span style="...">variable</span> a: <span style="...">vector</span>&lt;<span style="...">u8</span>&gt;</pre></div>
    """)

    fun `test struct docstring`() = doTest("""
    module 0x1::M {
        /// docstring
        struct S<R: store, phantom PH> has copy, drop, store {}
        fun m() {
            S { };
          //^  
        }
    }    
    """, expected = """
<div class='definition'><pre>0x1::M
<span style="...">struct</span> <span style="...">S</span>&lt;<span style="...">R</span>: <span style="...">store</span>, <span style="...">phantom</span> <span style="...">PH</span>&gt; <span style="...">has</span> <span style="...">copy</span>, <span style="...">drop</span>, <span style="...">store</span></pre></div>
<div class='content'><p>docstring</p></div>
    """)

    fun `test struct field as vector`() = doTest(
        """
    module 0x1::M {
        struct NFT {}
        struct Collection has key {
            /// docstring
            nfts: vector<NFT> 
        }
        fun m() acquires Collection {
            let coll = borrow_global_mut<Collection>(@0x1);
            coll.nfts;
               //^
        }
    }    
    """, expected = """
<div class='definition'><pre>0x1::M::Collection
<span style="...">nfts</span>: <span style="...">vector</span>&lt;NFT&gt;</pre></div>
<div class='content'><p>docstring</p></div>
        """
    )

    fun `test function signature with return generic`() = doTest("""
module 0x1::main {
    struct Box<T> has copy, drop, store { x: T }
    struct Box3<T> has copy, drop, store { x: Box<Box<T>> }
    
    fun box3<T>(x: T): Box3<T> {
       //^
        Box3 { x: Box { x: Box { x } } }
    }
}        
    """, expected = """
<div class='definition'><pre>0x1::main
<span style="...">fun</span> <span style="...">box3</span>&lt;<span style="...">T</span>&gt;(x: <span style="...">T</span>): Box3&lt;<span style="...">T</span>&gt;</pre></div>
    """)

    fun `test result type documentation`() = doTest("""
module 0x1::m {
    fun call(): u8 {}
    spec call {
        ensures result == 1;
                //^ 
    }
}        
    """, """
<div class='definition'><pre><span style="...">value parameter</span> result: <span style="...">num</span></pre></div>
    """)

    fun `test generic result type documentation`() = doTest("""
module 0x1::m {
    fun call<T>(): &mut T {}
    spec call {
        ensures result == 1;
                //^ 
    }
}        
    """, """
<div class='definition'><pre><span style="...">value parameter</span> result: &<span style="...">mut</span> <span style="...">T</span></pre></div>
    """)

    @MoveV2
    fun `test enum`() = doTest("""
        module 0x1::m {
            /// enum S documentation
            enum S<T> { Inner(T), Outer(T) }        
               //^ 
        }
    """, """
<div class='definition'><pre>0x1::m
<span style="...">enum</span> <span style="...">S</span>&lt;<span style="...">T</span>&gt;</pre></div>
<div class='content'><p>enum S documentation</p></div>
    """)

    @MoveV2
    fun `test enum variant`() = doTest("""
        module 0x1::m {
            enum S<T> { 
                /// i am a well documented enum variant
                Inner(T), 
                Outer(T) 
            }
            fun main() {
                let _ = S::Inner(1);
                            //^    
            }                    
        }
    """, """
<div class='definition'><pre>0x1::m::S::<span style="...">Inner</span></pre></div>
<div class='content'><p>i am a well documented enum variant</p></div>
    """)

    fun `test function docs through spec reference`() = doTest("""
        module 0x1::m {
            /// main function
            fun main() {}
        }
        spec 0x1::m {
            spec main {}
                 //^
        }
    """, """
<div class='definition'><pre>0x1::m
<span style="...">fun</span> <span style="...">main</span>()</pre></div>
<div class='content'><p>main function</p></div>
    """)

    fun `test struct docs through spec reference`() = doTest("""
        module 0x1::m {
            /// main struct
            struct S { val: u8 }
        }
        spec 0x1::m {
            spec S {}
               //^
        }
    """, """
        <div class='definition'><pre>0x1::m
        <span style="...">struct</span> <span style="...">S</span></pre></div>
        <div class='content'><p>main struct</p></div>
    """)

    fun `test spec fun docs`() = doTest("""
        module 0x1::m {
        }
        spec 0x1::m {
            /// my specification function
            spec fun ident(x: u8): u8 { x }
                    //^
        }
    """, """
<div class='definition'><pre>0x1::m
<span style="...">spec</span> <span style="...">fun</span> <span style="...">ident</span>(x: <span style="...">num</span>): <span style="...">num</span></pre></div>
<div class='content'><p>my specification function</p></div>
""")

    // todo: add context support
    fun `test inline spec fun docs`() = doTest("""
        module 0x1::m {
            spec module {
                /// my inline spec fun
                fun inline_spec_fun();
                      //^
            }
        }
    """, """
<div class='definition'><pre>0x1::m
<span style="...">fun</span> <span style="...">inline_spec_fun</span>()</pre></div>
    """)

    fun `test schema docs`() = doTest("""
        module 0x1::m {
        }
        spec 0x1::m {
            /// my schema
            spec schema CreateAccountAbortsIf<T> {
                        //^
                addr: address;
                val: T;
            }
        }
    """, """
<div class='definition'><pre>0x1::m
<span style="...">spec</span> <span style="...">schema</span> CreateAccountAbortsIf&lt;<span style="...">T</span>&gt;</pre></div>
<div class='content'><p>my schema</p></div>
    """)

    fun `test lambda parameter`() = doTest("""
        module 0x1::m {
            fun main() {
                let f = |m: u8| {
                    m;
                  //^  
                };
            }
        }
    """, """
<div class='definition'><pre><span style="...">lambda parameter</span> m: <span style="...">u8</span></pre></div>
    """)

    fun `test lambda parameter uninferred`() = doTest("""
        module 0x1::m {
            fun main() {
                let f = |m| {
                    m;
                  //^  
                };
            }
        }
    """, """
<div class='definition'><pre><span style="...">lambda parameter</span> m: &lt;unknown&gt;</pre></div>

    """)
}
