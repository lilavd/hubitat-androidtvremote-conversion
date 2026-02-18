# Input Switching Limitations - Philips Android TV

## The Bad News

**Philips Android TVs do NOT respond to standard Android TV input switching key codes.**

This is a **hardware/firmware limitation**, not a driver issue.

## What Doesn't Work

### Key Codes That Fail on Philips

| Key Code | Function | Status |
|----------|----------|--------|
| 178 | Input Source Menu | ❌ Doesn't work |
| 242 | TV Antenna/Cable | ❌ Doesn't work |
| 243 | HDMI 1 | ❌ Doesn't work |
| 244 | HDMI 2 | ❌ Doesn't work |
| 245 | HDMI 3 | ❌ Doesn't work |
| 246 | HDMI 4 | ❌ Doesn't work |
| 247 | Composite 1 | ❌ Doesn't work |
| 248 | Component 1 | ❌ Doesn't work |
| 249 | Component 2 | ❌ Doesn't work |
| 250 | VGA | ❌ Doesn't work |

**Source:** Home Assistant Community thread confirms this for Philips 70PUS9005/12 (2020 model) and similar Philips Android TVs.

### Why They Don't Work

1. **Philips uses custom firmware** that doesn't implement standard Android TV input key codes
2. **Older Philips TVs** had JointSpace API for input control (newer models removed it)
3. **Newer Philips Android TVs** don't support JointSpace OR standard key codes
4. **Google Assistant DOES work** - but that's a different API

## What DOES Work on Philips

✅ **All other remote commands:**
- Navigation (dpad, back, home, menu)
- Media control (play, pause, stop, FF, RW)
- Volume (up, down, mute)
- Power (on, off, wake)
- Channels (up, down, last)
- Number keys (0-9)
- Apps (launch, track current)
- Audio controls (audioTrack, captions)
- Color buttons (red, green, yellow, blue)

❌ **Only input switching fails**

## Workarounds

### Option 1: HDMI-CEC (Best Solution)

**Enable EasyLink on Philips TV:**
1. Settings → General Settings → EasyLink → On
2. Enable all EasyLink options

**How it works:**
```groovy
// When device powers on, TV auto-switches
appleTV.on()        // TV switches to Apple TV's HDMI
roku.on()           // TV switches to Roku's HDMI
cableBox.on()       // TV switches to cable box HDMI
```

**No commands to TV needed!** Devices control TV automatically.

### Option 2: Google Assistant Voice Commands

**If you have Google Home or Assistant integration:**

```groovy
// Via Google Home device
googleHome.speak("Switch to HDMI 1")
googleHome.speak("Switch to HDMI 2")
googleHome.speak("Switch to Cable")
```

**This DOES work** because it uses Google's API, not Android TV key codes.

### Option 3: Menu Navigation

Create automation sequences to navigate menus:

```groovy
// Example: Navigate to input menu
def switchToHDMI2() {
    tv.inputSource()      // Open inputs menu (may or may not work)
    pauseExecution(1000)
    tv.dpadRight()        // Navigate
    tv.dpadDown()
    tv.dpadCenter()       // Select HDMI 2
}
```

**Problem:** Menu layouts vary and change, making this unreliable.

### Option 4: Use Built-in Apps

Instead of switching to external devices, use TV's apps:

```groovy
// Instead of switching to external Apple TV
tv.launchApp("https://tv.apple.com/")  // Apple TV+ app

// Instead of switching to Roku/Fire Stick for Netflix
tv.launchApp("netflix://")             // Netflix app on TV
```

### Option 5: Physical Remote

Some operations can only be done with the actual Philips remote:
- Manual input switching when needed
- Initial setup of HDMI-CEC
- Configuring TV settings

## Confirmed Working TVs

**Standard Android TV key codes (243-246) work on:**
- Sony Bravia Android TVs ✅
- Nvidia Shield TV ✅
- Mi Box ✅
- Other generic Android TV devices ✅

**Don't work on:**
- Philips Android TVs (2016+) ❌
- Some TCL Android TVs ❌
- Custom firmware Android TVs ❌

## Why We Still Include Input Commands

**The driver includes input switching commands even though they don't work on Philips because:**

1. **They work on other Android TVs** (Sony, Nvidia Shield, Mi Box, etc.)
2. **Users might have multiple TV brands**
3. **Documentation shows what's possible** on standard Android TVs
4. **Future firmware updates** might add support
5. **No harm in trying** - commands just do nothing on Philips

## Testing on Your TV

**Quick test to see if your TV supports input switching:**

```groovy
tv.inputHdmi1()
// Wait 5 seconds
// Did TV switch to HDMI 1?
```

**If it worked:** ✅ Your TV supports standard Android TV key codes!

**If nothing happened:** ❌ Your TV (probably Philips) doesn't support it.

## Detection

**Unfortunately, there's no way to detect if a TV supports input switching without trying it.**

The Android TV Remote Protocol doesn't expose:
- Available inputs
- Current input
- Input switching capabilities

## Related Issues

### "Last Channel" Also Doesn't Work

**Same reason:** Key code 229 (LAST_CHANNEL) also doesn't work on Philips.

**Workaround:** Track channels manually in automation rules.

### Other TV-Specific Features

Some manufacturers add custom features that aren't accessible via standard Android TV protocol:
- Philips Ambilight (needs JointSpace API, not available on newer models)
- Sony Picture Modes (not accessible via Remote Protocol)
- TCL Roku Features (Roku TV != Android TV)

## Summary

### Input Switching on Philips Android TV

| Method | Works? | Notes |
|--------|--------|-------|
| Remote key codes | ❌ No | Philips doesn't implement them |
| HDMI-CEC (EasyLink) | ✅ Yes | Best solution, automatic |
| Google Assistant | ✅ Yes | Requires Google Home integration |
| Menu navigation | 🟡 Maybe | Unreliable, menus change |
| JointSpace API | ❌ No | Removed on newer models |
| Physical remote | ✅ Yes | Always works |

### Everything Else

| Feature | Works? |
|---------|--------|
| Power control | ✅ Yes |
| Volume control | ✅ Yes |
| Navigation | ✅ Yes |
| Media control | ✅ Yes |
| App launching | ✅ Yes |
| State tracking | ✅ Yes |
| All other commands | ✅ Yes |

## Recommendation

**For Philips Android TV users:**

1. ✅ **Enable HDMI-CEC (EasyLink)** - This solves 90% of input switching needs
2. ✅ **Use built-in apps** when possible instead of external devices
3. ✅ **Use Google Assistant** for manual input switching
4. ✅ **Accept that some features require physical remote**

**The driver works great for everything except input switching on Philips TVs!**

## References

- Home Assistant Community Thread: "Android TV HDMI" (October 2020)
- GitHub Issue: python-androidtv #6 "Philips Android TV HDMI support"
- Multiple user reports confirming issue across Philips models

**This is a known Philips limitation, not a bug in this driver.** 📺
