# QNAP Container Station Configuration

## Problem: Can't Modify Environment Variables

QNAP Container Station doesn't allow easy modification of environment variables for composed containers through the GUI.

## Solution: config.json File

The bridge now supports **config.json** for configuration instead of environment variables.

### Method: Use config.json Inside Container

**Priority:** config.json → Environment Variables → Defaults

---

## Setup Instructions

### Step 1: Create config.json

Create a file named `config.json` in the same directory as `androidtv-bridge.js`:

```json
{
  "port": 3000,
  "keepaliveInterval": 30,
  "reconnectDelay": 5,
  "minReconnectInterval": 30,
  "activityTimeout": 60,
  "statePollInterval": 10
}
```

### Step 2: Place in Container

**Option A: Mount as Volume (Recommended)**

In Container Station:
1. Create container
2. Add volume mapping:
   - Host path: `/share/Container/androidtv-bridge/config.json`
   - Container path: `/app/config.json`
3. Start container

**Option B: Copy into Container**

```bash
# Copy config.json into running container
docker cp config.json androidtv-bridge:/app/config.json

# Restart container
docker restart androidtv-bridge
```

**Option C: Build into Image**

In your Dockerfile:
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package.json .
RUN npm install
COPY androidtv-bridge.js .
COPY config.json .
CMD ["node", "androidtv-bridge.js"]
```

---

## Docker Compose for QNAP

### docker-compose.yml

```yaml
version: '3'
services:
  androidtv-bridge:
    image: node:18-alpine
    container_name: androidtv-bridge
    working_dir: /app
    command: node androidtv-bridge.js
    ports:
      - "3000:3000"
    volumes:
      - /share/Container/androidtv-bridge:/app
    restart: unless-stopped
```

**Directory structure on QNAP:**
```
/share/Container/androidtv-bridge/
├── androidtv-bridge.js
├── config.json              ← Configuration file
└── package.json
```

---

## Configuration Options

### config.json Format

```json
{
  "port": 3000,                    // Server port (default: 3000)
  "keepaliveInterval": 30,         // Seconds between keepalive checks (default: 30)
  "reconnectDelay": 5,             // Seconds to wait before reconnecting (default: 5)
  "minReconnectInterval": 30,      // Minimum seconds between reconnects (default: 30)
  "activityTimeout": 60,           // Seconds of inactivity before disconnect (default: 60)
  "statePollInterval": 10          // Internal state update interval (default: 10)
}
```

### Example Configurations

**Conservative (Less Traffic):**
```json
{
  "port": 3000,
  "keepaliveInterval": 60,
  "reconnectDelay": 10,
  "minReconnectInterval": 60,
  "activityTimeout": 120,
  "statePollInterval": 20
}
```

**Aggressive (Maximum Responsiveness):**
```json
{
  "port": 3000,
  "keepaliveInterval": 15,
  "reconnectDelay": 3,
  "minReconnectInterval": 15,
  "activityTimeout": 30,
  "statePollInterval": 5
}
```

---

## Verification

### Check Config Loaded

When bridge starts, look for:
```
======================================================================
Android TV Remote Bridge Server v3.0
======================================================================
✓ Loaded configuration from config.json
Configuration (from config.json):
  Port: 3000
  Keepalive Interval: 30s
  Reconnect Delay: 5s
  Min Reconnect Interval: 30s
  Activity Timeout: 60s
  State Poll Interval: 10s
======================================================================
```

If you see `Configuration (defaults):` instead, config.json wasn't found.

---

## Troubleshooting

### Config Not Loading

**1. Check file location:**
```bash
# SSH into QNAP
docker exec androidtv-bridge ls -la /app/config.json
# Should show the file
```

**2. Check file contents:**
```bash
docker exec androidtv-bridge cat /app/config.json
# Should show valid JSON
```

**3. Check for JSON errors:**
```bash
# Invalid JSON will show warning in bridge logs:
⚠ Error reading config.json, using defaults: ...
```

### Volume Mount Issues

**Check Container Station logs:**
1. Container Station → Your container
2. Click "Details"
3. Look for volume mapping
4. Should show: `/share/Container/androidtv-bridge:/app`

**Verify files inside container:**
```bash
docker exec androidtv-bridge ls /app
# Should show:
# androidtv-bridge.js
# config.json
# package.json
# node_modules/
```

### Permission Issues

```bash
# Fix permissions on QNAP
chmod 644 /share/Container/androidtv-bridge/config.json
```

---

## Updating Configuration

### Method 1: Edit and Restart

1. SSH into QNAP or use File Station
2. Edit `/share/Container/androidtv-bridge/config.json`
3. Save file
4. Restart container in Container Station
5. Check logs for new values

### Method 2: Container Station Web Editor

1. Container Station → Volumes
2. Find your volume
3. Browse to config.json
4. Edit file
5. Save
6. Restart container

### Method 3: Command Line

```bash
# Edit file
nano /share/Container/androidtv-bridge/config.json

# Restart container
docker restart androidtv-bridge

# Check logs
docker logs androidtv-bridge
```

---

## Environment Variables Still Work

**Priority order:**
1. Environment variables (highest)
2. config.json
3. Defaults (lowest)

**Example:** If you set `KEEPALIVE_INTERVAL=45` as environment variable, it will override config.json value.

---

## Migration from Environment Variables

If you were using environment variables before:

**Old way:**
```yaml
environment:
  - KEEPALIVE_INTERVAL=30
```

**New way (QNAP friendly):**
```yaml
volumes:
  - /share/Container/androidtv-bridge:/app
```

And create `config.json` with your settings.

---

## Multiple Instances

**Running multiple bridge instances on QNAP:**

```yaml
version: '3'
services:
  bedroom-tv-bridge:
    image: node:18-alpine
    container_name: bedroom-tv-bridge
    working_dir: /app
    command: node androidtv-bridge.js
    ports:
      - "3001:3000"
    volumes:
      - /share/Container/bedroom-tv-bridge:/app
    restart: unless-stopped

  livingroom-tv-bridge:
    image: node:18-alpine
    container_name: livingroom-tv-bridge
    working_dir: /app
    command: node androidtv-bridge.js
    ports:
      - "3002:3000"
    volumes:
      - /share/Container/livingroom-tv-bridge:/app
    restart: unless-stopped
```

Each directory has its own `config.json` with different settings.

---

## Summary

✅ **config.json works on QNAP**  
✅ **No need to modify environment variables**  
✅ **Easy to edit through File Station or SSH**  
✅ **Just restart container to apply changes**  
✅ **Can still use environment variables if needed**

**config.json is now the recommended configuration method for QNAP users!**
