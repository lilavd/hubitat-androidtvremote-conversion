/**
 * Android TV Remote - Bridge Server
 * 
 * This Node.js server acts as a bridge between Hubitat and Android TV.
 * It handles the complex Protocol Buffer and TLS requirements that
 * Hubitat's Groovy environment cannot natively support.
 * 
 * Features:
 * - Android TV Remote Protocol v2 support
 * - Real-time TV state feedback (power, volume, mute, current app)
 * - Automatic reconnection with keepalive
 * - Configurable via config.json or environment variables
 * - Scenes and presets support
 * - Multi-room audio sync
 * 
 * Version: 1.0.0
 * 
 * Installation:
 * npm install express body-parser androidtv-remote
 * 
 * Usage:
 * node androidtv-bridge.js
 * 
 * Configuration:
 * - Create config.json in same directory (see config.json.example)
 * - Or use environment variables (see README.md)
 */

const express = require('express');
const bodyParser = require('body-parser');
const AndroidRemote = require('androidtv-remote').AndroidRemote;
const RemoteKeyCode = require('androidtv-remote').RemoteKeyCode;
const RemoteDirection = require('androidtv-remote').RemoteDirection;
const fs = require('fs');
const path = require('path');

const app = express();

// Load configuration from config.json if it exists, otherwise use env vars
let config = {
    port: 3000,
    keepaliveInterval: 30,
    reconnectDelay: 5,
    minReconnectInterval: 30,
    activityTimeout: 60,
    statePollInterval: 10
};

const configPath = path.join(__dirname, 'config.json');
if (fs.existsSync(configPath)) {
    try {
        const fileConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'));
        config = { ...config, ...fileConfig };
        console.log('✓ Loaded configuration from config.json');
    } catch (error) {
        console.warn('⚠ Error reading config.json, using defaults:', error.message);
    }
}

// Environment variables override config.json
const PORT = process.env.BRIDGE_PORT || config.port;
const KEEPALIVE_INTERVAL = (process.env.KEEPALIVE_INTERVAL ? parseInt(process.env.KEEPALIVE_INTERVAL) : config.keepaliveInterval) * 1000;
const RECONNECT_DELAY = (process.env.RECONNECT_DELAY ? parseInt(process.env.RECONNECT_DELAY) : config.reconnectDelay) * 1000;
const MIN_RECONNECT_INTERVAL = (process.env.MIN_RECONNECT_INTERVAL ? parseInt(process.env.MIN_RECONNECT_INTERVAL) : config.minReconnectInterval) * 1000;
const ACTIVITY_TIMEOUT = (process.env.ACTIVITY_TIMEOUT ? parseInt(process.env.ACTIVITY_TIMEOUT) : config.activityTimeout) * 1000;
const STATE_POLL_INTERVAL = (process.env.STATE_POLL_INTERVAL ? parseInt(process.env.STATE_POLL_INTERVAL) : config.statePollInterval) * 1000;

console.log('='.repeat(70));
console.log('Android TV Remote Bridge Server v1.0');
console.log('='.repeat(70));
console.log(`Configuration ${fs.existsSync(configPath) ? '(from config.json)' : '(defaults)'}:`);
console.log(`  Port: ${PORT}`);
console.log(`  Keepalive Interval: ${KEEPALIVE_INTERVAL / 1000}s`);
console.log(`  Reconnect Delay: ${RECONNECT_DELAY / 1000}s`);
console.log(`  Min Reconnect Interval: ${MIN_RECONNECT_INTERVAL / 1000}s`);
console.log(`  Activity Timeout: ${ACTIVITY_TIMEOUT / 1000}s`);
console.log(`  State Poll Interval: ${STATE_POLL_INTERVAL / 1000}s`);
console.log('='.repeat(70));

// CRITICAL: Body parser middleware
app.use(bodyParser.json({ limit: '10mb' }));
app.use(bodyParser.urlencoded({ extended: true }));

// Storage
const devices = new Map(); // Store TV connections
const scenes = new Map(); // Store scenes/presets
const syncGroups = new Map(); // Store multi-room sync groups

// Request logging middleware
app.use((req, res, next) => {
    console.log(`\n[${new Date().toISOString()}] ${req.method} ${req.path}`);
    if (req.body && Object.keys(req.body).length > 0) {
        console.log('Request body:', JSON.stringify(req.body, null, 2));
    }
    next();
});

// ====================
// State Management
// ====================

