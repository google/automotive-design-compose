#!/usr/bin/env python3
# Copyright 2026 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Figma WebSocket Relay Server

Receives Figma webhook FILE_UPDATE events (HTTP POST) and pushes them
to connected Android devices via WebSocket.

Architecture:
  Figma Cloud -> HTTP POST /webhook -> This Server -> WebSocket -> Android App

Usage:
  pip install aiohttp websockets
  python relay_server.py

  # For development, expose with ngrok:
  ngrok http 8766
  # Then register the ngrok URL as your Figma webhook endpoint

Configuration:
  WS_PORT:      WebSocket server port (default: 8765)
  WEBHOOK_PORT: HTTP webhook receiver port (default: 8766)
  PASSCODE:     Webhook passcode for verification (default: "dc_relay")
"""

import asyncio
import json
import logging
import os
import sys
import time
from collections import defaultdict

try:
    import websockets
except ImportError:
    print("ERROR: 'websockets' package required. Install with: pip install websockets")
    sys.exit(1)

try:
    from aiohttp import web
except ImportError:
    print("ERROR: 'aiohttp' package required. Install with: pip install aiohttp")
    sys.exit(1)

# Configuration
WS_PORT = int(os.environ.get("WS_PORT", "8765"))
WEBHOOK_PORT = int(os.environ.get("WEBHOOK_PORT", "8766"))
PASSCODE = os.environ.get("PASSCODE", "dc_relay")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("relay")

# State: file_key -> set of connected websocket clients
subscriptions: dict[str, set] = defaultdict(set)
# All connected clients
all_clients: set = set()
# Stats
stats = {"webhooks_received": 0, "notifications_sent": 0, "connections_total": 0}


# ─── WebSocket Server ────────────────────────────────────────────────


async def handle_ws(websocket):
    """Handle a WebSocket connection from an Android device."""
    client_id = f"{websocket.remote_address[0]}:{websocket.remote_address[1]}"
    all_clients.add(websocket)
    stats["connections_total"] += 1
    log.info(f"Client connected: {client_id} (total: {len(all_clients)})")

    try:
        async for message in websocket:
            try:
                data = json.loads(message)
                msg_type = data.get("type", "")

                if msg_type == "subscribe":
                    file_key = data.get("file_key", "")
                    if file_key:
                        subscriptions[file_key].add(websocket)
                        log.info(
                            f"Client {client_id} subscribed to {file_key} "
                            f"({len(subscriptions[file_key])} subscriber(s))"
                        )
                        # Acknowledge subscription
                        await websocket.send(
                            json.dumps(
                                {"type": "subscribed", "file_key": file_key}
                            )
                        )

                elif msg_type == "PONG":
                    pass  # keepalive response, ignore

                else:
                    log.debug(f"Unknown message from {client_id}: {message}")

            except json.JSONDecodeError:
                log.warning(f"Invalid JSON from {client_id}: {message}")

    except websockets.exceptions.ConnectionClosed:
        pass
    finally:
        # Cleanup on disconnect
        all_clients.discard(websocket)
        for clients in subscriptions.values():
            clients.discard(websocket)
        log.info(f"Client disconnected: {client_id} (remaining: {len(all_clients)})")


# ─── HTTP Webhook Receiver ───────────────────────────────────────────


async def handle_webhook(request):
    """Receives Figma FILE_UPDATE webhook POST requests."""
    try:
        data = await request.json()
    except Exception:
        return web.json_response({"error": "invalid JSON"}, status=400)

    # Verify passcode if configured
    if PASSCODE and data.get("passcode") != PASSCODE:
        log.warning(f"Webhook rejected: invalid passcode")
        return web.json_response({"error": "invalid passcode"}, status=403)

    event_type = data.get("event_type", "")
    file_key = data.get("file_key", "")
    timestamp = data.get("timestamp", time.strftime("%Y-%m-%dT%H:%M:%SZ"))

    stats["webhooks_received"] += 1
    log.info(f"Webhook: {event_type} for {file_key} at {timestamp}")

    if event_type in ("FILE_UPDATE", "FILE_VERSION_UPDATE", "PING"):
        # Push to all subscribed clients
        msg = json.dumps(
            {
                "type": "FILE_UPDATE",
                "file_key": file_key,
                "timestamp": timestamp,
            }
        )

        targets = subscriptions.get(file_key, set())
        if targets:
            log.info(f"Pushing to {len(targets)} client(s) for {file_key}")
            # Send to all, remove dead connections
            dead = set()
            for ws in targets:
                try:
                    await ws.send(msg)
                    stats["notifications_sent"] += 1
                except websockets.exceptions.ConnectionClosed:
                    dead.add(ws)
            for ws in dead:
                targets.discard(ws)
                all_clients.discard(ws)
        else:
            log.info(f"No subscribers for {file_key}")

    return web.json_response({"status": "ok"})


async def handle_status(request):
    """Health check / status endpoint."""
    return web.json_response(
        {
            "status": "running",
            "clients": len(all_clients),
            "subscriptions": {k: len(v) for k, v in subscriptions.items()},
            "stats": stats,
        }
    )


async def handle_test_push(request):
    """
    Manual test: push a fake FILE_UPDATE to all clients subscribed to a file_key.
    POST /test?file_key=CuF1b1eAIukB6YszX6B5OZ
    """
    file_key = request.query.get("file_key", "")
    if not file_key:
        return web.json_response({"error": "file_key required"}, status=400)

    msg = json.dumps(
        {
            "type": "FILE_UPDATE",
            "file_key": file_key,
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ"),
        }
    )

    targets = subscriptions.get(file_key, set())
    count = 0
    for ws in list(targets):
        try:
            await ws.send(msg)
            count += 1
        except websockets.exceptions.ConnectionClosed:
            targets.discard(ws)

    log.info(f"Test push: sent to {count} client(s) for {file_key}")
    return web.json_response({"pushed_to": count, "file_key": file_key})


# ─── Main ────────────────────────────────────────────────────────────


async def main():
    # Start WebSocket server
    # Disable protocol-level ping/pong — OkHttp handles its own keepalive pings
    ws_server = await websockets.serve(
        handle_ws, "0.0.0.0", WS_PORT,
        ping_interval=None, ping_timeout=None
    )

    # Start HTTP webhook receiver
    app = web.Application()
    app.router.add_post("/webhook", handle_webhook)
    app.router.add_get("/status", handle_status)
    app.router.add_post("/test", handle_test_push)

    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", WEBHOOK_PORT)
    await site.start()

    log.info(f"╔══════════════════════════════════════════════╗")
    log.info(f"║  Figma WebSocket Relay Server                ║")
    log.info(f"║                                              ║")
    log.info(f"║  WebSocket:  ws://0.0.0.0:{WS_PORT:<5}             ║")
    log.info(f"║  Webhook:    http://0.0.0.0:{WEBHOOK_PORT:<5}           ║")
    log.info(f"║  Status:     http://0.0.0.0:{WEBHOOK_PORT}/status      ║")
    log.info(f"║  Test Push:  POST http://0.0.0.0:{WEBHOOK_PORT}/test   ║")
    log.info(f"╚══════════════════════════════════════════════╝")
    log.info(f"")
    log.info(f"For emulators, connect with: ws://10.0.2.2:{WS_PORT}")
    log.info(f"Waiting for connections...")

    await asyncio.Future()  # run forever


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        log.info("Shutting down...")
