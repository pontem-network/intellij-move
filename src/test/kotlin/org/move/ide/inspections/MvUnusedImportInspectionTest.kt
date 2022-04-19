package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionsTestCase

class MvUnusedImportInspectionTest: InspectionsTestCase(MvUnusedImportInspection::class) {
    fun `test no error`() = checkWarnings("""
module 0x1::M {
    struct MyItem {}
    struct MyItem2 {}
    public fun call() {}
}        
module 0x1::M2 {
    use 0x1::M::MyItem;
    use 0x1::M::MyItem2;
    use 0x1::M::call;
    fun main(arg: MyItem2) {
        let a: MyItem = call();
    }
}        
    """)

    fun `test error unused item import`() = checkWarnings("""
module 0x1::M {
    struct MyItem {}
    struct MyItem2 {}
    public fun call() {}
}        
module 0x1::M2 {
    <warning descr="Unused use item">use 0x1::M::MyItem;</warning>
    fun main() {}
}
    """)

    fun `test error unused module import`() = checkWarnings("""
module 0x1::M {
    struct MyItem {}
    struct MyItem2 {}
    public fun call() {}
}        
module 0x1::M2 {
    <warning descr="Unused use item">use 0x1::M;</warning>
    fun main() {}
}
    """)

    fun `test unused item in use group`() = checkWarnings("""
module 0x1::M {
    struct MyItem {}
    struct MyItem2 {}
    public fun call() {}
}        
module 0x1::M2 {
    use 0x1::M::{MyItem, <warning descr="Unused use item">MyItem2</warning>};
    fun main(a: MyItem) {}
}
    """)

    fun `test no error if module imported and used as fq`() = checkWarnings("""
module 0x1::M {
    public fun call() {}
}
module 0x1::M2 {
    use 0x1::M;
    fun main() {
        M::call();
    }
}
    """)
}
