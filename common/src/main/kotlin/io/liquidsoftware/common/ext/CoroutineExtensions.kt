package io.liquidsoftware.common.ext

import io.liquidsoftware.common.security.SecurityCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> withContextDefault(block: suspend CoroutineScope.() -> T): T =
  withContext(Dispatchers.Default + SecurityCoroutineContext()) {
    block()
  }

suspend fun <T> withContextIO(block: suspend CoroutineScope.() -> T): T =
  withContext(Dispatchers.IO + SecurityCoroutineContext()) {
    block()
  }
