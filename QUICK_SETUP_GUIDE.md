# Android TV Remote - Quick Setup Guide

## Overview

This system allows Hubitat to control Android TV devices using the official Android TV Remote Protocol v2. Since Hubitat's Groovy environment can't handle the complex protocol directly, we use a bridge server.

## Architecture

```
Hubitat → HTTP → Bridge Server (Node.js) → Protocol v2 → Android TV
```

## Prerequisites

- Android TV device (2015 or newer, NOT Fire TV)
- Hubitat hub
- Computer/Raspberry Pi to run bridge server
- Node.js installed (v14 or higher)
- All devices on same network

## Step 1: Install Bridge Server

### Option A: Linux/Mac/Raspberry Pi

```bash
# Create directory
mkdir androidtv-bridge
cd androidtv-bridge

# Install Node.js (if needed)
# For Raspberry Pi/Debian:
sudo apt update
sudo apt install nodejs npm

# For Mac with Homebrew:
brew install node

# Create package.json
cat > package.json << 'EOF'
{
  "name": "androidtv-bridge",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.2",
    "body-parser": "^1.20.2",
    "androidtvremote2": "^0.3.0"
  }
}
EOF

# Install dependencies
npm install

# Copy bridge server file
# (Use the androidtv-bridge.js file provided)

# Run the bridge
node androidtv-bridge.js
```

### Option B: Windows

```cmd
REM Create directory
mkdir androidtv-bridge
cd androidtv-bridge

REM Install Node.js from https://nodejs.org/

REM Create package.json (use same content as above)

REM Install dependencies
npm install

REM Run the bridge
node androidtv-bridge.js
```

### Option C: Docker

```dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package.json .
RUN npm install

COPY androidtv-bridge.js .

EXPOSE 3000

CMD ["node", "androidtv-bridge.js"]
```

```bash
docker build -t androidtv-bridge .
docker run -d -p 3000:3000 --name androidtv-bridge --restart unless-stopped androidtv-bridge
```

## Step 2: Install Hubitat Driver

1. **Go to Hubitat Web Interface**
   - Navigate to `Drivers Code`

2. **Create New Driver**
   - Click `+ New Driver`
   - Paste contents of `Android_TV_Remote_Bridge_Driver.groovy`
   - Click `Save`

3. **Create Virtual Device**
   - Navigate to `Devices`
   - Click `+ Add Device`
   - Click `Virtual`
   - Fill in:
     - Device Name: `Living Room TV` (or whatever you want)
     - Device Label: (optional)
     - Type: `Android TV Remote (Bridge)`
   - Click `Save Device`

## Step 3: Configure Driver

1. **Open Device Settings**
   - Click on your new device
   - Scroll to `Preferences`

2. **Enter Configuration**
   ```
   Android TV IP Address: 192.168.1.XXX (your TV's IP)
   Bridge Server IP: 192.168.1.YYY (computer running bridge)
   Bridge Server Port: 3000 (default)
   Device ID: living-room-tv (unique identifier)
   Device Name: Hubitat (shown during pairing)
   Auto-connect on Initialize: ✓
   ```

3. **Save Preferences**

## Step 4: Pair with Android TV

1. **Ensure TV and Bridge Are On**
   - TV powered on
   - Bridge server running
   - Check bridge logs for "Server running"

2. **Check Bridge Connection**
   - In Hubitat device, click `checkBridge`
   - Look for `bridgeStatus: online`
   - If offline, verify bridge server IP/port

3. **Start Pairing**
   - Click `startPairing` command
   - Watch Hubitat logs (should say "PAIRING STARTED")
   - **Look at your TV screen** - 6-digit code should appear

4. **Complete Pairing**
   - Note the 6-digit code from TV
   - Click `completePairing` command
   - Enter code (e.g., "123456")
   - Wait for "PAIRING SUCCESSFUL" in logs

5. **Connect**
   - Click `connect` command
   - Status should change to "connected"

## Step 5: Test Commands

Try these commands in the device page:

```
home           # Go to home screen
dpadUp         # Navigate up
dpadDown       # Navigate down
dpadCenter     # Select
back           # Go back
volumeUp       # Increase volume
volumeDown     # Decrease volume
play           # Play
pause          # Pause
```

## Troubleshooting

### Bridge Server Won't Start

