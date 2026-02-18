/**
 *  Android TV Remote Device Driver (Bridge Version)
 *  
 *  Copyright 2025
 *  
 *  Licensed under the Apache License, Version 2.0
 *  
 *  Description:
 *  Hubitat driver for Android TV devices using bridge server.
 *  Implements Android TV Remote Protocol v2 via HTTP bridge.
 *  
 *  Features:
 *  - Full remote control (navigation, media, volume, power)
 *  - Real-time state feedback (power, volume, mute, current app)
 *  - App launching with deep links
 *  - Scenes and presets
 *  - Multi-room audio sync
 *  - Configurable polling interval
 *  
 *  Version: 1.0.0
 *  
 *  Requirements:
 *  - Node.js bridge server running (androidtv-bridge.js)
 *  - Android TV with Remote Service enabled (2015+ models)
 *  - NOT compatible with Fire TV devices
 */
 
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition(
        name: "Android TV Remote (Bridge)",
        namespace: "lilavd",
        author: "lilavd@yahoo.com") {
        capability "Switch"
        capability "SwitchLevel"
        capability "Refresh"
        capability "Initialize"
        capability "MediaController"
        
        // Basic Navigation
        command "pressKey", [[name:"key*", type:"ENUM", constraints:[
            "DPAD_UP", "DPAD_DOWN", "DPAD_LEFT", "DPAD_RIGHT", "DPAD_CENTER",
            "ENTER", "BACK", "HOME", "MENU", "SEARCH"
        ]]]
        command "dpadUp"
        command "dpadDown"
        command "dpadLeft"
        command "dpadRight"
        command "dpadCenter"
        command "back"
        command "home"
        command "menu"
        
        // Media Control
        command "play"
        command "pause"
        command "playPause"
        command "stop"
        command "fastForward"
        command "rewind"
        command "nextTrack"
        command "previousTrack"
        
        // Volume Control
        command "volumeUp"
        command "volumeDown"
        command "mute"
        command "unmute"
        command "setVolume", [[name:"volume*", type:"NUMBER"]]
        
        // Power Control
        command "powerToggle"
        command "sleep"
        command "wakeUp"
        
        // Number Keys
        command "sendDigit", [[name:"digit*", type:"NUMBER", description:"0-9"]]
        command "digit0"
        command "digit1"
        command "digit2"
        command "digit3"
        command "digit4"
        command "digit5"
        command "digit6"
        command "digit7"
        command "digit8"
        command "digit9"
        
        // Channel Control
        command "channelUp"
        command "channelDown"
        command "lastChannel"  // Previous channel
        
        // TV Functions
        command "inputSource"  // TV Input/Source selector (key 178)
        command "tvGuide"      // TV Guide/EPG
        command "info"         // Info button
        command "captions"     // Closed captions (CC)
        command "audioTrack"   // Audio track selection (SAP/alternate audio)
        command "period"       // Period/Dot button
        command "getInputs"    // List available inputs
        
        // Direct Input Selection (242-250)
        command "inputAntenna"  // Direct to TV Tuner/Antenna/Cable (242)
        command "inputHdmi1"   // Direct to HDMI 1 (243)
        command "inputHdmi2"   // Direct to HDMI 2 (244)
        command "inputHdmi3"   // Direct to HDMI 3 (245)
        command "inputHdmi4"   // Direct to HDMI 4 (246)
        command "inputComposite1" // Direct to Composite/AV 1 (247)
        command "inputComponent1" // Direct to Component 1 (248)
        command "inputComponent2" // Direct to Component 2 (249)
        command "inputVga"     // Direct to VGA/PC (250)
        command "audioDescription" // Audio Description toggle (252)
        command "switchToInput", [[name:"inputName*", type:"STRING", description:"Antenna, HDMI1, HDMI2, HDMI3, HDMI4, Composite1, Component1, Component2, VGA"]]
        
        // Extended Functions
        command "brightnessDown"
        command "brightnessUp"
        
        // Color Buttons
        command "buttonRed"
        command "buttonGreen"
        command "buttonYellow"
        command "buttonBlue"
        
        // App Control
        command "launchApp", [[name:"appUrl*", type:"STRING"]]
        command "sendText", [[name:"text*", type:"STRING"]]
        
        // MediaController required commands
        command "getAllActivities"
        command "getCurrentActivity"
        command "startActivity", [[name:"activityId*", type:"STRING"]]
        
        // Pairing
        command "startPairing"
        command "completePairing", [[name:"code*", type:"STRING"]]
        command "unpair"
        
        // Connection
        command "checkBridge"
        command "showDiagnostics"
        command "getCurrentState"
        
        // Scenes/Presets
        command "saveScene", [[name:"sceneName*", type:"STRING"]]
        command "executeScene", [[name:"sceneName*", type:"STRING"]]
        command "deleteScene", [[name:"sceneName*", type:"STRING"]]
        command "listScenes"
        
        // Multi-Room Sync
        command "createSyncGroup", [[name:"groupName*", type:"STRING"], [name:"deviceIds*", type:"STRING"]]
        command "sendSyncCommand", [
            [name:"groupName*", type:"STRING"], 
            [name:"commandType*", type:"ENUM", constraints:["key", "app", "volume"]], 
            [name:"commandData*", type:"STRING"]
        ]
        command "deleteSyncGroup", [[name:"groupName*", type:"STRING"]]
        command "listSyncGroups"
        
        attribute "power", "string"
        attribute "volume", "number"
        attribute "muted", "string"
        attribute "connectionStatus", "string"
        attribute "paired", "string"
        attribute "bridgeStatus", "string"
        attribute "currentApp", "string"
        attribute "currentActivity", "string"
        attribute "activities", "JSON_OBJECT"
        attribute "lastStateUpdate", "number"
    }
    
    preferences {
        input name: "deviceIP", type: "text", title: "Android TV IP Address", required: true
        input name: "deviceMAC", type: "text", title: "Android TV MAC Address (for Wake-on-LAN)", description: "Format: AA:BB:CC:DD:EE:FF", required: false
        input name: "bridgeIP", type: "text", title: "Bridge Server IP", required: true
        input name: "bridgePort", type: "number", title: "Bridge Server Port", defaultValue: 3000
        input name: "deviceId", type: "text", title: "Device ID", description: "Unique ID for this TV", required: true
        input name: "deviceName", type: "text", title: "Device Name (for pairing)", defaultValue: "Hubitat"
        input name: "statusInterval", type: "number", title: "Status Check Interval (seconds)", description: "How often to query TV state (5-300 seconds)", defaultValue: 10, range: "5..300"
        input name: "autoConnect", type: "bool", title: "Auto-connect on Initialize", defaultValue: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

// Key code mapping
@Field static final Map KEY_CODES = [
    // Navigation
    "DPAD_UP": 19,
    "DPAD_DOWN": 20,
    "DPAD_LEFT": 21,
    "DPAD_RIGHT": 22,
    "DPAD_CENTER": 23,
    "ENTER": 66,
    "BACK": 4,
    "HOME": 3,
    "MENU": 82,
    "SEARCH": 84,
    
    // Media
    "MEDIA_PLAY": 126,
    "MEDIA_PAUSE": 127,
    "MEDIA_PLAY_PAUSE": 85,
    "MEDIA_STOP": 86,
    "MEDIA_NEXT": 87,
    "MEDIA_PREVIOUS": 88,
    "MEDIA_REWIND": 89,
    "MEDIA_FAST_FORWARD": 90,
    "MEDIA_RECORD": 130,
    
    // Volume
    "VOLUME_UP": 24,
    "VOLUME_DOWN": 25,
    "VOLUME_MUTE": 164,
    
    // Power
    "POWER": 26,
    "SLEEP": 223,
    "WAKEUP": 224,
    
    // Numbers
    "NUM_0": 7,
    "NUM_1": 8,
    "NUM_2": 9,
    "NUM_3": 10,
    "NUM_4": 11,
    "NUM_5": 12,
    "NUM_6": 13,
    "NUM_7": 14,
    "NUM_8": 15,
    "NUM_9": 16,
    
    // Channels
    "CHANNEL_UP": 166,
    "CHANNEL_DOWN": 167,
    "LAST_CHANNEL": 229,  // Previous channel
    
    // TV Functions
    "TV_INPUT": 178,       // Input/Source selector (may not work on all TVs)
    "INPUT_HDMI": 243,     // Alternative HDMI input switch
    "GUIDE": 172,          // TV Guide/EPG
    "INFO": 165,           // Info/Display
    "CAPTIONS": 175,       // Closed Captions/Subtitles
    "CC": 175,             // CC button (same as captions)
    "MEDIA_AUDIO_TRACK": 222, // Audio track selection (includes SAP, alternate languages)
    "SETTINGS": 176,       // Settings
    "PERIOD": 56,          // Period/Dot button
    "DOT": 56,             // Alias for period
    
    // Extended Key Codes (241-252) - TV Input Sources
    // Note: These are the ACTUAL Android key codes, not sequential
    "BRIGHTNESS_DOWN": 241,        // Brightness down
    "TV_ANTENNA_CABLE": 242,       // TV Tuner/Antenna/Cable input
    "TV_INPUT_HDMI_1": 243,        // Direct HDMI 1 input
    "TV_INPUT_HDMI_2": 244,        // Direct HDMI 2 input
    "TV_INPUT_HDMI_3": 245,        // Direct HDMI 3 input
    "TV_INPUT_HDMI_4": 246,        // Direct HDMI 4 input
    "TV_INPUT_COMPOSITE_1": 247,   // Composite/AV 1
    "TV_INPUT_COMPONENT_1": 248,   // Component 1
    "TV_INPUT_COMPONENT_2": 249,   // Component 2
    "TV_INPUT_VGA_1": 250,         // VGA/PC input
    "BRIGHTNESS_UP": 251,          // Brightness up
    "TV_AUDIO_DESCRIPTION": 252,   // Audio Description for visually impaired
    
    // Color Buttons
    "BUTTON_RED": 183,
    "BUTTON_GREEN": 184,
    "BUTTON_YELLOW": 185,
    "BUTTON_BLUE": 186,
    
    // Additional Functions
    "PROG_RED": 183,       // Alias for RED
    "PROG_GREEN": 184,     // Alias for GREEN
    "PROG_YELLOW": 185,    // Alias for YELLOW
    "PROG_BLUE": 186       // Alias for BLUE
]

// Lifecycle
def installed() {
    log.info "Android TV Remote (Bridge) driver installed"
    sendEvent(name: "paired", value: "unknown")
    sendEvent(name: "connectionStatus", value: "disconnected")
    sendEvent(name: "bridgeStatus", value: "unknown")
    initialize()
}

def updated() {
    log.info "Android TV Remote driver updated"
    unschedule()
    if (logEnable) runIn(1800, logsOff)
    initialize()
}

def initialize() {
    log.warn "▶ INITIALIZE: Starting"
    
    if (!deviceIP || !bridgeIP || !deviceId) {
        log.error "✗ INITIALIZE: Missing configuration (IP/Bridge/ID)"
        return
    }
    
    // Check for stored credentials
    def hasCreds = (state.certificate && state.privateKey)
    log.warn "▶ INITIALIZE: Checking credentials... ${hasCreds ? 'FOUND' : 'NOT FOUND'}"
    
    if (hasCreds) {
        // We have credentials - set paired to true
        sendEvent(name: "paired", value: "true", descriptionText: "Device is paired")
        log.warn "▶ INITIALIZE: Set paired='true'"
        
        // Check connection with bridge
        checkBridge()
        
        // After pairing, connection already exists in bridge - no need to connect
        // Just check status
        runIn(2, getStatus)
        
        // Schedule periodic full status checks (configurable interval)
        // This gets ALL state in one call: power, volume, mute, app
        def interval = statusInterval ?: 10
        def cronExpression = "0/${interval} * * * * ?"
        schedule(cronExpression, getStatus)
        
        log.warn "▶ INITIALIZE: ✓ Complete - Device is PAIRED and connected via bridge"
        log.warn "▶ INITIALIZE: Status checks scheduled every ${interval} seconds"
        
    } else {
        // No credentials - not paired
        sendEvent(name: "paired", value: "false", descriptionText: "Device not paired")
        sendEvent(name: "connectionStatus", value: "disconnected")
        log.warn "▶ INITIALIZE: Device NOT paired - use startPairing command"
    }
    
    // Verify
    pauseExecution(500)
    log.warn "▶ INITIALIZE: Current status - paired=${device.currentValue('paired')}, connected=${device.currentValue('connectionStatus')}"
}
def refresh() {
    log.info "Refreshing all state from bridge..."
    checkBridge()
    pauseExecution(500)
    getStatus()  // Gets ALL state: power, volume, mute, app
    
    if (txtEnable) {
        def bridgeStatus = device.currentValue("bridgeStatus")
        def connectionStatus = device.currentValue("connectionStatus")
        def paired = device.currentValue("paired")
        
        log.info "Status - Bridge: ${bridgeStatus}, Connection: ${connectionStatus}, Paired: ${paired}"
    }
}

// Bridge Communication
private String getBridgeUrl() {
    return "http://${bridgeIP}:${bridgePort}"
}

def checkBridge() {
    if (logEnable) log.debug "Checking bridge server"
    
    def params = [
        uri: getBridgeUrl(),
        path: "/health",
        timeout: 5
    ]
    
    try {
        httpGet(params) { resp ->
            if (resp.status == 200) {
                sendEvent(name: "bridgeStatus", value: "online")
                if (txtEnable) log.info "Bridge server is online"
                return true
            }
        }
    } catch (Exception e) {
        log.error "Bridge server unreachable: ${e.message}"
        sendEvent(name: "bridgeStatus", value: "offline")
        return false
    }
}

private Map callBridge(String endpoint, Map body = null, String method = "POST") {
    // Use longer timeout for pairing operations (30 seconds)
    def timeout = endpoint.contains("/pair") ? 30 : 10
    
    def params = [
        uri: getBridgeUrl(),
        path: endpoint,
        contentType: "application/json",
        timeout: timeout
    ]
    
    if (body) {
        params.body = body
    }
    
    def result = null
    
    try {
        if (method == "POST") {
            httpPost(params) { resp ->
                result = handleBridgeResponse(resp)
            }
        } else {
            httpGet(params) { resp ->
                result = handleBridgeResponse(resp)
            }
        }
        return result
    } catch (Exception e) {
        log.error "Bridge call failed (${endpoint}): ${e.message}"
        return [success: false, error: e.message]
    }
}

private Map handleBridgeResponse(resp) {
    if (resp.status == 200 && resp.data) {
        if (logEnable) log.debug "Bridge response: ${resp.data}"
        return resp.data
    } else {
        log.error "Bridge returned error: ${resp.status}"
        return [success: false, error: "HTTP ${resp.status}"]
    }
}

def getStatus() {
    if (logEnable) log.debug "Getting device status from bridge"
    
    // First, verify we have pairing credentials
    if (state.certificate && state.privateKey) {
        sendEvent(name: "paired", value: "true", isStateChange: true)
    } else {
        sendEvent(name: "paired", value: "false")
        sendEvent(name: "connectionStatus", value: "disconnected")
        log.warn "No pairing credentials found"
        return
    }
    
    def result = callBridge("/status/${deviceId}", null, "GET")
    
    if (logEnable) log.debug "Bridge returned: ${result}"
    
    if (result?.success) {
        if (result.connected) {
            sendEvent(name: "connectionStatus", value: "connected", isStateChange: true)
            if (logEnable) log.debug "Status: Connected"
            
            // Update state attributes from bridge feedback
            if (result.state) {
                if (logEnable) log.debug "State object: ${result.state}"
                
                // Power state
                if (result.state.powerState != null) {
                    def powerState = result.state.powerState
                    sendEvent(name: "power", value: powerState, isStateChange: true)
                    sendEvent(name: "switch", value: powerState == "on" ? "on" : "off", isStateChange: true)
                    if (logEnable) log.debug "✓ Power updated: ${powerState}"
                } else {
                    if (logEnable) log.warn "✗ No powerState in response"
                }
                
                // Volume
                if (result.state.volume != null) {
                    sendEvent(name: "volume", value: result.state.volume, isStateChange: true)
                    sendEvent(name: "level", value: result.state.volume, isStateChange: true)
                    if (logEnable) log.debug "✓ Volume updated: ${result.state.volume}"
                } else {
                    if (logEnable) log.warn "✗ No volume in response"
                }
                
                // Muted
                if (result.state.muted != null) {
                    def mutedValue = result.state.muted ? "muted" : "unmuted"
                    sendEvent(name: "muted", value: mutedValue, isStateChange: true)
                    if (logEnable) log.debug "✓ Muted updated: ${mutedValue}"
                } else {
                    if (logEnable) log.warn "✗ No muted in response"
                }
                
                // Current app
                if (result.state.currentApp) {
                    sendEvent(name: "currentApp", value: result.state.currentApp, isStateChange: true)
                    
                    // Also update currentActivity for MediaController
                    def activityName = result.state.currentApp
                    if (activityName.contains("tvlauncher")) activityName = "Home"
                    else if (activityName.contains("netflix")) activityName = "Netflix"
                    else if (activityName.contains("youtube") && !activityName.contains("music")) activityName = "YouTube"
                    else if (activityName.contains("disney")) activityName = "Disney+"
                    else if (activityName.contains("spotify")) activityName = "Spotify"
                    else if (activityName.contains("plex")) activityName = "Plex"
                    else if (activityName.contains("hulu")) activityName = "Hulu"
                    else if (activityName.contains("hbo")) activityName = "HBO Max"
                    else if (activityName.contains("primevideo") || activityName.contains("amazon")) activityName = "Prime Video"
                    
                    sendEvent(name: "currentActivity", value: activityName, isStateChange: true)
                    if (logEnable) log.debug "✓ App updated: ${result.state.currentApp} → Activity: ${activityName}"
                } else {
                    if (logEnable) log.warn "✗ No currentApp in response"
                }
                
                sendEvent(name: "lastStateUpdate", value: now())
                
                if (logEnable) {
                    log.debug "✓ State updated - Power: ${result.state.powerState}, Volume: ${result.state.volume}, Muted: ${result.state.muted}, App: ${result.state.currentApp}"
                }
            } else {
                if (logEnable) log.warn "✗ No state object in response"
            }
        } else {
            sendEvent(name: "connectionStatus", value: "disconnected")
            if (logEnable) log.debug "Status: Disconnected"
        }
    } else {
        log.warn "✗ Bridge call failed or returned error: ${result}"
        // If we can't get status but bridge is online, assume disconnected
        if (device.currentValue("bridgeStatus") == "online") {
            sendEvent(name: "connectionStatus", value: "disconnected")
        }
    }
}

def getPowerState() {
    if (logEnable) log.debug "Querying power state from bridge"
    
    // First check if we're paired
    if (!state.certificate || !state.privateKey) {
        if (logEnable) log.debug "Not paired - skipping power query"
        return
    }
    
    def result = callBridge("/power/${deviceId}", null, "GET")
    
    if (result?.success) {
        if (result.powerState) {
            def powerState = result.powerState
            def currentPower = device.currentValue("power")
            
            if (logEnable) log.debug "Bridge reports power: ${powerState}, Current: ${currentPower}"
            
            // Always update to ensure sync
            sendEvent(name: "power", value: powerState, isStateChange: true)
            sendEvent(name: "switch", value: powerState == "on" ? "on" : "off", isStateChange: true)
            
            if (powerState != currentPower && txtEnable) {
                log.info "Power state changed: ${currentPower} → ${powerState}"
            }
        } else {
            if (logEnable) log.debug "No power state in response: ${result}"
        }
    } else {
        if (logEnable) log.debug "Power query failed - result: ${result}"
    }
}

// Pairing
def startPairing() {
    if (logEnable) log.debug "Starting pairing"
    
    log.warn "=============================================="
    log.warn "PAIRING STARTED"
    log.warn "Look for a 6-digit code on your TV screen"
    log.warn "Then use: completePairing(\"123456\")"
    log.warn "=============================================="
    
    def result = callBridge("/pair/start", [
        deviceId: deviceId,
        host: deviceIP,
        deviceName: deviceName
    ])
    
    if (result?.success) {
        sendEvent(name: "paired", value: "pairing")
        log.warn "Pairing code should be displayed on TV"
        if (result.code) {
            log.warn "Bridge received code: ${result.code}"
        }
    } else {
        log.error "Pairing start failed: ${result?.error}"
        sendEvent(name: "paired", value: "false")
    }
}

def completePairing(String code) {
    if (!code || code.length() != 6) {
        log.error "Invalid code - must be 6 characters"
        return
    }
    
    log.warn "▶ PAIRING: Sending code ${code} to bridge"
    
    def result = callBridge("/pair/complete", [
        deviceId: deviceId,
        code: code
    ])
    
    if (result?.success) {
        log.warn "▶ PAIRING: Bridge confirmed success"
        
        // Store credentials in state
        state.certificate = result.certificate
        state.privateKey = result.privateKey
        
        log.warn "▶ PAIRING: Credentials stored (cert: ${state.certificate?.length()} chars, key: ${state.privateKey?.length()} chars)"
        
        // NOW update the attributes - FORCE the change
        sendEvent(name: "paired", value: "true", descriptionText: "Device successfully paired", isStateChange: true)
        sendEvent(name: "connectionStatus", value: "connected", descriptionText: "Connected to TV via bridge", isStateChange: true)
        
        log.warn "▶ PAIRING: Attributes set - paired='true', connectionStatus='connected'"
        log.warn "▶ PAIRING: ✓ COMPLETE - Device page should now show as paired and connected"
        
        // Verify by reading back
        pauseExecution(1000)
        def pairedStatus = device.currentValue('paired')
        def connStatus = device.currentValue('connectionStatus')
        log.warn "▶ VERIFICATION: paired=${pairedStatus}, connected=${connStatus}"
        
        if (pairedStatus != "true") {
            log.error "✗ WARNING: paired attribute is '${pairedStatus}' but should be 'true' - trying again"
            sendEvent(name: "paired", value: "true", isStateChange: true, displayed: true)
            pauseExecution(500)
            log.warn "▶ RETRY: paired is now ${device.currentValue('paired')}"
        }
        
    } else {
        log.error "✗ PAIRING FAILED: ${result?.error}"
        sendEvent(name: "paired", value: "false", isStateChange: true)
        sendEvent(name: "connectionStatus", value: "disconnected", isStateChange: true)
    }
}

def unpair() {
    if (logEnable) log.debug "Unpairing device"
    
    // Call bridge to unpair
    def result = callBridge("/unpair", [
        deviceId: deviceId
    ])
    
    // Clear local state
    state.remove("certificate")
    state.remove("privateKey")
    
    sendEvent(name: "paired", value: "false")
    sendEvent(name: "connectionStatus", value: "disconnected")
    
    log.warn "=============================================="
    log.warn "DEVICE UNPAIRED"
    log.warn "Cleared from Hubitat and bridge"
    log.warn "Also clear Android TV Remote Service data on TV:"
    log.warn "Settings > Apps > Android TV Remote Service > Clear storage"
    log.warn "=============================================="
    
    if (txtEnable) log.info "Device unpaired"
}

def showDiagnostics() {
    log.info "=== DIAGNOSTICS ==="
    log.info "Device ID: ${deviceId}"
    log.info "TV IP: ${deviceIP}"
    log.info "Bridge IP: ${bridgeIP}:${bridgePort}"
    log.info ""
    log.info "STATE VARIABLES:"
    log.info "  Certificate exists: ${state.certificate ? 'YES (' + state.certificate.take(30) + '...)' : 'NO'}"
    log.info "  Private Key exists: ${state.privateKey ? 'YES (' + state.privateKey.take(30) + '...)' : 'NO'}"
    log.info ""
    log.info "CURRENT ATTRIBUTES:"
    log.info "  paired: ${device.currentValue('paired')}"
    log.info "  connectionStatus: ${device.currentValue('connectionStatus')}"
    log.info "  bridgeStatus: ${device.currentValue('bridgeStatus')}"
    log.info "  power: ${device.currentValue('power')}"
    log.info "  volume: ${device.currentValue('volume')}"
    log.info "  muted: ${device.currentValue('muted')}"
    log.info "  currentApp: ${device.currentValue('currentApp')}"
    log.info ""
    log.info "TESTING BRIDGE:"
    checkBridge()
    pauseExecution(1000)
    log.info "Bridge test complete - check bridgeStatus attribute"
    log.info "==================="
}


def getCurrentState() {
    log.info "=== CURRENT TV STATE ==="
    
    // Force a status refresh first
    getStatus()
    
    pauseExecution(1000)
    
    // Display all state attributes
    log.info "Power: ${device.currentValue('power')}"
    log.info "Switch: ${device.currentValue('switch')}"
    log.info "Volume: ${device.currentValue('volume')}"
    log.info "Muted: ${device.currentValue('muted')}"
    log.info "Current App: ${device.currentValue('currentApp')}"
    log.info "Connection: ${device.currentValue('connectionStatus')}"
    log.info "Paired: ${device.currentValue('paired')}"
    log.info "Last Update: ${device.currentValue('lastStateUpdate')}"
    log.info "======================="
    
    // Return as map for use in rules
    return [
        power: device.currentValue('power'),
        volume: device.currentValue('volume'),
        muted: device.currentValue('muted'),
        currentApp: device.currentValue('currentApp'),
        connected: device.currentValue('connectionStatus'),
        paired: device.currentValue('paired')
    ]
}

// Switch Capability
def on() {
    wakeUp()
}

def off() {
    powerToggle()
}

// Volume
def setLevel(level, duration=0) {
    setVolume(level)
}

def setVolume(volume) {
    // Convert to int and clamp
    def targetVol = Math.max(0, Math.min(100, volume as int))
    
    if (logEnable) log.debug "Setting volume to ${targetVol}"
    
    def currentVolume = device.currentValue("volume") ?: 50
    def steps = targetVol - currentVolume
    
    if (logEnable) log.debug "Current volume: ${currentVolume}, Target: ${targetVol}, Steps: ${steps}"
    
    if (steps > 0) {
        steps.times { 
            volumeUp()
            pauseExecution(150)  // Increased from 100ms
        }
    } else if (steps < 0) {
        Math.abs(steps).times { 
            volumeDown()
            pauseExecution(150)  // Increased from 100ms
        }
    }
    
    // Wait for TV to settle
    pauseExecution(500)
    
    // Get actual volume from TV via bridge
    def result = callBridge("/status/${deviceId}", null, "GET")
    
    if (result?.success && result.state?.volume != null) {
        def actualVolume = result.state.volume
        sendEvent(name: "volume", value: actualVolume)
        sendEvent(name: "level", value: actualVolume)
        
        if (actualVolume != targetVol) {
            log.warn "Volume mismatch: Tried to set ${targetVol}, actual is ${actualVolume}"
            log.warn "This is normal - TV may have different volume steps or limits"
        } else {
            if (txtEnable) log.info "Volume set to ${actualVolume}"
        }
    } else {
        // Fallback: optimistically set to target
        sendEvent(name: "volume", value: targetVol)
        sendEvent(name: "level", value: targetVol)
        if (txtEnable) log.info "Volume set to ${targetVol} (unverified)"
    }
}

// Navigation
def pressKey(String keyName) {
    if (!KEY_CODES.containsKey(keyName)) {
        log.error "Unknown key: ${keyName}"
        return
    }
    
    sendKey(KEY_CODES[keyName], keyName)
}

def dpadUp() { pressKey("DPAD_UP") }
def dpadDown() { pressKey("DPAD_DOWN") }
def dpadLeft() { pressKey("DPAD_LEFT") }
def dpadRight() { pressKey("DPAD_RIGHT") }
def dpadCenter() { pressKey("DPAD_CENTER") }
def back() { pressKey("BACK") }
def home() { pressKey("HOME") }
def menu() { pressKey("MENU") }

// Media
def play() { pressKey("MEDIA_PLAY") }
def pause() { pressKey("MEDIA_PAUSE") }
def playPause() { pressKey("MEDIA_PLAY_PAUSE") }
def stop() { pressKey("MEDIA_STOP") }
def fastForward() { pressKey("MEDIA_FAST_FORWARD") }
def rewind() { pressKey("MEDIA_REWIND") }
def nextTrack() { pressKey("MEDIA_NEXT") }
def previousTrack() { pressKey("MEDIA_PREVIOUS") }

// Volume
def volumeUp() { pressKey("VOLUME_UP") }
def volumeDown() { pressKey("VOLUME_DOWN") }
def mute() { 
    pressKey("VOLUME_MUTE")
    sendEvent(name: "muted", value: "muted")
}
def unmute() { 
    pressKey("VOLUME_MUTE")
    sendEvent(name: "muted", value: "unmuted")
}

// Power
def powerToggle() { 
    pressKey("POWER")
    def current = device.currentValue("switch")
    def newState = current == "on" ? "off" : "on"
    sendEvent(name: "switch", value: newState)
    sendEvent(name: "power", value: newState)
    
    if (logEnable) log.debug "Power toggled to: ${newState}"
}

def sleep() { 
    pressKey("SLEEP")
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "power", value: "off")
}

