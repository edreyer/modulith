package io.liquidsoftware.common.ext

import arrow.core.Either
import arrow.core.raise.Raise
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

context(r: Raise<Err>) fun <Err> raise(error: Err): Nothing = r.raise(error)

@OptIn(ExperimentalContracts::class)
context(r: Raise<Err>) fun <Err> ensure(condition: Boolean, raise: () -> Err) {
  contract {
    callsInPlace(raise, InvocationKind.AT_MOST_ONCE)
    returns() implies condition
  }
  return if (condition) Unit else r.raise(raise())
}

@OptIn(ExperimentalContracts::class)
context(r: Raise<Err>) fun <Err, B : Any> ensureNotNull(value: B?, raise: () -> Err): B {
  contract {
    callsInPlace(raise, InvocationKind.AT_MOST_ONCE)
    returns() implies (value != null)
  }
  return value ?: r.raise(raise())
}

@Suppress("NOTHING_TO_INLINE")
context(r: Raise<Err>)
inline fun <Err, A> Either<Err, A>.bind(): A = with(r) { bind() }