**Error: Cannot find module 'androidtvremote2'**
```bash
npm install
```

**Error: Port 3000 already in use**
```bash
# Change port in androidtv-bridge.js
const PORT = 3001;  // Change this line

# Update Hubitat driver preference to match
```

**Error: EACCES permission denied**
```bash
# Use port above 1024 or run with sudo (not recommended)
# Better: use port 3000 or higher
```

### Pairing Issues

**No code appears on TV**

1. Verify Android TV Remote Service is installed:
   - Settings → Apps → See all apps
   - Show system apps
   - Find "Android TV Remote Service"
   - Should be enabled

2. Check bridge logs:
   ```
   Starting pairing for living-room-tv at 192.168.1.100
   ```

3. Verify network:
   - TV and bridge on same network
   - No firewall blocking port 6466
   - Try ping TV from bridge server

**"Invalid code" error**

- Code must be exactly 6 digits
- Don't include spaces or dashes
- Enter within ~60 seconds
- Code is case-sensitive (all digits though)

**Pairing times out**

- Start fresh: Clear Android TV Remote Service data
  - Settings → Apps → Android TV Remote Service
  - Storage → Clear data
- Try pairing again

### Connection Issues

**Bridge shows offline**

```bash
# Test manually
curl http://BRIDGE_IP:3000/health

# Should return:
# {"status":"ok","connectedDevices":0,"uptime":123}
```

**Connected but commands don't work**

1. Check bridge logs for errors
2. Verify deviceId matches
3. Try disconnect/reconnect
4. Check TV is still on

**Connection drops frequently**

- TV might be going to sleep
- Check TV power settings
- Bridge server might be restarting
- Network issues

## Advanced Configuration

### Auto-Start Bridge on Boot

**systemd (Linux):**

```bash
sudo nano /etc/systemd/system/androidtv-bridge.service
```

```ini
[Unit]
Description=Android TV Bridge Server
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/androidtv-bridge
ExecStart=/usr/bin/node /home/pi/androidtv-bridge/androidtv-bridge.js
Restart=always

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable androidtv-bridge
sudo systemctl start androidtv-bridge
sudo systemctl status androidtv-bridge
```

**PM2 (Cross-platform):**

```bash
npm install -g pm2
pm2 start androidtv-bridge.js
pm2 save
pm2 startup
```

### Multiple TVs

1. Create separate device in Hubitat for each TV
2. Use unique deviceId for each
3. Same bridge server handles all
4. Pair each TV individually

Example:
```
TV 1: deviceId = "living-room-tv"
TV 2: deviceId = "bedroom-tv"
TV 3: deviceId = "basement-tv"
```

### Static IP Addresses

Highly recommended to set static IPs:

**For Android TV:**
- Settings → Network → Advanced
- IP Settings → Static
- Enter IP, Gateway, DNS

**For Bridge Server:**
- Configure in router DHCP settings
- Or set static IP on server OS

## Automation Examples

### Rule Machine

**Movie Mode:**
```groovy
Actions:
  - Living Room TV: on
  - Wait 5 seconds
  - Living Room TV: launchApp("plex://")
  - Wait 2 seconds
  - Living Room TV: setVolume(40)
```

**Bedtime:**
```groovy
Actions:
  - Bedroom TV: powerToggle
  - Wait 1 second
  - Bedroom TV: disconnect
```

**Volume Control by Mode:**
```groovy
Trigger: Mode changes
Conditions:
  - IF Mode is "Night"
Actions:
  - All TVs: setVolume(20)
Else-If:
  - IF Mode is "Day"
Actions:
  - All TVs: setVolume(50)
```

### WebCoRE

```
If time is 8:00 PM
  Using Living Room TV
    Turn on
    Wait 5 seconds
    Send command launchApp with "https://www.netflix.com/"
End If
```

## Monitoring

### Check Bridge Health

```bash
# From terminal
curl http://BRIDGE_IP:3000/health

# Response:
{
  "status": "ok",
  "connectedDevices": 2,
  "uptime": 86400
}
```

### Check Device Status

In Hubitat:
```groovy
// Command
getStatus()

// Attributes
connectionStatus: "connected"
bridgeStatus: "online"
paired: "true"
```

### Bridge Logs

```bash
# If running in terminal
# Just read the output

# If running with PM2
pm2 logs androidtv-bridge

# If running with systemd
journalctl -u androidtv-bridge -f
```