def wakeUp() {
    if (logEnable) log.debug "Attempting to wake TV"
    
    // Try Wake-on-LAN first if MAC address is configured
    if (deviceMAC) {
        if (logEnable) log.debug "Sending Wake-on-LAN magic packet to ${deviceMAC}"
        sendWOL(deviceMAC)
        pauseExecution(1000)
    }
    
    // Method 1: Send POWER key (works if TV is in light sleep)
    pressKey("POWER")
    pauseExecution(500)
    
    // Method 2: Send HOME key (often more reliable for Android TV)
    pressKey("HOME")
    
    // Update state optimistically
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "power", value: "on")
    
    if (txtEnable) log.info "Wake commands sent to TV"
}

// Number Keys
def sendDigit(digit) {
    def digitNum = digit as int
    if (digitNum < 0 || digitNum > 9) {
        log.error "Digit must be 0-9, got: ${digitNum}"
        return
    }
    pressKey("NUM_${digitNum}")
}

def digit0() { pressKey("NUM_0") }
def digit1() { pressKey("NUM_1") }
def digit2() { pressKey("NUM_2") }
def digit3() { pressKey("NUM_3") }
def digit4() { pressKey("NUM_4") }
def digit5() { pressKey("NUM_5") }
def digit6() { pressKey("NUM_6") }
def digit7() { pressKey("NUM_7") }
def digit8() { pressKey("NUM_8") }
def digit9() { pressKey("NUM_9") }

