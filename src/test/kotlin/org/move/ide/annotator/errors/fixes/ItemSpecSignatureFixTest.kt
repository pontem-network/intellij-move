package org.move.ide.annotator.errors.fixes

import org.move.ide.annotator.MvErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class ItemSpecSignatureFixTest : AnnotatorTestCase(MvErrorAnnotator::class) {
    fun `test add signature for item one param`() = checkFixByText(
        "Fix item signature", """
module 0x1::mod {
    fun call(a: address) {}
}        
spec 0x1::mod {
    spec <warning descr="Function signature mismatch">/*caret*/call()</warning> {}
}
    """, """
module 0x1::mod {
    fun call(a: address) {}
}        
spec 0x1::mod {
    spec call(a: address) {}
}
    """
    )

    fun `test add signature for item one invalid param`() = checkFixByText(
        "Fix item signature", """
module 0x1::mod {
    fun call(a: address) {}
}        
spec 0x1::mod {
    spec <warning descr="Function signature mismatch">/*caret*/call(a: &signer)</warning> {}
}
    """, """
module 0x1::mod {
    fun call(a: address) {}
}        
spec 0x1::mod {
    spec call(a: address) {}
}
    """
    )

    fun `test add signature for item one type param no bounds`() = checkFixByText(
        "Fix item signature", """
module 0x1::mod {
    fun call<T>() {}
}        
spec 0x1::mod {
    spec <warning descr="Function signature mismatch">/*caret*/call()</warning> {}
}
    """, """
module 0x1::mod {
    fun call<T>() {}
}        
spec 0x1::mod {
    spec call<T>() {}
}
    """
    )

    fun `test add signature for item one spec param with bounds`() = checkFixByText(
        "Fix item signature", """
module 0x1::mod {
    fun call<T: drop>() {}
}        
spec 0x1::mod {
    spec <warning descr="Function signature mismatch">/*caret*/call()</warning> {}
}
    """, """
module 0x1::mod {
    fun call<T: drop>() {}
}        
spec 0x1::mod {
    spec call<T: drop>() {}
}
    """
    )
}
