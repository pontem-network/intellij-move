package org.move.lang.types

import org.move.utils.tests.types.TypificationTestCase

class ExpressionTypesTest: TypificationTestCase() {
    fun `test add expr`() = testExpr("""
    script {
        fun main() {
            (1u8 + 1u8);
          //^ u8
        }
    }    
    """)

    fun `test sub expr`() = testExpr("""
    script {
        fun main() {
            (1u8 - 1u8);
          //^ u8
        }
    }    
    """)

    fun `test mul expr`() = testExpr("""
    script {
        fun main() {
            (1u8 * 1u8);
          //^ u8
        }
    }    
    """)

    fun `test div expr`() = testExpr("""
    script {
        fun main() {
            (1u8 / 1u8);
          //^ u8
        }
    }    
    """)

    fun `test mod expr`() = testExpr("""
    script {
        fun main() {
            (1u8 % 10);
          //^ u8
        }
    }    
    """)

    fun `test bang expr`() = testExpr("""
    script {
        fun main() {
            !true;
          //^ bool
        }
    }    
    """)

    fun `test less expr`() = testExpr("""
    script {
        fun main() {
            (1 < 1);
          //^ bool
        }
    }    
    """)

    fun `test less equal expr`() = testExpr("""
    script {
        fun main() {
            (1 <= 1);
          //^ bool
        }
    }    
    """)

    fun `test greater expr`() = testExpr("""
    script {
        fun main() {
            (1 > 1);
          //^ bool
        }
    }    
    """)

    fun `test greater equal expr`() = testExpr("""
    script {
        fun main() {
            (1 >= 1);
          //^ bool
        }
    }    
    """)

    fun `test cast expr`() = testExpr("""
    script {
        fun main() {
            (1 as u8);
          //^ u8
        }
    }    
    """)

    fun `test copy expr`() = testExpr("""
    script {
        fun main() {
            copy 1u8;
          //^ u8
        }
    }    
    """)

    fun `test move expr`() = testExpr("""
    script {
        fun main() {
            move 1u8;
          //^ u8
        }
    }    
    """)

    fun `test struct literal expr with unresolved type param`() = testExpr("""
    module 0x1::M {
        struct R<CoinType> {}
        fun main() {
            R {};
          //^ 0x1::M::R<CoinType>  
        }
    }
    """)

    fun `test borrow expr`() = testExpr("""
    module 0x1::M {
        fun main(s: signer) {
            &s;
          //^ &signer 
        }
    }    
    """)

    fun `test mutable borrow expr`() = testExpr("""
    module 0x1::M {
        fun main(s: signer) {
            &mut s;
          //^ &mut signer 
        }
    }    
    """)

    fun `test deref expr`() = testExpr("""
    module 0x1::M {
        fun main(s: &signer) {
            *s;
          //^ signer 
        }
    }    
    """)

    fun `test dot access to primitive field`() = testExpr("""
    module 0x1::M {
        struct S { addr: address }
        fun main() {
            let s = S { addr: 0x1 };
            ((&s).addr);
          //^ address 
        }
    }    
    """)

    fun `test dot access to field with struct type`() = testExpr("""
    module 0x1::M {
        struct Addr {}
        struct S { addr: Addr }
        fun main() {
            let s = S { addr: Addr {} };
            ((&s).addr);
          //^ 0x1::M::Addr 
        }
    }    
    """)

    fun `test borrow expr of dot access`() = testExpr("""
    module 0x1::M {
        struct Addr {}
        struct S { addr: Addr }
        fun main() {
            let s = S { addr: Addr {} };
            &mut s.addr;
          //^ &mut 0x1::M::Addr 
        }
    }    
    """)

    fun `test add expr with untyped and typed integer`() = testExpr("""
    module 0x1::M {
        fun main() {
            (1 + 1u8);
          //^ u8  
        }
    }    
    """)

    fun `test add expr with untyped and typed integer reversed`() = testExpr("""
    module 0x1::M {
        fun main() {
            (1u8 + 1);
          //^ u8  
        }
    }    
    """)

    fun `test struct field as vector`() = testExpr("""
    module 0x1::M {
        struct NFT {}
        struct Collection { nfts: vector<NFT> }
        fun m(coll: Collection) {
            (coll.nfts)
          //^ vector<0x1::M::NFT>  
        }
    }    
    """)
}