// Channel Control
def channelUp() { 
    pressKey("CHANNEL_UP")
    if (txtEnable) log.info "Channel up"
}

def channelDown() { 
    pressKey("CHANNEL_DOWN")
    if (txtEnable) log.info "Channel down"
}

def lastChannel() {
    // Try LAST_CHANNEL key (229)
    pressKey("LAST_CHANNEL")
    
    // Philips TVs may not support LAST_CHANNEL key
    // Alternative: Try CHANNEL_UP then CHANNEL_DOWN twice
    // Or use the dedicated TV key if available
    
    if (txtEnable) log.info "Last channel (previous) - Note: May not work on all TVs"
}

// TV Functions
def inputSource() {
    // Opens input selector menu (if supported)
    pressKey("TV_INPUT")
    if (txtEnable) log.info "Input source selector (key 178)"
}

def tvGuide() {
    pressKey("GUIDE")
    if (txtEnable) log.info "TV Guide"
}

def info() {
    pressKey("INFO")
    if (txtEnable) log.info "Info/Display"
}

def captions() {
    pressKey("CAPTIONS")
    if (txtEnable) log.info "Closed captions toggle"
}

def audioTrack() {
    // Audio track selection - opens menu to select alternate audio (SAP, Spanish, descriptive audio, etc.)
    pressKey("MEDIA_AUDIO_TRACK")
    if (txtEnable) log.info "Audio track selection"
}

