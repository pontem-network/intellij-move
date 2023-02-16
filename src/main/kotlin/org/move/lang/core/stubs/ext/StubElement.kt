package org.move.lang.core.stubs.ext

import com.intellij.psi.stubs.StubElement

inline fun <reified T : StubElement<*>> StubElement<*>.childrenStubsOfType(): List<T> =
    childrenStubs.filterIsInstance<T>()
