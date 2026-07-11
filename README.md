# Work Timer

An Android app that records work hours through manual check-in/check-out or an optional workplace geofence.

## Implemented rules

- Manual check-in and check-out can be used as many times as needed.
- With automatic location enabled, entering the workplace starts the timer and leaving pauses it.
- A manual check-out while inside the workplace overrides the geofence. Automatic tracking resumes only after the user exits and enters again.
- Automatic location is optional and disabled by default.
- Sessions that cross midnight are split. The new day starts at zero and continues counting if the session remains active.
- The daily goal is configurable from 15 minutes to 24 hours.
- Android sends one sound and vibration alert when the daily goal is reached.
- Geofences and alarms are restored after a reboot or app update.

## Technology

- Web interface packaged with Capacitor 8.
- Native Android integration in Java.
- Google Play Services Location for geofencing.
- `AlarmManager`, `BroadcastReceiver`, and Android notifications for daily goals and midnight rollover.
- Local data stored in `SharedPreferences` and separated by the device's local date.

## Set up the project

Requirements: Node.js, Android Studio, Android SDK, and a JDK compatible with Capacitor 8.

```bash
npm install
npm run test:web
npm run android:sync
npm run android:open
```

The `android/` directory is already versioned. Do not run `npm run android:add` when it already exists.

## Configure permissions on a device

1. Open **Settings** in the app.
2. Set the daily goal.
3. Enable automatic location only when workplace-based tracking is wanted.
4. Select precise location when Android asks for location permission.
5. Tap **Configure permissions** again and select **Allow all the time** in Android settings.
6. Tap **Use current location as workplace** or enter the coordinates and radius manually.
7. Save the settings.

Android can delay geofence transitions to conserve battery. An initial radius between 100 and 200 meters is recommended.

## Build a debug APK

In Android Studio, use **Build > Build APK(s)**. From the command line:

```bash
cd android
./gradlew assembleDebug
```

The APK is created in `android/app/build/outputs/apk/debug/`.

## Google Play release

Before publishing, replace the default icon and splash screen, configure release signing, build a signed Android App Bundle, add an in-app background-location disclosure and privacy-policy link, and complete Google Play's background-location declaration.