// Direct Input Selection (242-250)
def inputAntenna() {
    // TV Tuner/Antenna/Cable input
    pressKey("TV_ANTENNA_CABLE")
    if (txtEnable) log.info "Switched to TV Tuner/Antenna/Cable"
}

def inputHdmi1() {
    pressKey("TV_INPUT_HDMI_1")
    if (txtEnable) log.info "Switched to HDMI 1"
}

def inputHdmi2() {
    pressKey("TV_INPUT_HDMI_2")
    if (txtEnable) log.info "Switched to HDMI 2"
}

def inputHdmi3() {
    pressKey("TV_INPUT_HDMI_3")
    if (txtEnable) log.info "Switched to HDMI 3"
}

def inputHdmi4() {
    pressKey("TV_INPUT_HDMI_4")
    if (txtEnable) log.info "Switched to HDMI 4"
}

def inputComposite1() {
    pressKey("TV_INPUT_COMPOSITE_1")
    if (txtEnable) log.info "Switched to Composite/AV 1"
}

def inputComponent1() {
    pressKey("TV_INPUT_COMPONENT_1")
    if (txtEnable) log.info "Switched to Component 1"
}

def inputComponent2() {
    pressKey("TV_INPUT_COMPONENT_2")
    if (txtEnable) log.info "Switched to Component 2"
}

