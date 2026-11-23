# SMILE — WebRTC & STUN/TURN Server Setup Guide

## 1. Current Calling Architecture in SMILE
- **Signaling Plane (Live in Firestore):** `/calls/{callId}` exchanges call state (`RINGING`, `ACTIVE`, `ENDED`, `MISSED`, `DECLINED`), SDP offers/answers, participant lists (`participantUids`), call type (`VOICE` / `VIDEO`), group flag (`groupCall`), and duration.
- **On-Device Media & Hardware Controls (Live in `CallScreen.kt`):**
  - `CameraX` (`ProcessCameraProvider` + `PreviewView`) renders live front/rear camera preview with flip & toggle controls.
  - `AudioManager` switches audio routing to `MODE_IN_COMMUNICATION`, speakerphone toggle, and hardware microphone mute.

## 2. Production Media Relay Setup (`coturn` STUN/TURN)
Direct peer-to-peer WebRTC media often fails behind carrier-grade NAT (common on mobile networks in India) without a TURN relay server. Deploy `coturn` on a cloud VM (`asia-south1` Mumbai recommended):

```bash
sudo apt-get update && sudo apt-get install -y coturn
```

Configure `/etc/turnserver.conf`:
```ini
listening-port=3478
tls-listening-port=5349
fingerprint
lt-cred-mech
use-auth-secret
static-auth-secret=<YOUR_TURN_HMAC_SECRET>
realm=turn.smile-app.example.com
cert=/etc/letsencrypt/live/turn.smile-app.example.com/fullchain.pem
pkey=/etc/letsencrypt/live/turn.smile-app.example.com/privkey.pem
no-tlsv1
no-tlsv1_1
```

## 3. Ephemeral TURN Credentials & WebRTC PeerConnection Integration
- Issue short-lived (`ttl = 3600s`) HMAC-SHA1 TURN credentials from a Cloud Function so static TURN secrets never ship in the APK.
- Add `io.getstream:stream-webrtc-android` (or official WebRTC AAR) to `app/build.gradle.kts` and pass ICE candidate exchange subcollections under `calls/{callId}/candidates/{candidateId}`.
- For incoming full-screen call ring alerts while the app is killed, pair `notifyIncomingCall` in `/backend/functions/src/index.ts` with Android `ConnectionService` / `CallStyle` notifications (`USE_FULL_SCREEN_INTENT` subject to Play Console core calling review).
