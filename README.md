# ESP32 Secure Lock

A native Android application written in **Kotlin** with **Jetpack Compose**
that protects itself with an **application-level passcode**. The passcode
never leaves the device, is never stored as plain text, and is never
displayed in the UI or logs.

> ⚠️ This application does **NOT** modify, replace, bypass, or control
> the Android system lock screen. It only protects access to *this* app.
> It does not change the device PIN, does not remove the device PIN,
> does not unlock the phone, and does not interact with the system
> Keyguard in any way.

A future version (v2) will add **ESP32 Bluetooth authentication** as a
second factor. The v1.0.1 codebase is structured so that feature can
be added without rewriting the app. **In v1.0.1 the ESP32 path is
explicitly reported as not configured** — it is not a stub that fakes
a connection.

---

## Table of contents

1. Version
2. Features (v1.0.1)
3. Technology stack
4. Architecture
5. Security approach
6. Passcode rules
7. App lock / background behaviour
8. ESP32 status (v1)
9. How to build
10. How to run tests
11. How to install the APK
12. Known limitations
13. Future ESP32 BLE integration (planned, NOT in v1)

---

## 1. Version

`versionName = 1.0.2`, `versionCode = 3`.

## 2. Features (v1.0.1)

- **First-launch setup** with a numeric passcode and confirmation.
- **App lock screen** on every cold start once a passcode is set.
- **Automatic re-lock** when the app is brought back from a fully
  stopped state (see §7).
- **Dashboard** with current protection status, change / remove
  passcode, ESP32 placeholder, and a manual lock button.
- **Change passcode** with re-authentication of the current passcode.
- **Remove passcode** with confirmation dialog and re-authentication.
- **Secure storage** of the passcode using
  `EncryptedSharedPreferences` + PBKDF2-HMAC-SHA256 with a random
  16-byte salt and 120 000 iterations. **No insecure fallback to
  plaintext persistent storage.**
- **Material 3** Compose UI with masked PIN inputs, dedicated error
  messages per failure mode, loading indicators, and basic
  accessibility (`testTag` semantics).
- **Bluetooth abstraction** ready for the upcoming ESP32 integration.

## 3. Technology stack

| Layer        | Choice                                            |
|--------------|---------------------------------------------------|
| Language     | Kotlin 1.9.22                                     |
| Build        | Gradle 8.5, Android Gradle Plugin 8.2.2           |
| UI           | Jetpack Compose (BOM 2024.02.00), Material 3      |
| Min / Target | minSdk 24, targetSdk / compileSdk 34              |
| Concurrency  | Kotlin Coroutines                                 |
| Security     | `androidx.security:security-crypto` 1.1.0-α06     |
|              | PBKDF2-HMAC-SHA256, 16-byte salt, 120 000 iters   |
| Tests        | JUnit 4, `kotlinx-coroutines-test`                |
| No external  | No Firebase, no analytics, no cloud backend       |

## 4. Architecture

```
ESP32SecureLock/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew, gradlew.bat
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/student/esp32securelock/
        │   │   ├── ESP32SecureLockApp.kt          (Application + AppContainer)
        │   │   ├── MainActivity.kt                (Activity + ProcessLifecycleOwner re-lock)
        │   │   ├── bluetooth/
        │   │   │   ├── BluetoothServiceProvider.kt
        │   │   │   └── Esp32Authenticator.kt      (interface + ConnectionResult)
        │   │   ├── data/
        │   │   │   ├── AppAuthState.kt            (NeedsSetup / Locked / Unlocked)
        │   │   │   └── AppContainer.kt            (lightweight service locator)
        │   │   ├── navigation/
        │   │   │   └── Destination.kt
        │   │   ├── security/
        │   │   │   ├── PasscodeHasher.kt          (PBKDF2-HMAC-SHA256)
        │   │   │   ├── PasscodeRepository.kt      (interface + Change/Remove result types)
        │   │   │   ├── PasscodeStore.kt           (EncryptedSharedPreferences impl)
        │   │   │   ├── PasscodeValidator.kt       (pure rules)
        │   │   │   └── InMemoryPreferences.kt     (legacy; no prod callers)
        │   │   └── ui/
        │   │       ├── AppViewModel.kt
        │   │       ├── AppViewModelFactory.kt
        │   │       ├── components/
        │   │       │   ├── CommonUi.kt
        │   │       │   └── ErrorMessages.kt
        │   │       ├── screens/
        │   │       │   ├── LockScreen.kt
        │   │       │   ├── MainScreen.kt
        │   │       │   └── SetupScreen.kt
        │   │       └── theme/Theme.kt
        │   └── res/
        │       ├── drawable/, mipmap-*, values/, values-night/, xml/
        └── test/java/com/student/esp32securelock/
            ├── bluetooth/NotConfiguredEsp32AuthenticatorTest.kt
            ├── security/{PasscodeHasher, PasscodeStore,
            │              PasscodeValidator}Test.kt
            └── ui/{AppViewModel, AppViewModelStorage,
                    ErrorMessageMapping}Test.kt
```