def inputVga() {
    pressKey("TV_INPUT_VGA_1")
    if (txtEnable) log.info "Switched to VGA/PC"
}

def audioDescription() {
    // Audio Description - narrates visual content for visually impaired
    pressKey("TV_AUDIO_DESCRIPTION")
    if (txtEnable) log.info "Audio Description toggle"
}

// Convenience function to switch by name
def switchToInput(String inputName) {
    def inputMap = [
        "Antenna": "TV_ANTENNA_CABLE",
        "HDMI1": "TV_INPUT_HDMI_1",
        "HDMI2": "TV_INPUT_HDMI_2",
        "HDMI3": "TV_INPUT_HDMI_3",
        "HDMI4": "TV_INPUT_HDMI_4",
        "Composite1": "TV_INPUT_COMPOSITE_1",
        "Component1": "TV_INPUT_COMPONENT_1",
        "Component2": "TV_INPUT_COMPONENT_2",
        "VGA": "TV_INPUT_VGA_1"
    ]
    
    def keyCode = inputMap[inputName]
    if (!keyCode) {
        log.error "Unknown input: ${inputName}. Valid: Antenna, HDMI1, HDMI2, HDMI3, HDMI4, Composite1, Component1, Component2, VGA"
        return
    }
    
    pressKey(keyCode)
    if (txtEnable) log.info "Switched to ${inputName}"
}

