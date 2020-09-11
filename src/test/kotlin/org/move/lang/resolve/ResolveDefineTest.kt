package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveDefineTest: ResolveTestCase() {
    fun `test resolve define in spec module`() = checkByCode("""
        module M {
            spec module {
                define reserve_exists(): bool {
                     //X
                   exists<Reserve>(CoreAddresses::CURRENCY_INFO_ADDRESS())
                }
                
                invariant [global] LibraTimestamp::is_operating() ==> reserve_exists()
                                                                    //^
            }
        }
    """)
}