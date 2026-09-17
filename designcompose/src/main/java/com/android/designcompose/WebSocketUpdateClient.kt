/*
 * Copyright 2026 Google LLC
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

package com.android.designcompose

import android.util.Base64
import android.util.Log
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLSocketFactory
import org.json.JSONObject

/**
 * WebSocket client that connects to a relay server and listens for Figma FILE_UPDATE push
 * notifications. When a notification arrives, it triggers an immediate document fetch instead of
 * waiting for the next poll cycle.
 *
 * This is an alternative to REST polling that reduces latency from 5-15s (poll interval) to <2s
 * (push notification + fetch).
 *
 * Implemented using standard Java/Android sockets (RFC 6455) so that it compiles and operates
 * identically in both Gradle and AOSP Soong build environments without external library
 * dependencies.
 *
 * Usage: val client = WebSocketUpdateClient( relayUrl = "ws://10.0.2.2:8765", onDocumentChanged = {
 * docId, timestamp -> fetchDoc(docId) }, onConnectionStateChanged = { connected ->
 * updateUI(connected) } ) client.connect() client.subscribeToDocument("CuF1b1eAIukB6YszX6B5OZ")
 */
internal class WebSocketUpdateClient(
    private val relayUrl: String,
    private val onDocumentChanged: (docId: String, timestamp: String) -> Unit,
    private val onConnectionStateChanged: (connected: Boolean) -> Unit,
) {
    companion object {
        private const val TAG = "DesignCompose"
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val PING_INTERVAL_SECONDS = 30L
        private const val WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

        private const val OPCODE_TEXT = 0x1
        private const val OPCODE_CLOSE = 0x8
        private const val OPCODE_PING = 0x9
        private const val OPCODE_PONG = 0xA
    }

    @Volatile private var activeSocket: Socket? = null
    @Volatile private var outputStream: BufferedOutputStream? = null
    @Volatile private var pingScheduler: ScheduledExecutorService? = null
    private val writeLock = Any()
    private val random = SecureRandom()

    private val isConnected = AtomicBoolean(false)
    private val isConnecting = AtomicBoolean(false)
    private val isShuttingDown = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)
    private val subscribedDocIds = mutableSetOf<String>()

    /** Connect to the relay WebSocket server. */
    fun connect() {
        if (isShuttingDown.get()) return
        if (isConnected.get() || !isConnecting.compareAndSet(false, true)) {
            Log.d(TAG, "WebSocket already connected or connecting to $relayUrl")
            return
        }

        Log.i(TAG, "WebSocket connecting to $relayUrl...")
        Thread(
                {
                    try {
                        runConnectionLoop()
                    } catch (t: Throwable) {
                        if (!isShuttingDown.get()) {
                            Log.w(TAG, "WebSocket failure: ${t.message}")
                        }
                    } finally {
                        isConnecting.set(false)
                        val wasConnected = isConnected.getAndSet(false)
                        closeActiveResources()
                        if (wasConnected) {
                            onConnectionStateChanged(false)
                        }
                        if (!isShuttingDown.get()) {
                            scheduleReconnect()
                        }
                    }
                },
                "DesignCompose-WebSocket",
            )
            .apply { isDaemon = true }
            .start()
    }

    /** Subscribe to change notifications for a specific Figma document. */
    fun subscribeToDocument(docId: String) {
        synchronized(subscribedDocIds) { subscribedDocIds.add(docId) }
        if (isConnected.get()) {
            sendSubscribe(docId)
        }
    }

    /** Disconnect from the relay server and stop reconnection attempts. */
    fun disconnect() {
        isShuttingDown.set(true)
        if (isConnected.get()) {
            Thread {
                    try {
                        sendFrame(OPCODE_CLOSE, ByteArray(0))
                    } catch (_: Throwable) {} finally {
                        closeActiveResources()
                    }
                }
                .start()
        } else {
            closeActiveResources()
        }
        isConnected.set(false)
    }

    /** Whether the WebSocket is currently connected. */
    fun isConnected(): Boolean = isConnected.get()

    private fun runConnectionLoop() {
        val uri = URI(relayUrl)
        val scheme = uri.scheme?.lowercase() ?: "ws"
        val isSecure = scheme == "wss" || scheme == "https"
        val host = uri.host ?: throw IOException("Invalid WebSocket host in $relayUrl")
        val port = if (uri.port != -1) uri.port else if (isSecure) 443 else 80
        val rawPath = uri.rawPath?.takeIf { it.isNotEmpty() } ?: "/"
        val requestPath = if (uri.rawQuery != null) "$rawPath?${uri.rawQuery}" else rawPath
        val hostHeader =
            if ((isSecure && port == 443) || (!isSecure && port == 80)) host else "$host:$port"

        val rawSocket = Socket()
        rawSocket.tcpNoDelay = true
        rawSocket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)

        val socket =
            if (isSecure) {
                (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(
                    rawSocket,
                    host,
                    port,
                    true,
                )
            } else {
                rawSocket
            }

        activeSocket = socket
        val input = DataInputStream(socket.getInputStream())
        val output = BufferedOutputStream(socket.getOutputStream())
        outputStream = output

        performHandshake(input, output, hostHeader, requestPath)

        Log.i(TAG, "WebSocket connected to $relayUrl")
        isConnected.set(true)
        isConnecting.set(false)
        reconnectAttempts.set(0)
        onConnectionStateChanged(true)

        startPingScheduler()

        // Re-subscribe to all previously subscribed documents
        val docsToSubscribe = synchronized(subscribedDocIds) { subscribedDocIds.toList() }
        for (docId in docsToSubscribe) {
            sendSubscribe(docId)
        }

        readFramesLoop(input)
    }

    private fun performHandshake(
        input: InputStream,
        output: BufferedOutputStream,
        hostHeader: String,
        requestPath: String,
    ) {
        val keyBytes = ByteArray(16)
        random.nextBytes(keyBytes)
        val secWebSocketKey = Base64.encodeToString(keyBytes, Base64.NO_WRAP)

        val request =
            "GET $requestPath HTTP/1.1\r\n" +
                "Host: $hostHeader\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: $secWebSocketKey\r\n" +
                "Sec-WebSocket-Version: 13\r\n\r\n"

        synchronized(writeLock) {
            output.write(request.toByteArray(Charsets.US_ASCII))
            output.flush()
        }

        val statusLine = readHttpLine(input)
        if (!statusLine.contains(" 101 ")) {
            throw IOException("WebSocket handshake failed with status: $statusLine")
        }

        var acceptHeader: String? = null
        while (true) {
            val line = readHttpLine(input)
            if (line.isEmpty()) break
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val name = line.substring(0, colonIndex).trim()
                val value = line.substring(colonIndex + 1).trim()
                if (name.equals("Sec-WebSocket-Accept", ignoreCase = true)) {
                    acceptHeader = value
                }
            }
        }

        if (acceptHeader != null) {
            val expectedAccept =
                Base64.encodeToString(
                    MessageDigest.getInstance("SHA-1")
                        .digest((secWebSocketKey + WEBSOCKET_GUID).toByteArray(Charsets.US_ASCII)),
                    Base64.NO_WRAP,
                )
            if (acceptHeader != expectedAccept) {
                throw IOException("Invalid Sec-WebSocket-Accept header")
            }
        }
    }

    private fun readHttpLine(input: InputStream): String {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) throw EOFException("Unexpected EOF during WebSocket handshake")
            if (b == '\r'.code) {
                val next = input.read()
                if (next == '\n'.code) break
                sb.append('\r')
                if (next != -1) sb.append(next.toChar())
            } else if (b == '\n'.code) {
                break
            } else {
                sb.append(b.toChar())
            }
        }
        return sb.toString()
    }

    private fun startPingScheduler() {
        val scheduler =
            Executors.newSingleThreadScheduledExecutor { r ->
                Thread(r, "DesignCompose-WebSocket-Ping").apply { isDaemon = true }
            }
        pingScheduler = scheduler
        scheduler.scheduleAtFixedRate(
            {
                if (isConnected.get() && !isShuttingDown.get()) {
                    try {
                        sendFrame(OPCODE_PING, ByteArray(0))
                    } catch (e: IOException) {
                        Log.w(TAG, "WebSocket ping failed: ${e.message}")
                        closeActiveResources()
                    }
                }
            },
            PING_INTERVAL_SECONDS,
            PING_INTERVAL_SECONDS,
            TimeUnit.SECONDS,
        )
    }

    private fun readFramesLoop(input: DataInputStream) {
        while (!isShuttingDown.get() && isConnected.get()) {
            val b0 = input.read()
            if (b0 == -1) break
            val b1 = input.read()
            if (b1 == -1) break

            val opcode = b0 and 0x0F
            val isMasked = (b1 and 0x80) != 0
            var payloadLength = (b1 and 0x7F).toLong()

            if (payloadLength == 126L) {
                payloadLength =
                    ((input.readUnsignedByte() shl 8) or input.readUnsignedByte()).toLong()
            } else if (payloadLength == 127L) {
                payloadLength = input.readLong()
            }

            if (payloadLength < 0 || payloadLength > Int.MAX_VALUE) {
                throw IOException("Unsupported WebSocket frame length: $payloadLength")
            }

            val maskKey =
                if (isMasked) {
                    ByteArray(4).also { input.readFully(it) }
                } else {
                    null
                }

            val payload = ByteArray(payloadLength.toInt())
            input.readFully(payload)

            if (maskKey != null) {
                for (i in payload.indices) {
                    payload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
                }
            }

            when (opcode) {
                OPCODE_TEXT -> handleTextMessage(String(payload, Charsets.UTF_8))
                OPCODE_PING -> sendFrame(OPCODE_PONG, payload)
                OPCODE_PONG -> {}
                OPCODE_CLOSE -> {
                    Log.i(TAG, "WebSocket closed by server")
                    try {
                        sendFrame(OPCODE_CLOSE, ByteArray(0))
                    } catch (_: Throwable) {}
                    break
                }
            }
        }
    }

    private fun handleTextMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "")

            when (type) {
                "FILE_UPDATE" -> {
                    val fileKey = json.optString("file_key", "")
                    val timestamp = json.optString("timestamp", "")
                    Log.i(TAG, "WebSocket: FILE_UPDATE for $fileKey at $timestamp")
                    if (fileKey.isNotEmpty()) {
                        onDocumentChanged(fileKey, timestamp)
                    }
                }
                "PING" -> {
                    // Respond to server pings
                    sendText("""{"type":"PONG"}""")
                }
                "subscribed" -> {
                    val fileKey = json.optString("file_key", "")
                    Log.i(TAG, "WebSocket: Subscribed to $fileKey")
                }
                else -> {
                    Log.d(TAG, "WebSocket: Unknown message type '$type': $text")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "WebSocket: Failed to parse message: $text", e)
        }
    }

    private fun sendSubscribe(docId: String) {
        val msg = """{"type":"subscribe","file_key":"$docId"}"""
        Thread {
                try {
                    sendText(msg)
                    Log.d(TAG, "WebSocket: Sent subscribe for $docId")
                } catch (e: IOException) {
                    Log.w(TAG, "WebSocket: Failed to send subscribe for $docId: ${e.message}")
                }
            }
            .start()
    }

    @Throws(IOException::class)
    private fun sendText(text: String) {
        sendFrame(OPCODE_TEXT, text.toByteArray(Charsets.UTF_8))
    }

    @Throws(IOException::class)
    private fun sendFrame(opcode: Int, payload: ByteArray) {
        synchronized(writeLock) {
            val out = outputStream ?: throw IOException("WebSocket is not connected")
            out.write(0x80 or (opcode and 0x0F))

            val length = payload.size
            when {
                length <= 125 -> out.write(0x80 or length)
                length <= 65535 -> {
                    out.write(0x80 or 126)
                    out.write((length ushr 8) and 0xFF)
                    out.write(length and 0xFF)
                }
                else -> {
                    out.write(0x80 or 127)
                    val lenLong = length.toLong()
                    for (shift in 56 downTo 0 step 8) {
                        out.write(((lenLong ushr shift) and 0xFF).toInt())
                    }
                }
            }

            val maskKey = ByteArray(4)
            random.nextBytes(maskKey)
            out.write(maskKey)

            val maskedPayload = ByteArray(length)
            for (i in 0 until length) {
                maskedPayload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
            }
            out.write(maskedPayload)
            out.flush()
        }
    }

    private fun closeActiveResources() {
        pingScheduler?.shutdownNow()
        pingScheduler = null
        synchronized(writeLock) {
            try {
                outputStream?.close()
            } catch (_: Throwable) {}
            outputStream = null
            try {
                activeSocket?.close()
            } catch (_: Throwable) {}
            activeSocket = null
        }
    }

    private fun scheduleReconnect() {
        if (isShuttingDown.get()) return

        val attempt = reconnectAttempts.incrementAndGet()
        val delay =
            minOf(
                INITIAL_RECONNECT_DELAY_MS * (1L shl minOf(attempt - 1, 5)),
                MAX_RECONNECT_DELAY_MS,
            )
        Log.i(TAG, "WebSocket: Reconnecting in ${delay}ms (attempt $attempt)")

        DocServer.mainHandler.postDelayed(
            {
                if (!isShuttingDown.get() && !isConnected.get()) {
                    connect()
                }
            },
            delay,
        )
    }
}
