# Android TV Bridge - Troubleshooting Pairing Issues

## Common Error: "No pairing in progress"

This error occurs when the pairing flow is interrupted or timed out.

### Solution Steps

#### 1. **Verify Bridge is Running**

Check bridge logs in Container Station:
```
Android TV Remote Bridge Server
====================================
Server running on port 3000
```

#### 2. **Complete Pairing Flow Properly**

The pairing MUST be done in this exact order:

**Step 1: Start Pairing**
```
1. In Hubitat device page
2. Click "startPairing" command
3. Check Hubitat logs for:
   "PAIRING STARTED"
   "Look for a 6-digit code on your TV screen"
```

**Step 2: Wait for TV Code**
```
1. Look at your Android TV screen
2. Code should appear within 5-10 seconds
3. If no code appears, see troubleshooting below
```

**Step 3: Complete Pairing**
```
1. Note the 6-digit code from TV
2. In Hubitat device page
3. Click "completePairing" command
4. Enter code EXACTLY as shown (6 digits)
5. Submit within 60 seconds
```

#### 3. **If Code Doesn't Appear on TV**

**Check Android TV Remote Service:**
```
1. On TV: Settings → Apps → See all apps
2. Enable "Show system apps"
3. Find "Android TV Remote Service"
4. Make sure it's:
   - Installed (should be pre-installed)
   - Enabled
   - Not disabled
   - Has permissions granted
```

**Clear Service Data (if needed):**
```
1. Settings → Apps → Android TV Remote Service
2. Storage & cache
3. Clear storage
4. Clear cache
5. Force stop
6. Try pairing again
```

#### 4. **If Still Not Working**

**Check Bridge Logs:**

SSH into QNAP or view Container Station logs:
```
# Should see:
[deviceId] Starting pairing at 192.168.1.XXX
[deviceId] Pairing code displayed on TV
```

**If you see errors like:**
- `ECONNREFUSED` → TV IP is wrong or TV is off
- `ETIMEDOUT` → Firewall blocking port 6467
- `Connection reset` → Android TV Remote Service not running

**Test TV Connectivity:**
```bash
# From QNAP SSH or bridge container
ping YOUR_TV_IP

# Test if port 6466 is open
nc -zv YOUR_TV_IP 6466
```

## Network Issues

### Firewall Blocking

**QNAP Firewall:**
```
1. QNAP Control Panel → Security → Firewall
2. Add rule to allow outbound to TV IP on ports 6466 and 6467
```

**TV Firewall:**
```
Some Android TVs have firewall settings
1. Settings → Network → Advanced
2. Look for firewall or security settings
3. Make sure ports 6466-6467 are open
```

### Different Subnets/VLANs

If QNAP and TV are on different networks:
```
1. Put them on same network/VLAN
2. Or configure routing between VLANs
3. Or use bridge network mode: host
```

## Timing Issues

### Pairing Timeout

The pairing code is only valid for ~60 seconds.

**If you're too slow:**
```
1. Click "startPairing" again
2. NEW code will appear on TV
3. Use the NEW code, not the old one
```

### Multiple Pairing Attempts

**If you've tried pairing multiple times:**
```
1. Clear TV's Android TV Remote Service data
2. Restart bridge container
3. Try fresh pairing from scratch
```

## Certificate Issues

### "Device not paired" Error After Successful Pairing

**Check Hubitat device state:**
```
1. Open device page
2. Click "State Variables"
3. Look for:
   - certificate: should have JSON data
   - privateKey: should have JSON data
```

**If missing:**
```
Pairing didn't complete properly
1. Clear pairing and try again
2. Make sure to wait for "PAIRING SUCCESSFUL" in logs
```

## Debugging Steps

### Enable Detailed Logging

**In Hubitat Driver:**
```
Device Preferences:
- Enable debug logging: ON
- Enable descriptionText logging: ON
- Save Preferences
```

**View Logs:**
```
Hubitat → Logs → Click device to filter
```

**In Bridge Server:**

Bridge logs are automatic. View them:
```
Container Station → Containers → androidtv-bridge → Logs tab
```

### Test Bridge Endpoints Manually

From any computer on your network:

**Test Health:**
```bash
curl http://QNAP_IP:3000/health
```

Should return:
```json
{
  "status": "ok",
  "connectedDevices": 0,
  "pairingInProgress": 0,
  "uptime": 123
}
```

