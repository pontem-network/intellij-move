package org.move.openapiext

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.DataInputOutputUtil

fun StubInputStream.readNameAsString(): String? = readName()?.string
fun StubInputStream.readUTFFastAsNullable(): String? =
    DataInputOutputUtil.readNullable(this, this::readUTFFast)

fun StubOutputStream.writeUTFFastAsNullable(value: String?) =
    DataInputOutputUtil.writeNullable(this, value, this::writeUTFFast)

fun StubOutputStream.writeLongAsNullable(value: Long?) =
    DataInputOutputUtil.writeNullable(this, value, this::writeLong)

fun StubInputStream.readLongAsNullable(): Long? = DataInputOutputUtil.readNullable(this, this::readLong)

fun StubOutputStream.writeDoubleAsNullable(value: Double?) =
    DataInputOutputUtil.writeNullable(this, value, this::writeDouble)

fun StubInputStream.readDoubleAsNullable(): Double? =
    DataInputOutputUtil.readNullable(this, this::readDouble)
