# Phone Break Guardian

Personal-use Android digital-wellbeing app.

## Features
- Counts foreground phone use across apps using Android Usage Access.
- Break prompt after 30 minutes.
- Addition/multiplication challenge before dismissing the break screen.
- Night warning (22:00–06:00 by default) when the ambient-light sensor reports a dark environment.
- English and Tamil warnings/resources.
- Foreground monitoring service.
- Restarts after device boot.

## Build
1. Install Android Studio (current stable) on a computer.
2. Open this folder as an existing project.
3. Let Android Studio download the Android Gradle Plugin/Kotlin/AndroidX dependencies.
4. Build > Build APK(s).
5. Install the resulting debug APK on the Android phone.

## First-run permissions
The app requires:
- Usage Access (Settings > Special app access > Usage access)
- Display over other apps
- Notification permission on Android 13+
- Optional battery-optimization exemption for more reliable background monitoring

## Important Android limitation
A normal sideloaded app cannot obtain unrestricted control of the device. Usage Access reports app usage; the overlay permission allows the break screen to appear over other apps. Some Android versions/OEMs may restrict background launches or overlays. Strong device-wide enforcement requires device-owner/managed-device provisioning and is intentionally not enabled by default.

## Privacy
This project does not request internet, contacts, SMS, microphone, camera, or location permissions. Usage statistics and sensor readings are processed locally by the app.

## Notes
The 30-minute counter uses periodic foreground-app sampling and is intentionally conservative. OEM battery management can affect monitoring reliability.