// Brightness controls
def brightnessDown() {
    pressKey("BRIGHTNESS_DOWN")
    if (txtEnable) log.info "Brightness down"
}

def brightnessUp() {
    pressKey("BRIGHTNESS_UP")
    if (txtEnable) log.info "Brightness up"
}

def period() {
    pressKey("PERIOD")
    if (txtEnable) log.info "Period/Dot"
}

def getInputs() {
    log.warn "=== TV INPUT CONTROLS ==="
    log.warn ""
    log.warn "DIRECT INPUT SELECTION (Key codes 242-250):"
    log.warn "Based on actual Android TV key codes"
    log.warn ""
    log.warn "Available Commands:"
    log.warn "  tv.inputAntenna()    - TV Tuner/Antenna/Cable (key 242)"
    log.warn "  tv.inputHdmi1()      - Switch to HDMI 1 (key 243)"
    log.warn "  tv.inputHdmi2()      - Switch to HDMI 2 (key 244)"
    log.warn "  tv.inputHdmi3()      - Switch to HDMI 3 (key 245)"
    log.warn "  tv.inputHdmi4()      - Switch to HDMI 4 (key 246)"
    log.warn "  tv.inputComposite1() - Switch to Composite/AV 1 (key 247)"
    log.warn "  tv.inputComponent1() - Switch to Component 1 (key 248)"
    log.warn "  tv.inputComponent2() - Switch to Component 2 (key 249)"
    log.warn "  tv.inputVga()        - Switch to VGA/PC (key 250)"
    log.warn ""
    log.warn "Generic Switcher:"
    log.warn "  tv.switchToInput('HDMI1')  - Switch by name"
    log.warn "  Valid names: Antenna, HDMI1, HDMI2, HDMI3, HDMI4,"
    log.warn "               Composite1, Component1, Component2, VGA"
    log.warn ""
    log.warn "Accessibility:"
    log.warn "  tv.audioDescription() - Audio Description toggle (key 252)"
    log.warn ""
    log.warn "Alternative (if direct inputs don't work):"
    log.warn "  tv.inputSource()  - Opens input menu (key 178)"
    log.warn ""
    log.warn "NOTE: Not all TVs have all inputs. Test to see which work."
    log.warn ""
    log.warn "TEST YOUR INPUTS:"
    log.warn "  tv.inputHdmi1()  // Does TV switch to HDMI 1?"
    log.warn "  tv.inputHdmi2()  // Does TV switch to HDMI 2?"
    log.warn "  etc."
    log.warn "==========================="
    
    def availableInputs = [
        [name: "TV Tuner/Antenna", keyCode: 242, command: "inputAntenna()"],
        [name: "HDMI 1", keyCode: 243, command: "inputHdmi1()"],
        [name: "HDMI 2", keyCode: 244, command: "inputHdmi2()"],
        [name: "HDMI 3", keyCode: 245, command: "inputHdmi3()"],
        [name: "HDMI 4", keyCode: 246, command: "inputHdmi4()"],
        [name: "Composite 1", keyCode: 247, command: "inputComposite1()"],
        [name: "Component 1", keyCode: 248, command: "inputComponent1()"],
        [name: "Component 2", keyCode: 249, command: "inputComponent2()"],
        [name: "VGA/PC", keyCode: 250, command: "inputVga()"],
        [name: "Audio Description", keyCode: 252, command: "audioDescription()"]
    ]
    
    log.info "Available input commands:"
    availableInputs.each { input ->
        log.info "  ${input.name}: tv.${input.command} (key ${input.keyCode})"
    }
    
    return availableInputs
}

// Color Buttons
def buttonRed() {
    pressKey("BUTTON_RED")
    if (txtEnable) log.info "Red button"
}

def buttonGreen() {
    pressKey("BUTTON_GREEN")
    if (txtEnable) log.info "Green button"
}