// Initialize device state tracking
function initializeDeviceState(deviceId, remote, host) {
    const state = {
        deviceId: deviceId,
        host: host,
        remote: remote,
        connected: false,
        powerState: 'unknown',
        volume: 0,
        muted: false,
        currentApp: 'unknown',
        lastActivity: Date.now(),
        pollInterval: null,
        stateListeners: [],
        lastStateUpdate: null
    };
    
    devices.set(deviceId, state);
    
    // Set up event listeners on the remote
    setupRemoteEventListeners(deviceId, remote);
    
    return state;
}

// Set up all event listeners for a remote instance
function setupRemoteEventListeners(deviceId, remote) {
    console.log(`[${deviceId}] Setting up event listeners...`);
    
    const deviceState = devices.get(deviceId);
    
    remote.on('ready', () => {
        console.log(`[${deviceId}] ✓ Remote ready`);
        if (deviceState) {
            deviceState.connected = true;
            deviceState.lastActivity = Date.now();
        }
    });
    
    remote.on('error', (error) => {
        console.error(`[${deviceId}] Remote error:`, error.message);
        
        // If connection error, mark as disconnected and attempt reconnect
        if (deviceState) {
            deviceState.connected = false;
            
            // Don't spam reconnects - only if it's been a while
            const timeSinceLastReconnect = Date.now() - (deviceState.lastReconnectAttempt || 0);
            if (timeSinceLastReconnect > MIN_RECONNECT_INTERVAL) {
                console.log(`[${deviceId}] Scheduling reconnect...`);
                deviceState.lastReconnectAttempt = Date.now();
                
                setTimeout(async () => {
                    console.log(`[${deviceId}] Attempting auto-reconnect...`);
                    try {
                        await remote.start();
                        console.log(`[${deviceId}] ✓ Auto-reconnect successful`);
                    } catch (reconnectError) {
                        console.error(`[${deviceId}] Auto-reconnect failed:`, reconnectError.message);
                    }
                }, RECONNECT_DELAY);
            }
        }
    });
    
    remote.on('unpaired', () => {
        console.warn(`[${deviceId}] ! Device unpaired`);
        if (deviceState) {
            deviceState.connected = false;
        }
    });
    
    remote.on('powered', (powered) => {
        console.log(`[${deviceId}] Power state: ${powered}`);
        if (deviceState) {
            deviceState.powerState = powered ? 'on' : 'off';
            deviceState.lastActivity = Date.now();
        }
    });
    
    remote.on('volume', (volume) => {
        console.log(`[${deviceId}] Volume: ${volume.level}, Muted: ${volume.muted}`);
        if (deviceState) {
            deviceState.volume = volume.level || 0;
            deviceState.muted = volume.muted || false;
            deviceState.lastActivity = Date.now();
        }
    });
    
    remote.on('current_app', (app) => {
        console.log(`[${deviceId}] Current app: ${app}`);
        if (deviceState) {
            deviceState.currentApp = app || 'unknown';
            deviceState.lastActivity = Date.now();
        }
    });
    
    // Handle RAW data events from TV for real-time state
    remote.on('data', (data) => {
        try {
            if (!deviceState) return;
            
            deviceState.lastActivity = Date.now();
            
            // App changes (remoteImeKeyInject)
            if (data.remoteImeKeyInject && data.remoteImeKeyInject.appInfo) {
                const appPackage = data.remoteImeKeyInject.appInfo.appPackage;
                console.log(`[${deviceId}] 📱 App changed: ${appPackage}`);
                deviceState.currentApp = appPackage;
            }
            
            // Volume changes (remoteSetVolumeLevel)
            if (data.remoteSetVolumeLevel) {
                const vol = data.remoteSetVolumeLevel;
                console.log(`[${deviceId}] 🔊 Volume: ${vol.volumeLevel}, Muted: ${vol.volumeMuted}`);
                deviceState.volume = vol.volumeLevel || 0;
                deviceState.muted = vol.volumeMuted || false;
            }
            
        } catch (e) {
            console.error(`[${deviceId}] Error parsing TV event:`, e.message);
        }
    });
    
    // Start keepalive checks every 30 seconds
    if (deviceState && !deviceState.keepaliveInterval) {
        console.log(`[${deviceId}] Starting keepalive checks (${KEEPALIVE_INTERVAL / 1000}s interval)`);
        deviceState.keepaliveInterval = setInterval(() => {
            try {
                if (remote && deviceState.connected) {
                    // Library maintains connection automatically - no ping needed
                    // Just update lastActivity to track we're alive
                    deviceState.lastActivity = Date.now();
                    console.log(`[${deviceId}] ⏱️  Keepalive check - connection active`);
                }
            } catch (error) {
                console.warn(`[${deviceId}] Keepalive check error:`, error.message);
            }
        }, KEEPALIVE_INTERVAL);
    }
    
    console.log(`[${deviceId}] ✓ Event listeners configured`);
}

