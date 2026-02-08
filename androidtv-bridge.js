/**
 * Android TV Remote - Bridge Server
 * 
 * This Node.js server acts as a bridge between Hubitat and Android TV
 * It handles the complex Protocol Buffer and TLS requirements that
 * Hubitat's Groovy environment cannot natively support.
 * 
 * Install dependencies:
 * npm install express body-parser androidtvremote2
 * 
 * Run:
 * node androidtv-bridge.js
 * 
 * Then configure Hubitat driver to point to this bridge's IP:port
 */

const express = require('express');
const bodyParser = require('body-parser');
const { AndroidRemote } = require('androidtvremote2');

const app = express();
app.use(bodyParser.json());

// Configuration
const PORT = 3000;
const devices = new Map(); // Store TV connections

// Helper function to get or create TV connection
async function getDevice(deviceId, host, certFile, keyFile) {
    if (!devices.has(deviceId)) {
        console.log(`Creating new connection for device: ${deviceId}`);
        const remote = new AndroidRemote(host, {
            certFile: certFile,
            keyFile: keyFile
        });
        devices.set(deviceId, remote);
    }
    return devices.get(deviceId);
}

// Pairing endpoint
app.post('/pair/start', async (req, res) => {
    try {
        const { deviceId, host, deviceName } = req.body;
        
        console.log(`Starting pairing for ${deviceId} at ${host}`);
        
        const remote = new AndroidRemote(host, {
            name: deviceName || 'Hubitat'
        });
        
        await remote.start();
        const code = await remote.getPairingCode();
        
        // Store temporarily for completion
        devices.set(`pairing_${deviceId}`, remote);
        
        res.json({
            success: true,
            message: 'Pairing started - enter code on TV',
            code: code
        });
        
    } catch (error) {
        console.error('Pairing start error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

app.post('/pair/complete', async (req, res) => {
    try {
        const { deviceId, code } = req.body;
        
        const remote = devices.get(`pairing_${deviceId}`);
        if (!remote) {
            throw new Error('No pairing in progress');
        }
        
        await remote.sendPairingCode(code);
        
        // Get certificates for storage
        const cert = remote.getCertificate();
        const key = remote.getPrivateKey();
        
        // Clean up temporary pairing connection
        devices.delete(`pairing_${deviceId}`);
        
        res.json({
            success: true,
            message: 'Pairing successful',
            certificate: cert.toString('base64'),
            privateKey: key.toString('base64')
        });
        
    } catch (error) {
        console.error('Pairing complete error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Connection management
app.post('/connect', async (req, res) => {
    try {
        const { deviceId, host, certificate, privateKey } = req.body;
        
        console.log(`Connecting to ${deviceId} at ${host}`);
        
        const certBuffer = Buffer.from(certificate, 'base64');
        const keyBuffer = Buffer.from(privateKey, 'base64');
        
        const remote = new AndroidRemote(host, {
            cert: certBuffer,
            key: keyBuffer
        });
        
        await remote.start();
        devices.set(deviceId, remote);
        
        res.json({
            success: true,
            message: 'Connected successfully'
        });
        
    } catch (error) {
        console.error('Connection error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

app.post('/disconnect', async (req, res) => {
    try {
        const { deviceId } = req.body;
        
        const remote = devices.get(deviceId);
        if (remote) {
            await remote.stop();
            devices.delete(deviceId);
        }
        
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

// Send key command
app.post('/key', async (req, res) => {
    try {
        const { deviceId, keyCode, keyName } = req.body;
        
        const remote = devices.get(deviceId);
        if (!remote) {
            throw new Error('Device not connected');
        }
        
        console.log(`Sending key to ${deviceId}: ${keyName} (${keyCode})`);
        
        await remote.sendKey(keyCode);
        
        res.json({
            success: true,
            message: `Sent key: ${keyName}`
        });
        
    } catch (error) {
        console.error('Send key error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Launch app
app.post('/app/launch', async (req, res) => {
    try {
        const { deviceId, appUrl } = req.body;
        
        const remote = devices.get(deviceId);
        if (!remote) {
            throw new Error('Device not connected');
        }
        
        console.log(`Launching app on ${deviceId}: ${appUrl}`);
        
        await remote.sendAppLink(appUrl);
        
        res.json({
            success: true,
            message: 'App launched'
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
        const { deviceId, text } = req.body;
        
        const remote = devices.get(deviceId);
        if (!remote) {
            throw new Error('Device not connected');
        }
        
        console.log(`Sending text to ${deviceId}: ${text}`);
        
        // Send text character by character
        for (const char of text) {
            await remote.sendText(char);
        }
        
        res.json({
            success: true,
            message: 'Text sent'
        });
        
    } catch (error) {
        console.error('Send text error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Get status
app.get('/status/:deviceId', async (req, res) => {
    try {
        const { deviceId } = req.params;
        
        const remote = devices.get(deviceId);
        const connected = remote && remote.isConnected();
        
        res.json({
            success: true,
            connected: connected,
            deviceId: deviceId
        });
        
    } catch (error) {
        console.error('Status error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        connectedDevices: devices.size,
        uptime: process.uptime()
    });
});

// Start server
app.listen(PORT, () => {
    console.log('='.repeat(60));
    console.log('Android TV Remote Bridge Server');
    console.log('='.repeat(60));
    console.log(`Server running on port ${PORT}`);
    console.log('');
    console.log('Endpoints:');
    console.log(`  POST http://localhost:${PORT}/pair/start`);
    console.log(`  POST http://localhost:${PORT}/pair/complete`);
    console.log(`  POST http://localhost:${PORT}/connect`);
    console.log(`  POST http://localhost:${PORT}/disconnect`);
    console.log(`  POST http://localhost:${PORT}/key`);
    console.log(`  POST http://localhost:${PORT}/app/launch`);
    console.log(`  POST http://localhost:${PORT}/text`);
    console.log(`  GET  http://localhost:${PORT}/status/:deviceId`);
    console.log(`  GET  http://localhost:${PORT}/health`);
    console.log('');
    console.log('Configure Hubitat driver to use this bridge:');
    console.log(`  Bridge URL: http://YOUR_SERVER_IP:${PORT}`);
    console.log('='.repeat(60));
});

// Graceful shutdown
process.on('SIGINT', async () => {
    console.log('\nShutting down...');
    
    // Disconnect all devices
    for (const [deviceId, remote] of devices.entries()) {
        try {
            console.log(`Disconnecting ${deviceId}`);
            await remote.stop();
        } catch (error) {
            console.error(`Error disconnecting ${deviceId}:`, error);
        }
    }
    
    process.exit(0);
});
