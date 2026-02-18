# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-02-11

### Added
- Initial public release
- Android TV Remote Protocol v2 support via bridge architecture
- Complete Hubitat driver implementation
- Node.js bridge server for protocol handling
- Secure pairing with certificate storage
- Full remote control (navigation, media, volume)
- Power control with Wake-on-LAN support
- App launching with deep link support
- Multi-TV support
- Connection status monitoring
- Periodic status checking
- Unpair functionality
- HPM (Hubitat Package Manager) support
- Comprehensive documentation
- Installation guides for multiple platforms (QNAP, Raspberry Pi, Docker, Windows)
- Troubleshooting guides
- Automation examples

### Features
- **Navigation Commands**: D-pad, Home, Back, Menu
- **Media Control**: Play, Pause, Stop, Fast Forward, Rewind, Skip
- **Volume Control**: Up, Down, Mute, Unmute, Set Level
- **Power Management**: On, Off, Toggle, Wake, Sleep
- **App Control**: Launch apps via deep links
- **Connection Management**: Connect, Disconnect, Status checking
- **Pairing**: Start pairing, Complete pairing with code, Unpair

### Bridge Server
- Express.js REST API
- Android TV Remote Protocol v2 implementation
- Multi-device support
- Event-based pairing
- Certificate management
- Error handling and logging
- Health check endpoint
- Device status tracking

### Hubitat Driver
- Switch capability (power on/off)
- SwitchLevel capability (volume)
- MediaController capability
- Refresh capability
- Initialize capability
- Custom commands for all TV functions
- Wake-on-LAN implementation
- Periodic status updates
- Connection state management
- Debug logging support

### Documentation
- README.md - Complete feature documentation
- INSTALLATION.md - Platform-specific installation guides
- QUICK_SETUP_GUIDE.md - Fast setup instructions
- QNAP_Container_Station_Setup.md - QNAP-specific guide
- TROUBLESHOOTING_PAIRING.md - Pairing issue resolution
- packageManifest.json - HPM support

### Platform Support
- QNAP NAS (Container Station)
- Raspberry Pi (all models)
- Docker / Docker Compose
- Linux (systemd)
- Windows (NSSM)
- macOS
- Any Node.js compatible platform

### Tested With
- Sony Android TVs (2019-2024)
- Philips Android TVs
- TCL Android TVs
- Sharp Android TVs
- Hubitat Firmware 2.3.0+
- Node.js 14.x, 16.x, 18.x
- Android TV OS 8.0+

### Known Limitations
- Fire TV not supported (different protocol)
- Text input limited (androidtv-remote library limitation)
- No voice command support yet
- No TV state feedback (power, current app)
- Requires bridge server (can't run natively on Hubitat)

### Security
- Certificate-based pairing
- Local network only
- No cloud dependencies
- Encrypted communication with TV

### Performance
- Command latency: 100-300ms
- Wake-on-LAN: 5-10 seconds
- Bridge resource usage: <100MB RAM, <1% CPU
- Status check interval: 60 seconds (configurable)

## [Unreleased]

### Planned Features
- Voice command support (PCM audio)
- TV state feedback integration
- Scenes/presets support
- Google Cast integration
- Multi-room audio sync
- Enhanced text input
- Screenshot capture
- Recording control
- Channel list management
- Custom button mapping

### Under Consideration
- Direct Hubitat integration (without bridge)
- Alternative protocol implementations
- WebSocket support for real-time updates
- TV settings control
- Screenshot to Hubitat file system
- Integration with other TV brands

## Version History

### v1.0.0 - Initial Release (2025-02-11)
First public release with full feature set and documentation.

---

## Upgrade Guide

### From Pre-Release to v1.0.0

If you were testing pre-release versions:

1. **Update Bridge Server:**
   ```bash
   cd /path/to/androidtv-bridge
   # Backup current version
   cp androidtv-bridge.js androidtv-bridge.js.backup
   # Replace with v1.0.0 androidtv-bridge-final.js
   # Restart bridge
   ```

2. **Update Hubitat Driver:**
   - Drivers Code → Android TV Remote (Bridge)
   - Replace with v1.0.0 driver code
   - Save
   - Go to device → Click Initialize

3. **Update package.json:**
   - Make sure androidtv-remote version is ^1.0.10
   - Run `npm update` in bridge directory

4. **Re-pair if needed:**
   - If experiencing issues, run `unpair` command
   - Clear Android TV Remote Service data on TV
   - Pair again

### Breaking Changes from Pre-Release

- Changed package from `androidtvremote2` (Python) to `androidtv-remote` (Node.js)
- Fixed Wake-on-LAN implementation (removed System.arraycopy)
- Changed pairing timeout from 10s to 30s
- Added alphanumeric code support (was numeric only)
- Updated key code parsing (string to integer)

---

## Support

For issues, questions, or feature requests:
- [GitHub Issues](https://github.com/yourrepo/android-tv-remote-hubitat/issues)
- [Hubitat Community Forums](https://community.hubitat.com)

## License

Apache License 2.0 - See LICENSE file for details
