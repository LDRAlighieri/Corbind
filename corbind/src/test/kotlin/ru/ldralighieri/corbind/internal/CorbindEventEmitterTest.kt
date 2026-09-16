/*
 * Copyright 2026 Vladimir Raupov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ldralighieri.corbind.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CorbindEventEmitterTest {

    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `rendezvous channel queues a burst in order`() = runBlocking {
        // given
        val channel = Channel<Int>()
        val emitter = channel.corbindEventEmitter(scope)
        val expected = (0 until BURST_SIZE).toList()

        // when
        expected.forEach { assertTrue(emitter(it)) }

        // then
        val actual = withTimeout(TIMEOUT_MILLIS) { List(BURST_SIZE) { channel.receive() } }
        assertEquals(expected, actual)
        assertTrue(scope.coroutineContext[Job]?.children?.none() == true)
    }

    @Test
    fun `finite channel queues values while its buffer is full`() = runBlocking {
        // given
        val channel = Channel<Int>(capacity = 1)
        val emitter = channel.corbindEventEmitter(scope)
        val expected = (0 until BURST_SIZE).toList()

        // when
        expected.forEach { assertTrue(emitter(it)) }

        // then
        val actual = withTimeout(TIMEOUT_MILLIS) { List(BURST_SIZE) { channel.receive() } }
        assertEquals(expected, actual)
    }

    @Test
    fun `conflated channel explicitly keeps only the latest value`() {
        // given
        val channel = Channel<Int>(Channel.CONFLATED)
        val emitter = channel.corbindEventEmitter(scope)

        // when
        assertTrue(emitter(1))
        assertTrue(emitter(2))
        assertTrue(emitter(3))

        // then
        assertEquals(3, channel.tryReceive().getOrThrow())
        assertTrue(channel.tryReceive().isFailure)
    }

    @Test
    fun `drop latest policy explicitly keeps the buffered value`() {
        // given
        val channel = Channel<Int>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)
        val emitter = channel.corbindEventEmitter(scope)

        // when
        assertTrue(emitter(1))
        assertTrue(emitter(2))

        // then
        assertEquals(1, channel.tryReceive().getOrThrow())
        assertTrue(channel.tryReceive().isFailure)
    }

    @Test
    fun `drop oldest policy explicitly replaces the buffered value`() {
        // given
        val channel = Channel<Int>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val emitter = channel.corbindEventEmitter(scope)

        // when
        assertTrue(emitter(1))
        assertTrue(emitter(2))

        // then
        assertEquals(2, channel.tryReceive().getOrThrow())
        assertTrue(channel.tryReceive().isFailure)
    }

    @Test
    fun `closed channel rejects a value`() {
        // given
        val channel = Channel<Int>()
        val emitter = channel.corbindEventEmitter(scope)
        channel.close()

        // then
        assertFalse(emitter(1))
    }

    @Test
    fun `cancelled owner rejects a value`() {
        // given
        val channel = Channel<Int>(Channel.UNLIMITED)
        val emitter = channel.corbindEventEmitter(scope)
        scope.cancel()

        // then
        assertFalse(emitter(1))
        assertTrue(channel.tryReceive().isFailure)
    }

    @Test
    fun `owner cancellation removes a pending send`() {
        // given
        val channel = Channel<Int>()
        val emitter = channel.corbindEventEmitter(scope)
        assertTrue(emitter(1))
        assertTrue(scope.coroutineContext[Job]?.children?.any() == true)

        // when
        scope.cancel()

        // then
        assertTrue(scope.coroutineContext[Job]?.children?.none() == true)
        assertTrue(channel.tryReceive().isFailure)
    }

    @Test
    fun `closing channel drains already queued sends in order`() = runBlocking {
        // given
        val channel = Channel<Int>()
        val emitter = channel.corbindEventEmitter(scope)
        val expected = (0 until BURST_SIZE).toList()
        expected.forEach { assertTrue(emitter(it)) }

        // when
        channel.close()

        // then
        val actual = withTimeout(TIMEOUT_MILLIS) { List(BURST_SIZE) { channel.receive() } }
        assertEquals(expected, actual)
        assertTrue(channel.receiveCatching().isClosed)
    }

    private companion object {

        const val BURST_SIZE = 128
        const val TIMEOUT_MILLIS = 5_000L
    }
}
