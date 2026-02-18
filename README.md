# Android TV Remote - Hubitat Driver & Bridge

Control your Android TV from Hubitat using the official Android TV Remote Protocol v2.

**Version 1.0.0**

## What This Does

Control your Android TV (Sony, Philips, Nvidia Shield, etc.) from Hubitat with full remote functionality and real-time state feedback - no ADB or developer mode required!

## Features

- ✅ **Full Remote Control** - Navigation, media, volume, power
- ✅ **Real-Time State** - Tracks power, volume, mute, current app
- ✅ **App Launching** - Start Netflix, YouTube, etc. with deep links
- ✅ **Scenes & Presets** - Save and recall TV states
- ✅ **Multi-Room Sync** - Synchronized control of multiple TVs
- ✅ **Easy Setup** - Simple pairing like Google TV app
- ✅ **Configurable** - Adjust polling and timeouts to your needs

## Quick Start

### Requirements
- Android TV device (2015+ models)
- Computer/Pi/NAS to run bridge server
- Node.js v14+
- Hubitat Elevation hub

### 1. Install Bridge

```bash
mkdir androidtv-bridge && cd androidtv-bridge
npm init -y
npm install express body-parser androidtv-remote
# Copy androidtv-bridge.js here
node androidtv-bridge.js
```

### 2. Install Driver

1. Hubitat → Drivers Code → New Driver
2. Paste `Android_TV_Remote_Bridge_Driver.groovy`
3. Save

### 3. Create Device

1. Devices → Add Device → Virtual
2. Type: "Android TV Remote (Bridge)"
3. Configure: TV IP, Bridge IP, Device ID
4. Save

### 4. Pair

1. Click `startPairing`
2. Enter 6-digit code from TV
3. Click `completePairing("code")`
4. Done!

## Documentation

| Document | Description |
|----------|-------------|
| [INSTALLATION.md](INSTALLATION.md) | Complete setup guide |
| [CONFIGURATION.md](CONFIGURATION.md) | All configuration options |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Common issues & fixes |
| [LIMITATIONS.md](LIMITATIONS.md) | Known limitations by TV brand |
| [DOCKER_SETUP.md](DOCKER_SETUP.md) | Docker/QNAP setup |

## Compatibility

### ✅ Works With
- Sony Bravia Android TV
- Philips Android TV
- Nvidia Shield TV
- Mi Box
- Most Android TV devices (2015+)

### ❌ Does NOT Work With
- Fire TV (different protocol)
- Roku, Apple TV, regular Smart TVs

### ⚠️ Limitations
- **Philips TVs:** Input switching (HDMI1-4) doesn't work - use HDMI-CEC instead
- **All TVs:** "Last Channel" may not work on some models

## Support

1. Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
2. Enable debug logging
3. Check [Hubitat Forums](https://community.hubitat.com/)
4. Open GitHub issue with logs

## License

Apache License 2.0

---

**v1.0.0** - Full Android TV control for Hubitat! 📺✨