def buttonYellow() {
    pressKey("BUTTON_YELLOW")
    if (txtEnable) log.info "Yellow button"
}

def buttonBlue() {
    pressKey("BUTTON_BLUE")
    if (txtEnable) log.info "Blue button"
}

// Send Wake-on-LAN magic packet
private void sendWOL(String mac) {
    try {
        def macBytes = mac.replaceAll("[:-]", "").decodeHex()
        
        if (macBytes.size() != 6) {
            log.error "Invalid MAC address format: ${mac}"
            return
        }
        
        // Build magic packet: 6 bytes of FF + 16 repetitions of MAC
        def packet = []
        
        // Fill first 6 bytes with 0xFF
        for (int i = 0; i < 6; i++) {
            packet << (byte) 0xFF
        }
        
        // Repeat MAC address 16 times
        for (int i = 0; i < 16; i++) {
            packet.addAll(macBytes)
        }
        
        // Convert to byte array
        byte[] packetBytes = packet as byte[]
        
        // Convert to hex string for HubAction
        def hexString = hubitat.helper.HexUtils.byteArrayToHexString(packetBytes)
        
        // Send broadcast packet
        def hubAction = new hubitat.device.HubAction(
            hexString,
            hubitat.device.Protocol.LAN,
            [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
             destinationAddress: "255.255.255.255:9",
             encoding: hubitat.device.HubAction.Encoding.HEX_STRING]
        )
        
        sendHubCommand(hubAction)
        
        if (logEnable) log.debug "WOL magic packet sent to ${mac}"
        
    } catch (Exception e) {
        log.error "Failed to send WOL packet: ${e.message}"
    }
}

// App Control
def launchApp(String appUrl) {
    if (!appUrl) {
        log.error "App URL required"
        return
    }
    
    if (logEnable) log.debug "Launching app: ${appUrl}"
    
    def result = callBridge("/app/launch", [
        deviceId: deviceId,
        appUrl: appUrl
    ])
    
    if (result?.success) {
        if (txtEnable) log.info "Launched app: ${appUrl}"
    } else {
        log.error "Failed to launch app: ${result?.error}"
    }
}

def sendText(String text) {
    if (!text) {
        log.error "Text required"
        return
    }
    
    if (logEnable) log.debug "Sending text: ${text}"
    
    def result = callBridge("/text", [
        deviceId: deviceId,
        text: text
    ])
    
    if (result?.success) {
        if (txtEnable) log.info "Sent text: ${text}"
    } else {
        log.error "Failed to send text: ${result?.error}"
    }
}

// ====================
// MediaController Required Commands
// ====================

def getAllActivities() {
    // Return list of known apps/activities
    // This is a static list of common Android TV apps
    def activities = [
        [id: "https://www.netflix.com/", name: "Netflix"],
        [id: "vnd.youtube://", name: "YouTube"],
        [id: "https://www.disneyplus.com/", name: "Disney+"],
        [id: "spotify://", name: "Spotify"],
        [id: "plex://", name: "Plex"],
        [id: "https://app.primevideo.com/", name: "Prime Video"],
        [id: "hulu://", name: "Hulu"],
        [id: "hbomax://", name: "HBO Max"],
        [id: "https://www.paramountplus.com/", name: "Paramount+"],
        [id: "https://tv.youtube.com/", name: "YouTube TV"],
        [id: "https://www.peacocktv.com/", name: "Peacock"],
        [id: "https://music.youtube.com/", name: "YouTube Music"],
        [id: "pandora://", name: "Pandora"]
    ]
    
    if (logEnable) log.debug "Returning ${activities.size()} available activities"
    
    // Send event with activities list
    sendEvent(name: "activities", value: groovy.json.JsonOutput.toJson(activities))
    
    return activities
}

def getCurrentActivity() {
    // Query bridge for REAL current state from TV
    if (logEnable) log.debug "Querying bridge for current activity..."
    
    def result = callBridge("/status/${deviceId}", null, "GET")
    
    def currentApp = "unknown"
    
    if (result?.success && result.state?.currentApp) {
        currentApp = result.state.currentApp
        // Update the attribute with real data
        sendEvent(name: "currentApp", value: currentApp)
        if (logEnable) log.debug "Got current app from TV: ${currentApp}"
    } else {
        // Fall back to cached value if bridge query fails
        currentApp = device.currentValue("currentApp") ?: "unknown"
        log.warn "Could not get current app from bridge, using cached: ${currentApp}"
    }
    
    // Map package name to friendly name
    def activityName = currentApp
    if (currentApp.contains("netflix")) activityName = "Netflix"
    else if (currentApp.contains("youtube") && !currentApp.contains("music")) activityName = "YouTube"
    else if (currentApp.contains("disney")) activityName = "Disney+"
    else if (currentApp.contains("spotify")) activityName = "Spotify"
    else if (currentApp.contains("plex")) activityName = "Plex"
    else if (currentApp.contains("amazon") || currentApp.contains("primevideo")) activityName = "Prime Video"
    else if (currentApp.contains("hulu")) activityName = "Hulu"
    else if (currentApp.contains("hbo")) activityName = "HBO Max"
    else if (currentApp.contains("tvlauncher")) activityName = "Home Screen"
    
    // Update currentActivity attribute
    sendEvent(name: "currentActivity", value: activityName)
    
    if (txtEnable) log.info "Current activity: ${activityName} (${currentApp})"
    
    return activityName
}

def startActivity(String activityId) {
    // Start an activity (same as launching an app)
    if (!activityId) {
        log.error "Activity ID required"
        return
    }
    
    if (logEnable) log.debug "Starting activity: ${activityId}"
    
    // If activityId is a deep link, use it directly
    // Otherwise, it might be a friendly name, so map it
    def appUrl = activityId
    
    // Map friendly names to deep links
    switch(activityId.toLowerCase()) {
        case "netflix":
            appUrl = "https://www.netflix.com/"
            break
        case "youtube":
            appUrl = "vnd.youtube://"
            break
        case "disney+":
        case "disney plus":
        case "disneyplus":
            appUrl = "https://www.disneyplus.com/"
            break
        case "spotify":
            appUrl = "spotify://"
            break
        case "plex":
            appUrl = "plex://"
            break
        case "prime video":
        case "prime":
            appUrl = "https://app.primevideo.com/"
            break
        case "hulu":
            appUrl = "hulu://"
            break
        case "hbo max":
        case "hbo":
            appUrl = "hbomax://"
            break
        case "paramount+":
        case "paramount":
            appUrl = "https://www.paramountplus.com/"
            break
        case "youtube tv":
            appUrl = "https://tv.youtube.com/"
            break
        case "peacock":
            appUrl = "https://www.peacocktv.com/"
            break
        case "youtube music":
            appUrl = "https://music.youtube.com/"
            break
        case "pandora":
            appUrl = "pandora://"
            break
        default:
            // Assume it's already a deep link
            appUrl = activityId
    }
    
    launchApp(appUrl)
}

