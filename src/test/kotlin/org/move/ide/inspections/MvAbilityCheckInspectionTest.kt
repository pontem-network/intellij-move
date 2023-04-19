package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionTestBase

class MvAbilityCheckInspectionTest: InspectionTestBase(MvAbilityCheckInspection::class) {
    fun `test no required ability for struct for type param`() = checkByText(
        """
    module 0x1::M {
        struct Res {}
        fun save<T: key>(r: T) {}
        fun main(r: Res) {
            save(<error descr="The type 'Res' does not have required ability 'key'">r</error>)
        }
    }    
    """
    )

    fun `test no required ability 'key' for move_to argument`() = checkByText(
        """
    module 0x1::M {
        struct Res {}
        fun main(s: &signer, r: Res) {
            move_to(s, <error descr="The type 'Res' does not have required ability 'key'">r</error>)
        }
    }    
    """
    )

    fun `test pass primitive type to generic with required abilities`() = checkByText(
        """
    module 0x1::M {
        fun balance<Token: key>(k: Token) {}
        fun m() {
            balance(<error descr="The type 'address' does not have required ability 'key'">@0x1</error>);
        }
    }    
    """
    )

    fun `test no ability error in call expr mut ref inference`() = checkByText(
        """
        module 0x1::m {
            struct AVLqueue<V> { }
            fun new<NewV: store>(): AVLqueue<NewV> { AVLqueue<NewV> {}}
            fun compute<ComputeV>(q: &mut AVLqueue<ComputeV>, v: ComputeV) {}
            fun main() {
                let avlq = new();
                compute(&mut avlq, 10u64);
            }
        }        
    """
    )

    fun `test fields of struct should have abilities of struct`() = checkByText(
        """
    module 0x1::M {
        struct A {}
        
        struct B has copy {
            <error descr="The type 'A' does not have the ability 'copy' required by the declared ability 'copy' of the struct 'B'">a: A</error>
        }
    }    
    """
    )

    fun `test key struct requires store fields`() = checkByText(
        """
    module 0x1::M {
        struct A {}
        
        struct B has key {
            <error descr="The type 'A' does not have the ability 'store' required by the declared ability 'key' of the struct 'B'">a: A</error>
        }
    }    
    """
    )

    fun `test store struct requires store fields`() = checkByText(
        """
    module 0x1::M {
        struct A {}
        
        struct B has store {
            <error descr="The type 'A' does not have the ability 'store' required by the declared ability 'store' of the struct 'B'">a: A</error>
        }
    }    
    """
    )

    fun `test copy struct requires copy fields`() = checkByText(
        """
    module 0x1::M {
        struct A {}
        
        struct B has copy {
            <error descr="The type 'A' does not have the ability 'copy' required by the declared ability 'copy' of the struct 'B'">a: A</error>
        }
    }    
    """
    )

    fun `test drop struct requires drop fields`() = checkByText(
        """
    module 0x1::M {
        struct A {}
        
        struct B has drop {
            <error descr="The type 'A' does not have the ability 'drop' required by the declared ability 'drop' of the struct 'B'">a: A</error>
        }
    }    
    """
    )

    fun `test function invocation with explicitly provided generic type`() = checkByText(
        """
    module 0x1::Event {
        struct Message has drop {}
        
        public fun emit_event<T: store + drop>() {}
        
        public fun main() {
            emit_event<<error descr="The type 'Message' does not have required ability 'store'">Message</error>>()
        }
    }    
    """
    )

    fun `test struct constructor with explicitly provided generic type`() = checkByText(
        """
    module 0x1::Event {
        struct Message has drop {}
        
        struct Event<Message: store + drop> {}
        
        public fun main() {
            Event<<error descr="The type 'Message' does not have required ability 'store'">Message</error>> {};
        }
    }    
    """
    )

    fun `test type param`() = checkByText(
        """
    module 0x1::Event {
        struct Message has drop {}
        
        public fun emit_event<T: store + drop>() {}
        
        public fun main<M: drop>() {
            emit_event<<error descr="The type 'M' does not have required ability 'store'">M</error>>()
        }
    }    
    """
    )

    fun `test no error in move_to with resource`() = checkByText(
        """
    module 0x1::M {
        struct Res has key {}
        fun main(s: &signer, r: Res) {
            move_to<Res>(s, r)
        }
    }    
    """
    )

    fun `test no error in type param if structure has required abilities`() = checkByText(
        """
    module 0x1::M {
        struct Res has key {}
        fun save<T: key>(r: T) {}
        fun main(r: Res) {
            save(r)
        }
    }    
    """
    )

    fun `test no error in specs`() = checkByText(
        """
    module 0x1::M {
        fun balance<Token: store>() {}
        spec schema PayFromEnsures<Token> {
            ensures balance<Token>();
        }
    }    
    """
    )
}
