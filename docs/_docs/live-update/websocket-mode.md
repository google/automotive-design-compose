---
title: 'WebSocket Live Updates'
layout: page
parent: Live Update
nav_order: 5
---

{% include toc.md %}

# WebSocket Live Updates

DesignCompose supports an optional WebSocket mode that provides faster change
detection by receiving push notifications from a relay server instead of
relying solely on periodic REST polling.

## Overview

DesignCompose offers two live update modes:

| Mode | Detection Latency | Setup Complexity |
|------|------------------|------------------|
| **REST Polling** (default) | ~15 seconds | None — works out of the box |
| **WebSocket + REST** | Instant (with webhook) | Requires relay server |

Both modes use the same **Smart Cache** and **Node-Scoped Fetching**
optimizations that reduce the actual Figma API fetch time.

## Architecture

```
┌─────────────────────────────────────────────┐
│               ANDROID APP                    │
│                                              │
│  DocServer ──► fetchDocuments() ──► Rust JNI │
│      │              │                        │
│      │         [15s poll]                    │
│      │         (safety net)                  │
│      │                                       │
│  WebSocket ◄────────┤                        │
│  Client        [push trigger]                │
│  (OkHttp)      (instant)                     │
└──────┬───────────────────────────────────────┘
       │ ws://10.0.2.2:8765
┌──────┼──────────────────────────────────────┐
│  Relay Server (Python)                       │
│      │                                       │
│  WebSocket ◄── HTTP POST ◄── Figma Webhooks │
│  (port 8765)   (port 8766)                   │
└─────────────────────────────────────────────┘
```

When WebSocket mode is enabled:
1. A **REST polling safety net** runs every 15s (guaranteed detection)
2. A **WebSocket client** connects to the relay server
3. When the relay pushes a `FILE_UPDATE` event, the app fetches immediately
4. The REST poll is cancelled and rescheduled after the fetch completes

## Setup Guide

### Prerequisites

- Python 3.8+ installed on your development machine
- Android emulator running with the app installed
- Figma API token configured (see [Live Update Setup][1])

### Step 1: Install Relay Dependencies

```shell
python3 -m venv /tmp/ws_relay_venv
/tmp/ws_relay_venv/bin/pip install websockets aiohttp
```

### Step 2: Start the Relay Server

```shell
/tmp/ws_relay_venv/bin/python tools/ws-relay/relay_server.py
```

The server starts on two ports:
- **Port 8765** — WebSocket server (devices connect here)
- **Port 8766** — HTTP server (receives Figma webhooks, serves status)

### Step 3: Enable WebSocket Mode on Device

After the app is running and the API key is set:

```shell
# Set the relay URL (10.0.2.2 is the host machine from the emulator)
adb shell am startservice \
  -n "<YOUR_APP_ID>/com.android.designcompose.ApiKeyService" \
  -a setWebSocketUrl \
  -e Url "ws://10.0.2.2:8765"

# Enable WebSocket mode
adb shell am startservice \
  -n "<YOUR_APP_ID>/com.android.designcompose.ApiKeyService" \
  -a setWebSocket \
  --ez Enabled true
```

Example for the HelloWorld app:

```shell
adb shell am startservice \
  -n "com.android.designcompose.testapp.helloworld/com.android.designcompose.ApiKeyService" \
  -a setWebSocketUrl \
  -e Url "ws://10.0.2.2:8765"

adb shell am startservice \
  -n "com.android.designcompose.testapp.helloworld/com.android.designcompose.ApiKeyService" \
  -a setWebSocket \
  --ez Enabled true
```

### Step 4: Verify the Connection

Check the relay server status:

```shell
curl http://localhost:8766/status
```

Expected output:
```json
{"clients": 1, "subscriptions": {"YOUR_FILE_KEY": 1}}
```

Check the app logs:

```shell
adb logcat -d | grep "WebSocket" | tail -5
```

Expected output:
```
WebSocket connected to ws://10.0.2.2:8765
WebSocket: Connected — real-time updates active
WebSocket: Subscribed to YOUR_FILE_KEY
```

### Step 5: Test with a Manual Push

Simulate a Figma change notification:

```shell
curl -X POST "http://localhost:8766/test?file_key=YOUR_FILE_KEY"
```

The app will immediately trigger a fetch. Check logs:

```shell
adb logcat -d | grep -E "(FILE_UPDATE|Received change|informed)" | tail -3
```

### Step 6 (Optional): Register a Figma Webhook

For true real-time updates, register a Figma webhook that posts to your relay.
This requires a publicly accessible URL:

```shell
# Expose the relay via ngrok
ngrok http 8766

# Register the webhook with Figma
curl -X POST "https://api.figma.com/v2/webhooks" \
  -H "X-Figma-Token: YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "FILE_UPDATE",
    "team_id": "YOUR_TEAM_ID",
    "endpoint": "https://YOUR_NGROK_URL.ngrok.io/webhook",
    "passcode": "dc_relay"
  }'
```

## Disabling WebSocket Mode

```shell
adb shell am startservice \
  -n "<YOUR_APP_ID>/com.android.designcompose.ApiKeyService" \
  -a setWebSocket \
  --ez Enabled false
```

The app reverts to REST-only polling.

## Network Configuration

The `helloworld` reference app includes a `network_security_config.xml` that
allows cleartext WebSocket connections to `10.0.2.2` (the emulator's host
loopback). For production apps, use TLS (`wss://`) instead.

If your app needs cleartext WebSocket support during development, add to your
`AndroidManifest.xml`:

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

And create `res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

## Relay Server API Reference

| Endpoint | Method | Description |
|----------|--------|-------------|
| `ws://host:8765` | WebSocket | Device connection endpoint |
| `http://host:8766/status` | GET | Returns client count and subscriptions |
| `http://host:8766/test?file_key=KEY` | POST | Sends a test push to all subscribers |
| `http://host:8766/webhook` | POST | Receives Figma FILE_UPDATE webhooks |

## Troubleshooting

**WebSocket won't connect:**
- Verify the relay server is running: `curl http://localhost:8766/status`
- Check the emulator can reach the host: `adb shell ping 10.0.2.2`
- Verify `network_security_config.xml` allows cleartext to `10.0.2.2`

**Changes detected but slow to render:**
- The Figma API response time (~17s with `ids=` filter) is the bottleneck
- Verify smart cache is active: `adb logcat -d | grep "Smart cache HIT"`

**WebSocket disconnects frequently:**
- The client uses exponential backoff (1s → 2s → 4s → ... → 30s)
- Check relay logs for errors

[1]: {%link _docs/live-update/setup.md %}
