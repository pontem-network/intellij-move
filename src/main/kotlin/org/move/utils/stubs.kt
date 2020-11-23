package org.move.utils

import com.intellij.psi.stubs.StubInputStream

fun StubInputStream.readNameAsString(): String? = readName()?.string