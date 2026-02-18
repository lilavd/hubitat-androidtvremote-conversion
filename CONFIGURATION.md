# Configuration Guide - Timing & Polling

## Driver Configuration

### Status Check Interval

**Setting:** `statusInterval` (in device preferences)

**What it does:** Controls how often the driver queries the bridge for TV state (power, volume, mute, app)

**Default:** 10 seconds

**Range:** 5-300 seconds

**How to configure:**
1. Go to device page
2. Click "Preferences"
3. Find "Status Check Interval (seconds)"
4. Enter value between 5-300
5. Click "Save Preferences"

**Recommendations:**
- **5-10 seconds:** Real-time tracking (recommended for active use)
- **15-30 seconds:** Balanced (good for most users)
- **60+ seconds:** Light polling (reduces network traffic)

**Example:**
```
Status Check Interval: 10 seconds (default)
→ Driver queries bridge every 10 seconds
→ Updates power, volume, mute, currentApp
```

---

## Bridge Configuration

All bridge timings can be configured via **environment variables** or **Docker environment variables**.

### Available Settings

| Setting | Environment Variable | Default | Description |
|---------|---------------------|---------|-------------|
| Port | `BRIDGE_PORT` | 3000 | HTTP server port |
| Keepalive Interval | `KEEPALIVE_INTERVAL` | 30 | Seconds between keepalive checks |
| Reconnect Delay | `RECONNECT_DELAY` | 5 | Seconds to wait before reconnecting |
| Min Reconnect Interval | `MIN_RECONNECT_INTERVAL` | 30 | Minimum seconds between reconnect attempts |
| Activity Timeout | `ACTIVITY_TIMEOUT` | 60 | Seconds of inactivity before marking disconnected |
| State Poll Interval | `STATE_POLL_INTERVAL` | 10 | Internal state update interval |

---

## Setting Environment Variables

### Method 1: Command Line (Linux/Mac)

```bash
# Set variables before running bridge
export KEEPALIVE_INTERVAL=60
export RECONNECT_DELAY=10
export MIN_RECONNECT_INTERVAL=60
export ACTIVITY_TIMEOUT=120

node androidtv-bridge.js
```

### Method 2: .env File

**Create `.env` file:**
```bash
BRIDGE_PORT=3000
KEEPALIVE_INTERVAL=30
RECONNECT_DELAY=5
MIN_RECONNECT_INTERVAL=30
ACTIVITY_TIMEOUT=60
STATE_POLL_INTERVAL=10
```

**Run with dotenv:**
```bash
# Install dotenv
npm install dotenv

# Modify bridge file (add at top):
# require('dotenv').config();

# Run normally
node androidtv-bridge.js
```

### Method 3: systemd Service (Linux)

**Edit service file:**
```bash
sudo nano /etc/systemd/system/androidtv-bridge.service
```

**Add environment variables:**
```ini
[Unit]
Description=Android TV Bridge Server
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/androidtv-bridge
Environment="KEEPALIVE_INTERVAL=60"
Environment="RECONNECT_DELAY=10"
Environment="MIN_RECONNECT_INTERVAL=60"
Environment="ACTIVITY_TIMEOUT=120"
ExecStart=/usr/bin/node /home/pi/androidtv-bridge/androidtv-bridge.js
Restart=always

[Install]
WantedBy=multi-user.target
```

**Reload and restart:**
```bash
sudo systemctl daemon-reload
sudo systemctl restart androidtv-bridge
```

### Method 4: PM2 (Process Manager)

**Create ecosystem file:**
```bash
nano ecosystem.config.js
```

**Add configuration:**
```javascript
module.exports = {
  apps: [{
    name: 'androidtv-bridge',
    script: './androidtv-bridge.js',
    env: {
      BRIDGE_PORT: 3000,
      KEEPALIVE_INTERVAL: 30,
      RECONNECT_DELAY: 5,
      MIN_RECONNECT_INTERVAL: 30,
      ACTIVITY_TIMEOUT: 60,
      STATE_POLL_INTERVAL: 10
    }
  }]
};
```

**Start with PM2:**
```bash
pm2 start ecosystem.config.js
pm2 save
```

### Method 5: Docker

**docker-compose.yml:**
```yaml
version: '3'
services:
  androidtv-bridge:
    build: .
    ports:
      - "3000:3000"
    environment:
      - BRIDGE_PORT=3000
      - KEEPALIVE_INTERVAL=30
      - RECONNECT_DELAY=5
      - MIN_RECONNECT_INTERVAL=30
      - ACTIVITY_TIMEOUT=60
      - STATE_POLL_INTERVAL=10
    restart: unless-stopped
```

**Docker run command:**
```bash
docker run -d \
  -p 3000:3000 \
  -e KEEPALIVE_INTERVAL=30 \
  -e RECONNECT_DELAY=5 \
  -e MIN_RECONNECT_INTERVAL=30 \
  -e ACTIVITY_TIMEOUT=60 \
  --name androidtv-bridge \
  --restart unless-stopped \
  androidtv-bridge
```

---

## Configuration Recommendations

### Default Configuration (Recommended)
```bash
KEEPALIVE_INTERVAL=30        # Check connection every 30s
RECONNECT_DELAY=5            # Wait 5s before reconnecting
MIN_RECONNECT_INTERVAL=30    # Don't reconnect more than every 30s
ACTIVITY_TIMEOUT=60          # Mark disconnected after 60s of no activity
```
**Good for:** Most users, balanced performance