// Poll device state
async function updateDeviceState(deviceId) {
    const deviceState = devices.get(deviceId);
    if (!deviceState || !deviceState.remote) {
        return null;
    }
    
    try {
        // If we have a remote object and have received any activity, we're connected
        // Don't rely on isReady() as it can be unreliable
        const hasRecentActivity = deviceState.lastActivity && (Date.now() - deviceState.lastActivity) < ACTIVITY_TIMEOUT;
        deviceState.connected = (deviceState.remote && hasRecentActivity) || deviceState.connected;
        
        // Power state is tracked via 'powered' event listener
        // No need to poll - the event is reliable
        
        const state = {
            deviceId: deviceId,
            connected: deviceState.connected,
            powerState: deviceState.powerState || 'unknown',
            volume: deviceState.volume || 0,
            muted: deviceState.muted || false,
            currentApp: deviceState.currentApp || 'unknown',
            lastActivity: deviceState.lastActivity,
            timestamp: Date.now()
        };
        
        deviceState.lastStateUpdate = state;
        
        // Notify listeners
        deviceState.stateListeners.forEach(callback => {
            try {
                callback(state);
            } catch (e) {
                console.error(`[${deviceId}] State listener error:`, e);
            }
        });
        
        return state;
        
    } catch (error) {
        console.error(`[${deviceId}] State update error:`, error.message);
        return null;
    }
}

// Start state polling
function startStatePolling(deviceId) {
    const deviceState = devices.get(deviceId);
    if (!deviceState) return;
    
    // Clear existing interval
    if (deviceState.pollInterval) {
        clearInterval(deviceState.pollInterval);
    }
    
    // Set up new polling
    deviceState.pollInterval = setInterval(async () => {
        await updateDeviceState(deviceId);
    }, STATE_POLL_INTERVAL);
    
    console.log(`[${deviceId}] State polling started (${STATE_POLL_INTERVAL}ms)`);
}

// Stop state polling
function stopStatePolling(deviceId) {
    const deviceState = devices.get(deviceId);
    if (deviceState && deviceState.pollInterval) {
        clearInterval(deviceState.pollInterval);
        deviceState.pollInterval = null;
        console.log(`[${deviceId}] State polling stopped`);
    }
}

// Track state changes from commands
function trackPowerState(deviceId, state) {
    const deviceState = devices.get(deviceId);
    if (deviceState) {
        deviceState.powerState = state;
        deviceState.lastActivity = Date.now();
        updateDeviceState(deviceId);
    }
}

function trackVolume(deviceId, volume, muted = null) {
    const deviceState = devices.get(deviceId);
    if (deviceState) {
        if (volume !== null && volume !== undefined) {
            deviceState.volume = volume;
        }
        if (muted !== null && muted !== undefined) {
            deviceState.muted = muted;
        }
        deviceState.lastActivity = Date.now();
        updateDeviceState(deviceId);
    }
}

function trackApp(deviceId, appUrl) {
    const deviceState = devices.get(deviceId);
    if (deviceState) {
        deviceState.currentApp = appUrl;
        deviceState.lastActivity = Date.now();
        updateDeviceState(deviceId);
    }
}

// ====================
// Pairing Endpoints
// ====================

// Start pairing
app.post('/pair/start', async (req, res) => {
    console.log('\n=== PAIRING START REQUEST ===');
    
    try {
        const deviceId = req.body.deviceId;
        const host = req.body.host;
        const deviceName = req.body.deviceName || 'Hubitat';
        
        if (!deviceId || !host) {
            throw new Error('Missing required parameters: deviceId, host');
        }
        
        console.log(`Device ID: ${deviceId}`);
        console.log(`Host: ${host}`);
        console.log(`Device Name: ${deviceName}`);
        
        // Create AndroidRemote instance for pairing
        console.log(`[${deviceId}] Creating AndroidRemote instance...`);
        const remote = new AndroidRemote(host, {
            name: deviceName
        });
        
        // Track pairing state
        const pairingState = {
            remote: remote,
            host: host,
            codeDisplayed: false,
            deviceId: deviceId
        };
        
        // Set up event listeners for pairing
        remote.on('secret', () => {
            console.log(`[${deviceId}] âœ" Pairing code displayed on TV`);
            pairingState.codeDisplayed = true;
        });
        
        remote.on('ready', () => {
            console.log(`[${deviceId}] âœ" Remote ready`);
        });
        
        remote.on('unpaired', () => {
            console.log(`[${deviceId}] ! Device unpaired`);
        });
        
        remote.on('error', (error) => {
            console.error(`[${deviceId}] Remote error:`, error);
        });
        
        // Store pairing state temporarily
        devices.set(`pairing_${deviceId}`, pairingState);
        
        console.log(`[${deviceId}] Starting remote.start()...`);
        
        // Start pairing
        await remote.start();
        
        console.log(`[${deviceId}] âœ" remote.start() completed`);
        
        // Give it a brief moment for events to fire
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        console.log(`[${deviceId}] Pairing initiated successfully`);
        console.log(`[${deviceId}] Code displayed: ${pairingState.codeDisplayed}`);
        
        res.json({
            success: true,
            message: 'Pairing initiated - check TV for 6-digit code',
            deviceId: deviceId,
            codeDisplayed: pairingState.codeDisplayed
        });
        
    } catch (error) {
        console.error('âœ— PAIRING START FAILED:', error.message);
        console.error('Error details:', error);
        res.status(500).json({
            success: false,
            error: error.message || 'Failed to start pairing'
        });
    }
});

