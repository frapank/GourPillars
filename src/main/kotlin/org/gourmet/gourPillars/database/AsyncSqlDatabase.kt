package org.gourmet.gourPillars.database

import org.gourmet.gourPillars.other.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Shared plumbing for JDBC-backed [Database] implementations: a dedicated executor so blocking
 * JDBC calls never touch the server thread, and a wrapper that turns any exception into a logged
 * warning plus a safe default instead of an exceptionally-completed future.
 */
abstract class AsyncSqlDatabase(
    poolSize: Int,
) : Database {
    private val executor: ExecutorService =
        Executors.newFixedThreadPool(poolSize.coerceIn(1, 8)) { runnable ->
            Thread(runnable, "GourPillars-Database").apply { isDaemon = true }
        }

    @Volatile
    override var isOnline: Boolean = false
        protected set

    @Volatile
    override var lastError: String? = null
        protected set

    protected fun markOnline() {
        isOnline = true
        lastError = null
    }

    protected fun markOffline(message: String) {
        Logger.warning(message)
        isOnline = false
        lastError = message
    }

    protected fun <T> async(
        default: T,
        errorMessage: String,
        block: () -> T,
    ): CompletableFuture<T> =
        CompletableFuture.supplyAsync(
            {
                try {
                    block()
                } catch (e: Exception) {
                    Logger.warning("$errorMessage: ${e.message}")
                    default
                }
            },
            executor,
        )

    override fun close() {
        executor.shutdown()
    }
}
