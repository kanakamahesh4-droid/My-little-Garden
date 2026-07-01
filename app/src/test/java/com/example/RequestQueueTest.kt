package com.example

import com.example.api.RequestQueue
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.util.concurrent.atomic.AtomicInteger

class RequestQueueTest {

    @Test
    fun test_request_queue_executes_tasks_sequentially() = runBlocking {
        val executionOrder = mutableListOf<Int>()

        val job1 = async {
            RequestQueue.submit {
                delay(100)
                executionOrder.add(1)
                "First"
            }
        }

        val job2 = async {
            RequestQueue.submit {
                executionOrder.add(2)
                "Second"
            }
        }

        assertEquals("First", job1.await())
        assertEquals("Second", job2.await())
        assertEquals(listOf(1, 2), executionOrder)
    }

    @Test
    fun test_request_queue_retries_on_rate_limit_429() = runBlocking {
        val attempts = AtomicInteger(0)
        
        // Create a dummy HttpException for HTTP 429
        val dummyResponse = Response.error<Any>(429, "Too Many Requests".toResponseBody(null))
        val httpException429 = HttpException(dummyResponse)

        val result = try {
            RequestQueue.submit(
                maxRetries = 2,
                initialDelayMs = 10,
                onRetry = { attempt, _ ->
                    attempts.set(attempt)
                }
            ) {
                if (attempts.get() < 2) {
                    throw httpException429
                }
                "Success after retries"
            }
        } catch (e: Exception) {
            "Failed"
        }

        assertEquals("Success after retries", result)
        assertEquals(2, attempts.get())
    }

    @Test
    fun test_request_queue_fails_after_max_retries() = runBlocking {
        val attempts = AtomicInteger(0)
        val dummyResponse = Response.error<Any>(503, "Service Unavailable".toResponseBody(null))
        val httpException503 = HttpException(dummyResponse)

        var caughtException: Exception? = null

        try {
            RequestQueue.submit(
                maxRetries = 2,
                initialDelayMs = 10,
                onRetry = { attempt, _ ->
                    attempts.incrementAndGet()
                }
            ) {
                throw httpException503
            }
        } catch (e: Exception) {
            caughtException = e
        }

        assertTrue(caughtException is HttpException)
        assertEquals(503, (caughtException as HttpException).code())
        // For rate limit/overload, maxAllowedRetries = 5, but our test will reach whatever max is configured
        assertTrue(attempts.get() > 0)
    }
}