// Complete pairing
app.post('/pair/complete', async (req, res) => {
    console.log('\n=== PAIRING COMPLETE REQUEST ===');
    
    try {
        const deviceId = req.body.deviceId;
        const code = req.body.code;
        
        if (!deviceId || !code) {
            throw new Error('Missing required parameters: deviceId, code');
        }
        
        console.log(`Device ID: ${deviceId}`);
        console.log(`Code: ${code}`);
        
        // Validate code format - can be alphanumeric (letters and numbers)
        if (!/^[A-Z0-9]{6}$/i.test(code)) {
            throw new Error('Code must be exactly 6 characters (letters or numbers)');
        }
        
        // Convert to uppercase to ensure consistency
        const upperCode = code.toUpperCase();
        
        // Get pairing state
        const pairingState = devices.get(`pairing_${deviceId}`);
        if (!pairingState || !pairingState.remote) {
            throw new Error('No pairing in progress for this device. Start pairing first.');
        }
        
        const remote = pairingState.remote;
        
        console.log(`[${deviceId}] Sending code to TV...`);
        
        // Send pairing code (use uppercase version)
        await remote.sendCode(upperCode);
        
        console.log(`[${deviceId}] âœ" Code sent successfully`);
        
        // Wait for pairing to complete
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // Get certificate (contains both cert and key)
        const cert = remote.getCertificate();
        
        if (!cert) {
            throw new Error('Failed to get certificate from remote');
        }
        
        console.log(`[${deviceId}] âœ" Certificates obtained`);
        
        // Initialize device state with paired remote
        const deviceState = initializeDeviceState(deviceId, remote, pairingState.host);
        deviceState.connected = true;
        deviceState.powerState = 'on';
        
        // Start state polling
        startStatePolling(deviceId);
        
        // Clean up pairing state
        devices.delete(`pairing_${deviceId}`);
        
        console.log(`[${deviceId}] âœ" PAIRING SUCCESSFUL`);
        
        res.json({
            success: true,
            message: 'Pairing successful',
            deviceId: deviceId,
            certificate: cert.toString('base64'),
            privateKey: cert.toString('base64')  // Same as cert for this library
        });
        
    } catch (error) {
        console.error('âœ— PAIRING COMPLETE FAILED:', error.message);
        console.error('Error details:', error);
        res.status(500).json({
            success: false,
            error: error.message || 'Failed to complete pairing'
        });
    }
});

// ====================
// Connection Management
// ====================

// Connect to TV
app.post('/connect', async (req, res) => {
    console.log('\n=== CONNECT REQUEST ===');
    
    try {
        const deviceId = req.body.deviceId;
        const host = req.body.host;
        const certificate = req.body.certificate;
        const privateKey = req.body.privateKey;
        const deviceName = req.body.deviceName || 'Hubitat';
        
        if (!deviceId || !host || !certificate || !privateKey) {
            throw new Error('Missing required parameters');
        }
        
        console.log(`Device ID: ${deviceId}`);
        console.log(`Host: ${host}`);
        console.log(`Device Name: ${deviceName}`);
        
        // Check if already connected
        const existing = devices.get(deviceId);
        if (existing && existing.remote && existing.connected) {
            console.log(`[${deviceId}] Already connected, reusing existing connection`);
            return res.json({
                success: true,
                message: 'Already connected (reused existing)',
                deviceId: deviceId
            });
        }
        
        // Decode certificates
        const certBuffer = Buffer.from(certificate, 'base64');
        const keyBuffer = Buffer.from(privateKey, 'base64');
        
        // Create remote instance
        const remote = new AndroidRemote(host, {
            name: deviceName,  // Use device name from Hubitat
            cert: certBuffer,
            key: keyBuffer
        });
        
        console.log(`[${deviceId}] Starting connection...`);
        
        // Start connection
        await remote.start();
        
        // Initialize device state (this sets up event listeners)
        const deviceState = initializeDeviceState(deviceId, remote, host);
        deviceState.connected = true;
        
        // Start state polling
        startStatePolling(deviceId);
        
        console.log(`[${deviceId}] âœ" Connected successfully`);
        
        res.json({
            success: true,
            message: 'Connected successfully',
            deviceId: deviceId
        });
        
    } catch (error) {
        console.error('Connection error:', error);
        res.status(500).json({
            success: false,
            error: error.message || 'Failed to connect'
        });
    }
});