// Send key via bridge
private sendKey(int keyCode, String keyName) {
    if (logEnable) log.debug "Sending key: ${keyName} (${keyCode})"
    
    def result = callBridge("/key", [
        deviceId: deviceId,
        keyCode: keyCode,
        keyName: keyName
    ])
    
    if (!result?.success) {
        log.error "Failed to send key: ${result?.error}"
        
        // If not connected, update status
        if (result?.error?.contains("not connected")) {
            sendEvent(name: "connectionStatus", value: "disconnected")
            log.error "Bridge lost connection - bridge may have restarted"
            log.error "Try: unpair() then re-pair, OR restart bridge and driver"
        }
    }
}

// ====================
// Scenes/Presets
// ====================

def saveScene(String sceneName) {
    if (!sceneName) {
        log.error "Scene name required"
        return
    }
    
    if (logEnable) log.debug "Saving scene: ${sceneName}"
    
    // Capture current state
    def scene = [
        description: "Saved ${new Date()}",
        volume: device.currentValue("volume") ?: 50,
        muted: device.currentValue("muted") == "muted",
        currentApp: device.currentValue("currentApp") ?: "unknown"
    ]
    
    def result = callBridge("/scene/save", [
        sceneName: sceneName,
        scene: scene
    ])
    
    if (result?.success) {
        if (txtEnable) log.info "Scene '${sceneName}' saved"
    } else {
        log.error "Failed to save scene: ${result?.error}"
    }
}

def executeScene(String sceneName) {
    if (!sceneName) {
        log.error "Scene name required"
        return
    }
    
    if (logEnable) log.debug "Executing scene: ${sceneName}"
    
    def result = callBridge("/scene/execute", [
        sceneName: sceneName,
        deviceId: deviceId
    ])
    
    if (result?.success) {
        if (txtEnable) log.info "Scene '${sceneName}' executed"
        
        // Update status after scene execution
        runIn(2, getStatus)
    } else {
        log.error "Failed to execute scene: ${result?.error}"
    }
}

def deleteScene(String sceneName) {
    if (!sceneName) {
        log.error "Scene name required"
        return
    }
    
    if (logEnable) log.debug "Deleting scene: ${sceneName}"
    
    def params = [
        uri: getBridgeUrl(),
        path: "/scene/${sceneName}",
        timeout: 10
    ]
    
    try {
        httpDelete(params) { resp ->
            if (resp.status == 200 && resp.data?.success) {
                if (txtEnable) log.info "Scene '${sceneName}' deleted"
            }
        }
    } catch (Exception e) {
        log.error "Failed to delete scene: ${e.message}"
    }
}

def listScenes() {
    if (logEnable) log.debug "Listing scenes"
    
    def result = callBridge("/scenes", null, "GET")
    
    if (result?.success) {
        log.info "Available scenes (${result.count}):"
        result.scenes.each { scene ->
            log.info "  - ${scene.name}: ${scene.description ?: 'No description'}"
        }
        return result.scenes
    } else {
        log.error "Failed to list scenes"
        return []
    }
}

// ====================
// Multi-Room Sync
// ====================

def createSyncGroup(String groupName, String deviceIds) {
    if (!groupName || !deviceIds) {
        log.error "Group name and device IDs required"
        return
    }
    
    // Parse comma-separated device IDs
    def deviceList = deviceIds.split(',').collect { it.trim() }
    
    if (deviceList.size() < 2) {
        log.error "At least 2 devices required for sync group"
        return
    }
    
    if (logEnable) log.debug "Creating sync group: ${groupName} with devices: ${deviceList}"
    
    def result = callBridge("/sync/create", [
        groupName: groupName,
        deviceIds: deviceList
    ])
    
    if (result?.success) {
        if (txtEnable) log.info "Sync group '${groupName}' created with ${deviceList.size()} devices"
    } else {
        log.error "Failed to create sync group: ${result?.error}"
    }
}

def sendSyncCommand(String groupName, String commandType, String commandData) {
    if (!groupName || !commandType || !commandData) {
        log.error "Group name, command type, and command data required"
        return
    }
    
    if (logEnable) log.debug "Sending ${commandType} command to sync group: ${groupName}"
    
    def command = [type: commandType]
    
    if (commandType == "key") {
        command.keyCode = commandData as int
    } else if (commandType == "app") {
        command.appUrl = commandData
    } else if (commandType == "volume") {
        command.volume = commandData as int
    } else {
        log.error "Invalid command type: ${commandType}"
        return
    }
    
    def result = callBridge("/sync/command", [
        groupName: groupName,
        command: command
    ])
    
    if (result?.success) {
        if (txtEnable) log.info "Sync command sent to group '${groupName}'"
        
        // Log results for each device
        result.results?.each { r ->
            if (logEnable) {
                def status = r.success ? "OK" : "FAIL"
                log.debug "  [${status}] ${r.deviceId}: ${r.error ?: 'Success'}"
            }
        }
    } else {
        log.error "Failed to send sync command: ${result?.error}"
    }
}

def deleteSyncGroup(String groupName) {
    if (!groupName) {
        log.error "Group name required"
        return
    }
    
    if (logEnable) log.debug "Deleting sync group: ${groupName}"
    
    def params = [
        uri: getBridgeUrl(),
        path: "/sync/${groupName}",
        timeout: 10
    ]
    
    try {
        httpDelete(params) { resp ->
            if (resp.status == 200 && resp.data?.success) {
                if (txtEnable) log.info "Sync group '${groupName}' deleted"
            }
        }
    } catch (Exception e) {
        log.error "Failed to delete sync group: ${e.message}"
    }
}

def listSyncGroups() {
    if (logEnable) log.debug "Listing sync groups"
    
    def result = callBridge("/sync/groups", null, "GET")
    
    if (result?.success) {
        log.info "Sync groups (${result.count}):"
        result.groups?.each { group ->
            log.info "  - ${group.name}: ${group.devices.join(', ')}"
        }
        return result.groups
    } else {
        log.error "Failed to list sync groups"
        return []
    }
}

// Utility
def logsOff() {
    log.warn "Debug logging disabled"
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}