State flow: `AppAuthState { NeedsSetup, Locked, Unlocked }` is a
`StateFlow` driven by the repository + UI actions. `MainActivity`
chooses the Compose destination from that state.

## 5. Security approach

1. **No plaintext anywhere.** The passcode is fed straight from the
   Compose `OutlinedTextField` to `PasscodeHasher.hash(...)` which:
   - generates a fresh 16-byte random salt with `SecureRandom`,
   - runs **PBKDF2-HMAC-SHA256** for 120 000 iterations to derive a
     256-bit key,
   - returns `(salt, hash, iterations)` as Base64 strings.
2. **Persistent storage is encrypted.** Only `(salt, hash, iterations)`
   are stored, via `EncryptedSharedPreferences`
   (AES-256-GCM values, AES-256-SIV keys).
3. **No insecure fallback.** If `EncryptedSharedPreferences` cannot be
   initialised (corrupt keystore, misconfigured emulator, locked
   device) the store throws `SecureStorageUnavailableException`. The
   ViewModel surfaces this as `ErrorKind.STORAGE` and the UI shows
   "Could not access secure storage. The passcode was NOT saved to
   disk. Please restart the app." The passcode is never written to
   ordinary `SharedPreferences`.
4. **Verification is constant-time** — the candidate hash is compared
   to the stored hash byte-by-byte without short-circuiting.
5. **The plaintext `char[]` used for PBKDF2 is wiped** after derivation.
6. **The passcode is never logged**, never included in exception
   messages, and never persisted.
7. **No default passcode, no hard-coded secrets, no analytics, no
   network calls.**
8. **Failure modes for `change` and `remove` are typed**, not
   booleans. The repository returns `ChangePasscodeResult` /
   `RemovePasscodeResult` (Success, IncorrectCurrent, StorageFailure,
   Unexpected) so the UI can show the *real* problem instead of
   collapsing "wrong passcode" and "secure storage failed" into the
   same "Incorrect passcode." message.

## 6. Passcode rules

- 4 to 32 digits
- numeric characters only (`0-9`)
- enforced consistently by the validator **and** the input field

| Input        | Result                          |
|--------------|---------------------------------|
| `""`         | `EMPTY` — "Passcode cannot be empty." |
| `"12"`       | `TOO_SHORT` — "Passcode must be at least 4 digits." |
| `"1234"`     | `Ok` |
| `"1" * 32`   | `Ok` |
| `"1" * 33`   | `TOO_LONG` — "Passcode must not exceed 32 digits." |
| `"12ab34"`   | `NOT_NUMERIC` — "Passcode must contain only digits." |
| `"12#56"`    | `NOT_NUMERIC` |
| mismatched   | `MISMATCH` — "Passcodes do not match." |
| wrong one    | `INCORRECT` — "Incorrect passcode." |
| storage fail | `STORAGE` — "Could not access secure storage…" |
| unexpected   | `UNEXPECTED` — "An unexpected error occurred. Please try again. If the problem persists, restart the app." |

## 7. App lock / background behaviour

- **Manual lock**: the *Lock App* button on the main screen immediately
  returns to the lock screen.
- **Cold start**: when the app process is killed and relaunched, the
  lock screen is shown.