// Disconnect
app.post('/disconnect', async (req, res) => {
    try {
        const deviceId = req.body.deviceId;
        
        if (!deviceId) {
            throw new Error('Missing deviceId');
        }
        
        const deviceState = devices.get(deviceId);
        if (deviceState) {
            // Stop keepalive
            if (deviceState.keepaliveInterval) {
                clearInterval(deviceState.keepaliveInterval);
                deviceState.keepaliveInterval = null;
                console.log(`[${deviceId}] Keepalive stopped`);
            }
            
            // Stop state polling
            stopStatePolling(deviceId);
            
            // Stop remote
            if (deviceState.remote) {
                try {
                    await deviceState.remote.stop();
                } catch (e) {
                    console.error(`[${deviceId}] Error stopping remote:`, e);
                }
            }
            
            devices.delete(deviceId);
        }
        
        console.log(`[${deviceId}] Disconnected`);
        
        res.json({
            success: true,
            message: 'Disconnected'
        });
        
    } catch (error) {
        console.error('Disconnect error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Get status
app.get('/status/:deviceId', async (req, res) => {
    try {
        const deviceId = req.params.deviceId;
        
        const deviceState = devices.get(deviceId);
        if (!deviceState) {
            console.log(`[${deviceId}] Status query - device not found`);
            return res.json({
                success: true,
                connected: false,
                deviceId: deviceId
            });
        }
        
        // Update state before returning
        await updateDeviceState(deviceId);
        
        const response = {
            success: true,
            connected: deviceState.connected,
            deviceId: deviceId,
            state: {
                powerState: deviceState.powerState || 'unknown',
                volume: deviceState.volume || 0,
                muted: deviceState.muted || false,
                currentApp: deviceState.currentApp || 'unknown',
                lastActivity: deviceState.lastActivity
            }
        };
        
        console.log(`[${deviceId}] Status query response:`, JSON.stringify(response.state));
        
        res.json(response);
        
    } catch (error) {
        console.error('Status error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Get power state specifically
app.get('/power/:deviceId', async (req, res) => {
    try {
        const deviceId = req.params.deviceId;
        
        const deviceState = devices.get(deviceId);
        if (!deviceState) {
            return res.json({
                success: true,
                powerState: 'unknown',
                deviceId: deviceId
            });
        }
        
        console.log(`[${deviceId}] Querying power state...`);
        
        // Force power state detection
        await updateDeviceState(deviceId);
        
        const powerState = deviceState.powerState || 'unknown';
        
        console.log(`[${deviceId}] Power state: ${powerState}`);
        
        res.json({
            success: true,
            powerState: powerState,
            deviceId: deviceId
        });
        
    } catch (error) {
        console.error('Power query error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// ====================
// Command Endpoints
// ====================

// Send key
app.post('/key', async (req, res) => {
    try {
        const deviceId = req.body.deviceId;
        const keyCode = parseInt(req.body.keyCode);
        const keyName = req.body.keyName;
        
        if (!deviceId || isNaN(keyCode)) {
            throw new Error('Missing or invalid required parameters: deviceId, keyCode');
        }
        
        const deviceState = devices.get(deviceId);
        if (!deviceState || !deviceState.remote) {
            throw new Error(`Device ${deviceId} not connected`);
        }
        
        console.log(`[${deviceId}] Sending key: ${keyName || 'unknown'} (${keyCode})`);
        
        await deviceState.remote.sendKey(keyCode, RemoteDirection.SHORT);
        
        // Track state changes
        if (keyName === 'POWER' || keyName === 'WAKEUP') {
            trackPowerState(deviceId, 'on');
        } else if (keyName === 'SLEEP') {
            trackPowerState(deviceId, 'off');
        } else if (keyName === 'VOLUME_UP') {
            trackVolume(deviceId, Math.min(100, deviceState.volume + 1));
        } else if (keyName === 'VOLUME_DOWN') {
            trackVolume(deviceId, Math.max(0, deviceState.volume - 1));
        } else if (keyName === 'VOLUME_MUTE') {
            trackVolume(deviceId, null, !deviceState.muted);
        }
        
        deviceState.lastActivity = Date.now();
        
        console.log(`[${deviceId}] âœ" Key sent`);
        
        res.json({
            success: true,
            message: `Sent key: ${keyName}`,
            deviceId: deviceId
        });
        
    } catch (error) {
        console.error('Send key error:', error);
        res.status(500).json({
            success: false,
            error: error.message || 'Failed to send key'
        });
    }
});

// Launch app
app.post('/app/launch', async (req, res) => {
    try {
        const deviceId = req.body.deviceId;
        const appUrl = req.body.appUrl;
        
        if (!deviceId || !appUrl) {
            throw new Error('Missing required parameters');
        }
        
        const deviceState = devices.get(deviceId);
        if (!deviceState || !deviceState.remote) {
            throw new Error(`Device ${deviceId} not connected`);
        }
        
        console.log(`[${deviceId}] Launching app: ${appUrl}`);
        
        await deviceState.remote.sendAppLink(appUrl);
        
        // Track current app
        trackApp(deviceId, appUrl);
        
        console.log(`[${deviceId}] âœ" App launched`);
        
        res.json({
            success: true,
            message: 'App launched',
            deviceId: deviceId
        });
        
    } catch (error) {
        console.error('Launch app error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Send text
app.post('/text', async (req, res) => {
    try {
        const deviceId = req.body.deviceId;
        const text = req.body.text;
        
        if (!deviceId || !text) {
            throw new Error('Missing required parameters');
        }
        
        const deviceState = devices.get(deviceId);
        if (!deviceState || !deviceState.remote) {
            throw new Error(`Device ${deviceId} not connected`);
        }
        
        console.log(`[${deviceId}] Sending text: ${text}`);
        
        // Send text character by character
        for (const char of text) {
            await deviceState.remote.sendText(char);
        }
        
        deviceState.lastActivity = Date.now();
        
        console.log(`[${deviceId}] âœ" Text sent`);
        
        res.json({
            success: true,
            message: 'Text sent',
            deviceId: deviceId
        });
        
    } catch (error) {
        console.error('Send text error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// ====================
// Scenes/Presets
// ====================

// Save scene
app.post('/scene/save', async (req, res) => {
    try {
        const sceneName = req.body.sceneName;
        const scene = req.body.scene;
        
        if (!sceneName || !scene) {
            throw new Error('Missing required parameters: sceneName, scene');
        }
        
        scenes.set(sceneName, {
            name: sceneName,
            ...scene,
            createdAt: Date.now()
        });
        
        console.log(`Scene saved: ${sceneName}`);
        
        res.json({
            success: true,
            message: `Scene '${sceneName}' saved`,
            sceneName: sceneName
        });
        
    } catch (error) {
        console.error('Save scene error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Execute scene
app.post('/scene/execute', async (req, res) => {
    try {
        const sceneName = req.body.sceneName;
        const deviceId = req.body.deviceId;
        
        if (!sceneName || !deviceId) {
            throw new Error('Missing required parameters: sceneName, deviceId');
        }
        
        const scene = scenes.get(sceneName);
        if (!scene) {
            throw new Error(`Scene '${sceneName}' not found`);
        }
        
        const deviceState = devices.get(deviceId);
        if (!deviceState || !deviceState.remote) {
            throw new Error(`Device ${deviceId} not connected`);
        }
        
        console.log(`[${deviceId}] Executing scene: ${sceneName}`);
        
        // Execute scene actions
        if (scene.app) {
            await deviceState.remote.sendAppLink(scene.app);
            trackApp(deviceId, scene.app);
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
        
        if (scene.volume !== undefined) {
            // Set volume by calculating steps
            const currentVolume = deviceState.volume || 50;
            const steps = scene.volume - currentVolume;
            const keyCode = steps > 0 ? 24 : 25; // VOLUME_UP or VOLUME_DOWN
            
            for (let i = 0; i < Math.abs(steps); i++) {
                await deviceState.remote.sendKey(keyCode, RemoteDirection.SHORT);
                await new Promise(resolve => setTimeout(resolve, 100));
            }
            
            trackVolume(deviceId, scene.volume);
        }
        
        if (scene.muted !== undefined && scene.muted !== deviceState.muted) {
            await deviceState.remote.sendKey(164, RemoteDirection.SHORT); // VOLUME_MUTE
            trackVolume(deviceId, null, scene.muted);
        }
        
        if (scene.keys && Array.isArray(scene.keys)) {
            for (const keyCode of scene.keys) {
                await deviceState.remote.sendKey(parseInt(keyCode), RemoteDirection.SHORT);
                await new Promise(resolve => setTimeout(resolve, 200));
            }
        }
        
        console.log(`[${deviceId}] âœ" Scene executed: ${sceneName}`);
        
        res.json({
            success: true,
            message: `Scene '${sceneName}' executed`,
            deviceId: deviceId,
            sceneName: sceneName
        });
        
    } catch (error) {
        console.error('Execute scene error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// List scenes
app.get('/scenes', (req, res) => {
    const sceneList = Array.from(scenes.values());
    res.json({
        success: true,
        scenes: sceneList,
        count: sceneList.length
    });
});

// Delete scene
app.delete('/scene/:sceneName', (req, res) => {
    try {
        const sceneName = req.params.sceneName;
        
        if (scenes.has(sceneName)) {
            scenes.delete(sceneName);
            console.log(`Scene deleted: ${sceneName}`);
            res.json({
                success: true,
                message: `Scene '${sceneName}' deleted`
            });
        } else {
            res.status(404).json({
                success: false,
                error: `Scene '${sceneName}' not found`
            });
        }
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// ====================
// Multi-Room Sync
// ====================

// Create sync group
app.post('/sync/create', (req, res) => {
    try {
        const groupName = req.body.groupName;
        const deviceIds = req.body.deviceIds;
        
        if (!groupName || !Array.isArray(deviceIds) || deviceIds.length < 2) {
            throw new Error('groupName and at least 2 deviceIds required');
        }
        
        // Verify all devices exist and are connected
        for (const deviceId of deviceIds) {
            const deviceState = devices.get(deviceId);
            if (!deviceState || !deviceState.connected) {
                throw new Error(`Device ${deviceId} not found or not connected`);
            }
        }
        
        syncGroups.set(groupName, {
            name: groupName,
            devices: deviceIds,
            master: deviceIds[0],
            createdAt: Date.now()
        });
        
        console.log(`Sync group created: ${groupName} with ${deviceIds.length} devices`);
        
        res.json({
            success: true,
            message: `Sync group '${groupName}' created`,
            groupName: groupName,
            devices: deviceIds
        });
        
    } catch (error) {
        console.error('Create sync group error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Send command to sync group
app.post('/sync/command', async (req, res) => {
    try {
        const groupName = req.body.groupName;
        const command = req.body.command;
        
        if (!groupName || !command) {
            throw new Error('Missing required parameters: groupName, command');
        }
        
        const group = syncGroups.get(groupName);
        if (!group) {
            throw new Error(`Sync group '${groupName}' not found`);
        }
        
        console.log(`[${groupName}] Sending sync command to ${group.devices.length} devices`);
        
        const results = [];
        
        // Send command to all devices in parallel
        const promises = group.devices.map(async (deviceId) => {
            const deviceState = devices.get(deviceId);
            if (!deviceState || !deviceState.remote) {
                console.warn(`[${deviceId}] Not connected, skipping`);
                return { deviceId, success: false, error: 'Not connected' };
            }
            
            try {
                if (command.type === 'key') {
                    await deviceState.remote.sendKey(parseInt(command.keyCode), RemoteDirection.SHORT);
                } else if (command.type === 'app') {
                    await deviceState.remote.sendAppLink(command.appUrl);
                    trackApp(deviceId, command.appUrl);
                } else if (command.type === 'volume') {
                    const steps = command.volume - (deviceState.volume || 50);
                    const keyCode = steps > 0 ? 24 : 25;
                    for (let i = 0; i < Math.abs(steps); i++) {
                        await deviceState.remote.sendKey(keyCode, RemoteDirection.SHORT);
                        await new Promise(resolve => setTimeout(resolve, 50));
                    }
                    trackVolume(deviceId, command.volume);
                }
                
                console.log(`[${deviceId}] âœ" Sync command executed`);
                return { deviceId, success: true };
                
            } catch (error) {
                console.error(`[${deviceId}] Sync command error:`, error);
                return { deviceId, success: false, error: error.message };
            }
        });
        
        const commandResults = await Promise.all(promises);
        
        res.json({
            success: true,
            message: `Command sent to sync group '${groupName}'`,
            groupName: groupName,
            results: commandResults
        });
        
    } catch (error) {
        console.error('Sync command error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// List sync groups
app.get('/sync/groups', (req, res) => {
    const groupList = Array.from(syncGroups.values());
    res.json({
        success: true,
        groups: groupList,
        count: groupList.length
    });
});

// Delete sync group
app.delete('/sync/:groupName', (req, res) => {
    try {
        const groupName = req.params.groupName;
        
        if (syncGroups.has(groupName)) {
            syncGroups.delete(groupName);
            console.log(`Sync group deleted: ${groupName}`);
            res.json({
                success: true,
                message: `Sync group '${groupName}' deleted`
            });
        } else {
            res.status(404).json({
                success: false,
                error: `Sync group '${groupName}' not found`
            });
        }
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// ====================
// Utility Endpoints
// ====================

// Unpair device
app.post('/unpair', async (req, res) => {
    console.log('\n=== UNPAIR REQUEST ===');
    
    try {
        const deviceId = req.body.deviceId;
        
        if (!deviceId) {
            throw new Error('Missing required parameter: deviceId');
        }
        
        console.log(`Device ID: ${deviceId}`);
        
        // Stop state polling
        stopStatePolling(deviceId);
        
        // Remove from active devices
        devices.delete(deviceId);
        
        // Also remove any pairing in progress
        devices.delete(`pairing_${deviceId}`);
        
        console.log(`[${deviceId}] âœ" Unpaired and removed from bridge`);
        console.log(`[${deviceId}] Note: Also clear pairing on TV to fully reset`);
        
        res.json({
            success: true,
            message: 'Unpaired from bridge. Also clear Android TV Remote Service data on TV.',
            deviceId: deviceId
        });
        
    } catch (error) {
        console.error('Unpair error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// List devices
app.get('/devices', (req, res) => {
    const deviceList = Array.from(devices.entries())
        .filter(([id]) => !id.startsWith('pairing_'))
        .map(([deviceId, state]) => ({
            deviceId: deviceId,
            host: state.host || 'unknown',
            connected: state.connected || false,
            powerState: state.powerState || 'unknown',
            volume: state.volume || 0,
            muted: state.muted || false,
            currentApp: state.currentApp || 'unknown',
            lastActivity: state.lastActivity || null
        }));
    
    res.json({
        devices: deviceList,
        count: deviceList.length
    });
});

// Health check
app.get('/health', (req, res) => {
    const connectedCount = Array.from(devices.values())
        .filter(d => d.connected && !d.deviceId?.startsWith('pairing_'))
        .length;
    
    res.json({
        status: 'ok',
        connectedDevices: connectedCount,
        totalDevices: devices.size,
        scenes: scenes.size,
        syncGroups: syncGroups.size,
        uptime: process.uptime()
    });
});

// Test endpoint
app.get('/test', (req, res) => {
    res.json({
        status: 'Bridge server is running',
        endpoints: {
            pairing: ['/pair/start', '/pair/complete'],
            connection: ['/connect', '/disconnect', '/status/:deviceId'],
            commands: ['/key', '/app/launch', '/text'],
            scenes: ['/scene/save', '/scene/execute', '/scenes'],
            sync: ['/sync/create', '/sync/command', '/sync/groups'],
            utility: ['/unpair', '/devices', '/health']
        }
    });
});

// Start server
app.listen(PORT, '0.0.0.0', () => {
    console.log('='.repeat(70));
    console.log('Android TV Remote Bridge Server v2.0');
    console.log('='.repeat(70));
    console.log(`Server running on port ${PORT}`);
    console.log('');
    console.log('Features:');
    console.log('  âœ" Android TV Remote Protocol v2');
    console.log('  âœ" TV State Feedback (power, volume, app)');
    console.log('  âœ" Scenes/Presets Support');
    console.log('  âœ" Multi-Room Audio Sync');
    console.log('');
    console.log('Endpoints:');
    console.log(`  POST http://localhost:${PORT}/pair/start`);
    console.log(`  POST http://localhost:${PORT}/pair/complete`);
    console.log(`  POST http://localhost:${PORT}/connect`);
    console.log(`  GET  http://localhost:${PORT}/status/:deviceId`);
    console.log(`  POST http://localhost:${PORT}/key`);
    console.log(`  POST http://localhost:${PORT}/scene/save`);
    console.log(`  POST http://localhost:${PORT}/scene/execute`);
    console.log(`  POST http://localhost:${PORT}/sync/create`);
    console.log(`  POST http://localhost:${PORT}/sync/command`);
    console.log(`  GET  http://localhost:${PORT}/health`);
    console.log('');
    console.log('Configure Hubitat driver to use this bridge:');
    console.log(`  Bridge URL: http://YOUR_SERVER_IP:${PORT}`);
    console.log('='.repeat(70));
});

// Graceful shutdown
process.on('SIGINT', async () => {
    console.log('\n\nShutting down...');
    
    // Stop all state polling
    for (const [deviceId] of devices.entries()) {
        if (!deviceId.startsWith('pairing_')) {
            stopStatePolling(deviceId);
        }
    }
    
    // Disconnect all devices
    for (const [deviceId, state] of devices.entries()) {
        if (!deviceId.startsWith('pairing_') && state.remote) {
            try {
                console.log(`Disconnecting ${deviceId}`);
                await state.remote.stop();
            } catch (error) {
                console.error(`Error disconnecting ${deviceId}:`, error);
            }
        }
    }
    
    process.exit(0);
});
