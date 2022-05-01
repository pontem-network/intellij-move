/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.stdext

sealed class MvResult<out T, out E> {
    data class Ok<T>(val ok: T) : MvResult<T, Nothing>()
    data class Err<E>(val err: E) : MvResult<Nothing, E>()

    val isOk: Boolean get() = this is Ok
    val isErr: Boolean get() = this is Err

    fun ok(): T? = when (this) {
        is Ok -> ok
        is Err -> null
    }

    fun err(): E? = when (this) {
        is Ok -> null
        is Err -> err
    }

    inline fun <U> map(mapper: (T) -> U): MvResult<U, E> = when (this) {
        is Ok -> Ok(mapper(ok))
        is Err -> Err(err)
    }

    inline fun <U> mapErr(mapper: (E) -> U): MvResult<T, U> = when (this) {
        is Ok -> Ok(ok)
        is Err -> Err(mapper(err))
    }

    fun unwrap(): T = when (this) {
        is Ok -> ok
        is Err -> if (err is Throwable) {
            throw IllegalStateException("called `RsResult.unwrap()` on an `Err` value", err)
        } else {
            throw IllegalStateException("called `RsResult.unwrap()` on an `Err` value: $err")
        }
    }
}

inline fun <T, E, U> MvResult<T, E>.andThen(action: (T) -> MvResult<U, E>): MvResult<U, E> = when (this) {
    is MvResult.Ok -> action(ok)
    is MvResult.Err -> MvResult.Err(err)
}

inline fun <T, E, F> MvResult<T, E>.orElse(op: (E) -> MvResult<T, F>): MvResult<T, F> = when (this) {
    is MvResult.Ok -> MvResult.Ok(ok)
    is MvResult.Err -> op(err)
}

inline fun <T, E> MvResult<T, E>.unwrapOrElse(op: (E) -> T): T = when (this) {
    is MvResult.Ok -> ok
    is MvResult.Err -> op(err)
}

fun <T, E : Throwable> MvResult<T, E>.unwrapOrThrow(): T = when (this) {
    is MvResult.Ok -> ok
    is MvResult.Err -> throw err
}

fun <T : Any> T?.toResult(): MvResult<T, Unit> = if (this != null) MvResult.Ok(this) else MvResult.Err(Unit)
