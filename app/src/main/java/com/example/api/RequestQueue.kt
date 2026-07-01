package com.example.api

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * A centralized, thread-safe request queuing and processing mechanism.
 * All API tasks submitted to this queue are executed sequentially (concurrency of 1)
 * to avoid overloading the service, especially during high traffic or when rate-limited.
 *
 * It incorporates robust exponential backoff retry logic to handle rate-limiting (HTTP 429)
 * and server overloads (HTTP 503) gracefully.
 */
object RequestQueue {
    private const val TAG = "RequestQueue"
    private val taskChannel = Channel<QueuedTask<*>>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        // Sequentially process queued requests (concurrency = 1)
        scope.launch {
            for (task in taskChannel) {
                executeTask(task)
            }
        }
    }

    private class QueuedTask<T>(
        val block: suspend () -> T,
        val deferred: CompletableDeferred<T>,
        val maxRetries: Int = 3,
        val initialDelayMs: Long = 1000,
        val factor: Double = 2.0,
        val onRetry: (attempt: Int, exception: Exception) -> Unit = { _, _ -> }
    )

    /**
     * Submits a task to the queue and suspends until it completes.
     */
    suspend fun <T> submit(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        factor: Double = 2.0,
        onRetry: (attempt: Int, exception: Exception) -> Unit = { _, _ -> },
        block: suspend () -> T
    ): T {
        val deferred = CompletableDeferred<T>()
        val task = QueuedTask(block, deferred, maxRetries, initialDelayMs, factor, onRetry)
        taskChannel.send(task)
        return deferred.await()
    }

    private suspend fun <T> executeTask(task: QueuedTask<T>) {
        var currentDelay = task.initialDelayMs
        var attempt = 1
        while (true) {
            try {
                val result = task.block()
                task.deferred.complete(result)
                return
            } catch (e: Exception) {
                val isRateLimitOrServerOverload = e is HttpException && (e.code() == 429 || e.code() == 503)
                val isNetworkError = e is java.io.IOException
                val maxAllowedRetries = if (isRateLimitOrServerOverload || isNetworkError) 5 else task.maxRetries

                if (attempt >= maxAllowedRetries) {
                    task.deferred.completeExceptionally(e)
                    return
                }

                task.onRetry(attempt, e)

                // Add randomized delay jitter (±15%) to prevent synchronized requests
                val jitterFactor = 0.85 + (0.3 * Math.random())
                var delayTime = (currentDelay * jitterFactor).toLong()

                // If rate-limited or service is unavailable, back off more aggressively
                if (isRateLimitOrServerOverload || isNetworkError) {
                    delayTime = (delayTime * 1.5).toLong().coerceAtLeast(3000L)
                }

                Log.w(TAG, "Request failed (Attempt $attempt). Error: ${e.message}. Retrying in ${delayTime}ms...")
                
                kotlinx.coroutines.delay(delayTime)
                currentDelay = (currentDelay * task.factor).toLong()
                attempt++
            }
        }
    }
}
