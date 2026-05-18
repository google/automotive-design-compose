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

import android.util.Log
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * WebSocket client that connects to a relay server and listens for Figma FILE_UPDATE push
 * notifications. When a notification arrives, it triggers an immediate document fetch instead of
 * waiting for the next poll cycle.
 *
 * This is an alternative to REST polling that reduces latency from 5-15s (poll interval) to <2s
 * (push notification + fetch).
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
        private const val NORMAL_CLOSURE_STATUS = 1000
    }

    private val client =
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS) // no timeout for WebSocket
            .pingInterval(30, TimeUnit.SECONDS) // keepalive
            .build()

    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private val isShuttingDown = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)
    private val subscribedDocIds = mutableSetOf<String>()

    /** Connect to the relay WebSocket server. */
    fun connect() {
        if (isShuttingDown.get()) return
        if (isConnected.get()) {
            Log.d(TAG, "WebSocket already connected to $relayUrl")
            return
        }

        Log.i(TAG, "WebSocket connecting to $relayUrl...")
        val request = Request.Builder().url(relayUrl).build()

        webSocket =
            client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.i(TAG, "WebSocket connected to $relayUrl")
                        isConnected.set(true)
                        reconnectAttempts.set(0)
                        onConnectionStateChanged(true)

                        // Re-subscribe to all previously subscribed documents
                        synchronized(subscribedDocIds) {
                            for (docId in subscribedDocIds) {
                                sendSubscribe(webSocket, docId)
                            }
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
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
                                    webSocket.send("""{"type":"PONG"}""")
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

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        Log.i(TAG, "WebSocket closing: $code / $reason")
                        webSocket.close(NORMAL_CLOSURE_STATUS, null)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.i(TAG, "WebSocket closed: $code / $reason")
                        isConnected.set(false)
                        onConnectionStateChanged(false)
                        scheduleReconnect()
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?,
                    ) {
                        Log.w(TAG, "WebSocket failure: ${t.message}")
                        isConnected.set(false)
                        onConnectionStateChanged(false)
                        scheduleReconnect()
                    }
                },
            )
    }

    /** Subscribe to change notifications for a specific Figma document. */
    fun subscribeToDocument(docId: String) {
        synchronized(subscribedDocIds) { subscribedDocIds.add(docId) }
        webSocket?.let { ws ->
            if (isConnected.get()) {
                sendSubscribe(ws, docId)
            }
        }
    }

    /** Disconnect from the relay server and stop reconnection attempts. */
    fun disconnect() {
        isShuttingDown.set(true)
        webSocket?.close(NORMAL_CLOSURE_STATUS, "Client shutting down")
        webSocket = null
        isConnected.set(false)
        client.dispatcher.executorService.shutdown()
    }

    /** Whether the WebSocket is currently connected. */
    fun isConnected(): Boolean = isConnected.get()

    private fun sendSubscribe(ws: WebSocket, docId: String) {
        val msg = """{"type":"subscribe","file_key":"$docId"}"""
        ws.send(msg)
        Log.d(TAG, "WebSocket: Sent subscribe for $docId")
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
