package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionsTestCase

class RedundantQualifiedPathInspectionTest : InspectionsTestCase(RedundantQualifiedPathInspection::class) {
    fun `test no error if fully qualified without imports`() = checkWarnings(
        """
    module 0x1::M {
        public fun call() {}        
    }    
    module 0x1::M2 {
        fun m() {
            0x1::M::call();
        }
    }    
    """
    )

    fun `test no error if qualified with module import`() = checkWarnings(
        """
    module 0x1::M {
        public fun call() {}        
    }    
    module 0x1::M2 {
        use 0x1::M;
        fun m() {
            M2::call();
        }
    }    
    """
    )

    fun `test no error if local name or import`() = checkWarnings(
        """
    module 0x1::M {
        public fun call() {}        
    }    
    module 0x1::M2 {
        use 0x1::M::call;
        fun local_call() {}
        fun m() {
            call();
            local_call();
        }
    }    
    """
    )

    fun `test error if fully qualified path and module imported`() = checkFixByText(
        "Remove redundant qualifier", """
    module 0x1::M {
        public fun call() {}        
    }    
    module 0x1::M2 {
        use 0x1::M;
        fun m() {
            <warning descr="Redundant qualifier">0x1/*caret*/::</warning>M::call();
        }
    }    
    """, """
    module 0x1::M {
        public fun call() {}        
    }    
    module 0x1::M2 {
        use 0x1::M;
        fun m() {
            /*caret*/M::call();
        }
    }    
    """
    )

    fun `test error if fully qualified path and method imported`() = checkFixByText(
        "Remove redundant qualifier", """
    module 0x1::M {
        public fun call() {}
    }    
    module 0x1::M2 {
        use 0x1::M::call;
        fun m() {
            <warning descr="Redundant qualifier">0x1::M/*caret*/::</warning>call();
        }
    }    
    """, """
    module 0x1::M {
        public fun call() {}
    }    
    module 0x1::M2 {
        use 0x1::M::call;
        fun m() {
            /*caret*/call();
        }
    }    
    """
    )

    fun `test error if qualified path and method imported`() = checkFixByText(
        "Remove redundant qualifier", """
    module 0x1::M {
        public fun call() {}
    }    
    module 0x1::M2 {
        use 0x1::M;
        use 0x1::M::call;
        fun m() {
            <warning descr="Redundant qualifier">M/*caret*/::</warning>call();
        }
    }    
    """, """
    module 0x1::M {
        public fun call() {}
    }    
    module 0x1::M2 {
        use 0x1::M;
        use 0x1::M::call;
        fun m() {
            /*caret*/call();
        }
    }    
    """
    )

    fun `test error method imported is higher priority`() = checkFixByText(
        "Remove redundant qualifier", """
    module 0x1::M {
        public fun call() {}
    }        
    module 0x1::M2 {
        use 0x1::M;
        use 0x1::M::call;
        fun m() {
            <warning descr="Redundant qualifier">0x1::M/*caret*/::</warning>call();
        }
    }    
    """, """
    module 0x1::M {
        public fun call() {}
    }        
    module 0x1::M2 {
        use 0x1::M;
        use 0x1::M::call;
        fun m() {
            /*caret*/call();
        }
    }    
    """
    )

    fun `test no redundant qual with Self path`() = checkWarnings("""
    module 0x1::M {
        struct S {}
        fun call() {}
        fun m(a: Self::S) acquires Self::S {
            Self::call();
        }
    }    
    """)
}