**Test Pairing Start:**
```bash
curl -X POST http://QNAP_IP:3000/pair/start \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "test-tv",
    "host": "TV_IP_ADDRESS",
    "deviceName": "Test"
  }'
```

Should return:
```json
{
  "success": true,
  "message": "Pairing initiated - check TV for 6-digit code"
}
```

**Check if pairing is in progress:**
```bash
curl http://QNAP_IP:3000/devices
```

Should show:
```json
{
  "devices": [
    {
      "deviceId": "pairing_test-tv",
      "type": "pairing"
    }
  ]
}
```

### Common Error Messages

#### "Cannot find module 'androidtv-remote'"

**Fix:**
```
1. Stop container
2. Delete container
3. Make sure package.json has:
   "androidtv-remote": "^1.0.10"
4. Recreate container
5. npm install will run automatically
```

#### "ECONNREFUSED"

**Meaning:** Can't connect to TV

**Fix:**
```
1. Verify TV IP address is correct
2. Ping TV from QNAP
3. Make sure TV is powered on
4. Check if TV is on same network
```

#### "ETIMEDOUT"

**Meaning:** Connection timeout

**Fix:**
```
1. Firewall blocking connection
2. TV is off or unreachable
3. Wrong IP address
4. Network routing issue
```

#### "Invalid code or pairing timeout"

**Meaning:** Code was wrong or you were too slow

**Fix:**
```
1. Start pairing again (new code)
2. Enter code faster (within 60 seconds)
3. Make sure code is exactly 6 digits
4. Don't include spaces or dashes
```

## Complete Reset Procedure

If nothing else works, do a complete reset:

### 1. Clear Everything

**On Hubitat:**
```
1. Open device page
2. Remove any saved state:
   - Click "State Variables"
   - Delete certificate and privateKey if present
3. Or just delete device and recreate
```

**On QNAP:**
```
1. Stop androidtv-bridge container
2. Delete container
3. Delete /share/Container/androidtv-bridge folder
4. Recreate everything from scratch
```

**On Android TV:**
```
1. Settings → Apps → Android TV Remote Service
2. Storage → Clear storage
3. Clear cache
4. Force stop
5. Restart TV
```

### 2. Start Fresh

```
1. Upload files to QNAP
2. Create container
3. Verify bridge is running (check /health)
4. Start pairing process
5. Complete within 60 seconds
```

## Quick Checklist

Before attempting pairing:

✓ Bridge container is running
✓ Bridge shows "Server running on port 3000"
✓ Bridge health endpoint returns "ok"
✓ TV is powered on
✓ TV Android TV Remote Service is enabled
✓ TV and QNAP are on same network
✓ Can ping TV from QNAP
✓ Hubitat device has correct IPs configured:
  - Android TV IP
  - Bridge Server IP (QNAP)
✓ Device ID is unique
✓ Ready to enter code quickly (within 60 seconds)

## Still Having Issues?

### Gather This Information:

1. **Bridge Logs** (last 20 lines from Container Station)
2. **Hubitat Logs** (from device page)
3. **TV Model and Android Version**
4. **Network Setup** (same subnet? VLANs?)
5. **Exact Error Message**
6. **What you've tried so far**

### Alternative Approaches:

If the bridge approach isn't working, consider:

1. **Different Library**: Try the Python-based approach with Home Assistant
2. **ADB Method**: Enable developer mode and use ADB
3. **IR Control**: Use IR blaster for basic commands
4. **CEC Control**: If TV supports HDMI-CEC

## Success Indicators

When pairing works correctly, you'll see:

**Hubitat Logs:**
```
PAIRING STARTED
Look for a 6-digit code on your TV screen
...
PAIRING SUCCESSFUL!
Credentials have been saved
```

**Bridge Logs:**
```
[deviceId] Starting pairing at 192.168.1.100
[deviceId] Pairing code displayed on TV
[deviceId] Completing pairing with code: 123456
[deviceId] Code sent, waiting for pairing to complete...
[deviceId] Pairing successful!
```

**TV Screen:**
```
Shows 6-digit code briefly
Then goes away after code accepted
```

**Hubitat Device Attributes:**
```
paired: true
connectionStatus: connected (after running connect)
bridgeStatus: online
```
