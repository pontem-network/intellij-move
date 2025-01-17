package org.move.ide.inspections.compilerV2

import org.move.ide.inspections.MvTypeCheckInspection
import org.move.utils.tests.MoveV2
import org.move.utils.tests.annotation.InspectionTestBase

@MoveV2()
class TypeCheckIndexExprTest: InspectionTestBase(MvTypeCheckInspection::class) {

    @MoveV2(enabled = false)
    fun `test no error receiver of vector index expr without compiler v2 feature`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let b = false;
                b[0];
            }
        }        
    """
    )

    fun `test error receiver of vector index expr`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let b = false;
                <error descr="Indexing receiver type should be vector or resource, got 'bool'">b</error>[0];
            }
        }        
    """
    )

    fun `test error receiver of resource index expr`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let b = false;
                <error descr="Indexing receiver type should be vector or resource, got 'bool'">b</error>[@0x1];
            }
        }        
    """
    )

    fun `test vector index expr argument is not an integer`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let v = vector[1, 2];
                v[<error descr="Incompatible type 'bool', expected 'integer'">false</error>];
            }
        }        
    """
    )

    fun `test vector index expr argument is not an integer in spec`() = checkByText(
        """
        module 0x1::m {
            spec fun main(): u8 {
                let v = vector[1, 2];
                v[<error descr="Incompatible type 'bool', expected 'num'">false</error>];
                1
            }
        }        
    """
    )

    fun `test resource index expr argument is not an address`() = checkByText(
        """
        module 0x1::m {
            struct S has key {}
            fun main() {
                S[<error descr="Incompatible type 'bool', expected 'address'">false</error>];
            }
        }        
    """
    )

    fun `test no error for vector reference`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let v = vector[1, 2];
                (&v)[1];
            }
        }        
    """
    )

    fun `test no error for vector reference that is result of function call`() = checkByText(
        """
        module 0x1::m {
            fun call(): &vector<u8> { &vector[1] }
            enum BigOrderedMap<K: store, V: store> has store { BPlusTreeMap }
            public native fun borrow<K: drop + copy + store, V: store>(self: &BigOrderedMap<K, V>, key: &K): &V;
            fun main() {
                let map = BigOrderedMap<vector<u8>, vector<u8>>::BPlusTreeMap;
                borrow(&map, &vector[1])[0];
            }
        }        
    """
    )

    fun `test no error for resource indexing expr of unknown type`() = checkByText("""
        module 0x1::m {
            fun main() {
                &mut PermissionStorage[addr];
            }
        }        
    """)
}