### Conservative Configuration (Less Network Traffic)
```bash
KEEPALIVE_INTERVAL=60        # Check every 60s
RECONNECT_DELAY=10           # Wait 10s before reconnecting
MIN_RECONNECT_INTERVAL=60    # Don't reconnect more than every 60s
ACTIVITY_TIMEOUT=120         # Mark disconnected after 2 minutes
```
**Good for:** Reducing network traffic, stable networks

### Aggressive Configuration (Maximum Responsiveness)
```bash
KEEPALIVE_INTERVAL=15        # Check every 15s
RECONNECT_DELAY=3            # Wait only 3s before reconnecting
MIN_RECONNECT_INTERVAL=15    # Allow reconnect every 15s
ACTIVITY_TIMEOUT=30          # Mark disconnected quickly
```
**Good for:** Unstable networks, maximum responsiveness

---

## Verification

### Check Bridge Configuration

**When bridge starts, it prints configuration:**
```
======================================================================
Android TV Remote Bridge Server v3.0
======================================================================
Configuration:
  Port: 3000
  Keepalive Interval: 30s
  Reconnect Delay: 5s
  Min Reconnect Interval: 30s
  Activity Timeout: 60s
  State Poll Interval: 10s
======================================================================
```

**Verify your settings are showing correctly!**

### Check Driver Configuration

**In Hubitat logs after Initialize:**
```
[warn] ▶ INITIALIZE: Status checks scheduled every 10 seconds
```

**Verify your interval is showing correctly!**

---

## Troubleshooting Configuration

### Bridge Not Using Custom Values

**Problem:** Bridge still shows default values

**Check:**
1. Environment variables set correctly?
2. Using correct variable names? (case-sensitive)
3. Restarted bridge after setting variables?

**Test:**
```bash
# Before starting bridge
echo $KEEPALIVE_INTERVAL
# Should print your value, not empty
```

### Driver Not Using Custom Interval

**Problem:** Driver still polls every 10 seconds

**Check:**
1. Saved preferences?
2. Value in valid range (5-300)?
3. Clicked Initialize after saving?

**Fix:**
```
Device Page → Preferences → Set interval → Save → Initialize
```

### Settings Not Taking Effect

**Bridge:**
- Must restart bridge for changes to take effect
- Check startup logs show new values

**Driver:**
- Must save preferences
- Must reinitialize (or just save preferences which calls initialize)
- Check logs show new schedule

---

## Performance Impact

### High Frequency (5-15 seconds)

**Pros:**
- Real-time state updates
- Quick detection of changes
- Better for automations

**Cons:**
- More network traffic
- More bridge load
- More log entries

**Network:** ~6-12 requests/minute per TV

### Medium Frequency (15-30 seconds)

**Pros:**
- Balanced performance
- Good state updates
- Reasonable traffic

**Cons:**
- Slightly delayed updates

**Network:** ~2-4 requests/minute per TV

### Low Frequency (60+ seconds)

**Pros:**
- Minimal network traffic
- Low load on bridge
- Clean logs

**Cons:**
- Delayed state updates
- May miss quick changes

**Network:** ~1 request/minute per TV

---

## Advanced Tuning

### Match Driver and Bridge Intervals

**For best performance:**
```bash
# Bridge
STATE_POLL_INTERVAL=10

# Driver
statusInterval=10
```

**Both poll at same rate - no wasted cycles**

### Tune for Network Conditions

**Stable Network:**
```bash
KEEPALIVE_INTERVAL=60
ACTIVITY_TIMEOUT=120
statusInterval=30
```

**Unstable Network:**
```bash
KEEPALIVE_INTERVAL=15
RECONNECT_DELAY=3
ACTIVITY_TIMEOUT=30
statusInterval=10
```

### Tune for Usage Pattern

**Always Active (gaming, streaming):**
```bash
statusInterval=5
KEEPALIVE_INTERVAL=15
```

**Occasional Use:**
```bash
statusInterval=30
KEEPALIVE_INTERVAL=60
```

**Mostly Off:**
```bash
statusInterval=60
KEEPALIVE_INTERVAL=120
```

---

## Examples

### Example 1: Home Theater Setup

**Requirements:** Real-time state, stable network

**Configuration:**
```bash
# Bridge
KEEPALIVE_INTERVAL=30
RECONNECT_DELAY=5
ACTIVITY_TIMEOUT=60

# Driver
statusInterval=10
```

### Example 2: Multiple TVs

**Requirements:** Reduced traffic, many devices

**Configuration:**
```bash
# Bridge
KEEPALIVE_INTERVAL=60
ACTIVITY_TIMEOUT=120

# Driver (per TV)
statusInterval=20
```

### Example 3: Unstable WiFi

**Requirements:** Quick reconnect, short timeouts

**Configuration:**
```bash
# Bridge
KEEPALIVE_INTERVAL=15
RECONNECT_DELAY=3
MIN_RECONNECT_INTERVAL=15
ACTIVITY_TIMEOUT=30

# Driver
statusInterval=10
```

---

## Summary

### Driver
✅ Configure in preferences
✅ Range: 5-300 seconds
✅ Default: 10 seconds
✅ Save and reinitialize to apply

### Bridge
✅ Configure via environment variables
✅ 6 configurable timings
✅ Defaults work for most users
✅ Restart bridge to apply

### Best Practice
✅ Start with defaults
✅ Tune based on your needs
✅ Monitor logs to verify
✅ Adjust as needed

**Both driver and bridge are now fully configurable!** ⚙️