- **Background → foreground**: when the **process** is sent to the
  background (user pressed Home, switched apps, locked the device, or
  no activity is visible) and then becomes visible again, the app
  re-locks **only if a passcode is currently set** **and the user is
  currently Unlocked**. This is implemented with
  `ProcessLifecycleOwner` so:
  - configuration changes (rotation, dark-mode toggle, font scale,
    locale change) do **not** trigger a re-lock,
  - transient activity recreation does **not** trigger a re-lock,
  - a brief system dialog (e.g. permission prompt) that only pauses
    the foreground activity does **not** trigger a re-lock,
  - there is no recomposition / state-flow loop.
  The observer is added in `MainActivity.onCreate` and removed in
  `onDestroy`, so it never leaks across configuration changes.
- **First-launch setup** is not interrupted by the background-lock
  logic — `onProcessBackgrounded()` is a no-op when no passcode is
  set.

## 8. ESP32 status (v1)

The dashboard shows:

> **ESP32 Device**
> ESP32 connection: Not configured
> Bluetooth authentication via ESP32 will be added in a future version.
> Bluetooth status: Not available in this version

The "Test connection" button always resolves to `Not configured`. The
app **does not** fake a connection and **does not** report a
successful ESP32 link. The seam for v2 is the
`Esp32Authenticator` interface in `bluetooth/`.

## 9. How to build

Requirements: **JDK 17** and the **Android SDK with platform 34**
installed. `local.properties` must point `sdk.dir` at your SDK.

```bash
cd ESP32SecureLock
# 1) One-time: generate the Gradle wrapper jar (binary; not committed).
gradle wrapper --gradle-version 8.5

# 2) Build a debug APK.
./gradlew :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

If `./gradlew` complains that the wrapper jar is missing, run step 1
(it generates `gradle/wrapper/gradle-wrapper.jar`).

## 10. How to run tests

```bash
./gradlew :app:testDebugUnitTest
```

Report: `app/build/reports/tests/testDebugUnitTest/index.html`.

Test coverage:

- `PasscodeValidatorTest` — 3 / 4 / 6 / 32 / 33 digits, letters,
  symbols, whitespace, empty, confirmation match / mismatch.
- `PasscodeHasherTest` — salt randomness, hash round-trip, wrong-PIN
  rejection, empty rejection, 32-digit round-trip.
- `PasscodeStoreTest` — no passcode initially, save, verify, replace,
  remove, wrong-current rejection (via in-memory `FakeRepository`).
- `AppViewModelTest` — initial state (NeedsSetup / Locked), setup
  flow, mismatch, too-short, unlock success, unlock failure, change,
  remove, manual lock, error clearing, ESP32 placeholder.
- `AppViewModelStorageTest` — `STORAGE` error when persistence
  fails.
- `AppViewModelPasscodeMutationTest` — `changePasscode` and
  `removePasscode` map every repository failure to a distinct
  `ErrorKind` (INCORRECT, STORAGE, UNEXPECTED); verified that
  storage failures are not misreported as "Incorrect passcode."
- `AppViewModelLifecycleLockTest` — `onProcessBackgrounded` re-locks
  an Unlocked app, is a no-op when no passcode is set or when the
  app is already Locked, and clears transient error/loading state.
- `ErrorMessageMappingTest` — each `ErrorKind` (including the new
  `UNEXPECTED`) resolves to a distinct, correct message.
- `NotConfiguredEsp32AuthenticatorTest` — `statusText` and
  `connect()` behaviour.

## 11. How to install the APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 12. Known limitations

- **No ESP32 BLE yet.** The `Esp32Authenticator` interface exists but
  has only a `NotConfiguredEsp32Authenticator` implementation. The
  dashboard explicitly says so.
- **No rate limiting / lockout.** A future version may add a small
  attempt counter.
- **App-level only.** A determined attacker with root/physical access
  to the device can still read the encrypted file. The app does not
  attempt to lock the device itself.
- **We rely on `androidx.security:security-crypto`.** On a device
  with a broken Android Keystore the app refuses to store the
  passcode and asks the user to restart — this is the safe behaviour.

## 13. Future ESP32 BLE integration (planned, NOT in v1)

```
Android App
     ↓
Esp32Authenticator
     ↓
Future BLE implementation
     ↓
ESP32
     ↓
ESP32 authentication response
     ↓
Android authentication decision
```

The `Esp32Authenticator` interface in `bluetooth/` is the only seam
that needs a real implementation. `BluetoothServiceProvider` is the
single place to swap the placeholder for the real BLE service.

## 14. License

This is a student / portfolio project. Use it as inspiration for your
own security projects. Do not ship it unmodified without reviewing
the security model.