## Common App Deep Links

### Netflix
```
https://www.netflix.com/title/TITLE_ID
```

### YouTube
```
vnd.youtube://www.youtube.com/watch?v=VIDEO_ID
```

### Plex
```
plex://
```

### Spotify
```
spotify://
```

### Disney+
```
https://www.disneyplus.com/video/GUID
```

### Finding Deep Links

1. Check app's URL scheme documentation
2. Use ADB to dump package info
3. Search community forums
4. Try common patterns

## Security Notes

### Bridge Server

- Runs on local network only
- No authentication by default
- Don't expose to internet
- Keep Node.js updated

### Stored Credentials

- Certificates stored in Hubitat state
- Base64 encoded
- Not encrypted at rest
- Only shared with bridge server

### Network Security

- Use VLAN isolation if needed
- Firewall bridge from internet
- Consider VPN for remote access

## Performance Tips

1. **Keep bridge server close to TV**
   - Same network switch ideal
   - Reduces latency

2. **Use wired connection**
   - Bridge server wired preferred
   - TV wired if possible

3. **Dedicated device**
   - Raspberry Pi works great
   - Always-on computer
   - Don't run on laptop

4. **Monitor resources**
   - Bridge is lightweight
   - Shouldn't use much CPU/RAM
   - Check logs for errors

## Backup and Recovery

### Backup Hubitat Device

1. Export device settings
2. Save deviceId and IP addresses
3. Document any customizations

### Backup Bridge Server

```bash
# Save pairing certificates
# (Stored by androidtvremote2 library)
cp -r ~/.config/androidtvremote2 ~/backup/

# Save bridge configuration
cp androidtv-bridge.js ~/backup/
cp package.json ~/backup/
```

### Recovery

1. Reinstall bridge server
2. Restore certificates
3. Recreate Hubitat device
4. Enter same deviceId
5. Should reconnect automatically

If not, just re-pair.

## Getting Help

### Resources

- Hubitat Community Forums
- This README
- Bridge server logs
- Hubitat device logs

### Gathering Information

When asking for help, provide:

1. **Bridge Info:**
   - Node.js version: `node --version`
   - Package versions: `npm list`
   - Bridge logs (last 20 lines)

2. **Hubitat Info:**
   - Platform version
   - Device logs
   - Driver version
   - Device preferences

3. **TV Info:**
   - Make and model
   - Android version
   - Network configuration

4. **What's not working:**
   - Specific command or feature
   - Error messages
   - When it started

## Uninstalling

### Remove Hubitat Device

1. Delete virtual device
2. Delete driver code (optional)

### Stop Bridge Server

```bash
# If running in terminal
Ctrl+C

# If using PM2
pm2 stop androidtv-bridge
pm2 delete androidtv-bridge

# If using systemd
sudo systemctl stop androidtv-bridge
sudo systemctl disable androidtv-bridge
```

### Clean Up

```bash
# Remove files
rm -rf androidtv-bridge/

# Remove pairing data from TV
# Settings → Apps → Android TV Remote Service
# Clear data
```

## FAQ

**Q: Why do I need a bridge server?**  
A: Hubitat's Groovy can't handle Protocol Buffers and persistent TLS connections required by Android TV.

**Q: Can I use Raspberry Pi Zero?**  
A: Yes, any model works. Even Pi Zero W is sufficient.

**Q: Does this work with Fire TV?**  
A: No, Fire TV doesn't support Android TV Remote Protocol v2.

**Q: How many TVs can one bridge handle?**  
A: Theoretically unlimited. Tested with 5+. Each TV needs unique deviceId.

**Q: Can I run bridge on same machine as Hubitat?**  
A: Not if you're running Hubitat Elevation hub. Use separate computer.

**Q: Will this work remotely?**  
A: Only if bridge and TV are on same local network. Use VPN for remote access.

**Q: What happens if bridge crashes?**  
A: Commands will fail. Bridge should auto-restart with PM2/systemd. Hubitat will show offline.

**Q: Can I use this with Home Assistant too?**  
A: Yes, Home Assistant has native support. Bridge is only for Hubitat.

## Changelog

### v1.0.0 (2025-02-08)
- Initial release
- Bridge architecture
- Full protocol support
- Pairing implementation
- All key commands
- App launching
- Text input
