# Changelog

All notable changes to **ESP32 Secure Lock** are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

---

## [1.0.2] - 2026-08-12

### Fixed
- **Lifecycle / background lock**: replaced the activity-level
  `onStop` / `onStart` + `wasStopped` flag in `MainActivity` with a
  `ProcessLifecycleOwner` observer. The new approach only re-locks
  when the *process* is sent to the background, so configuration
  changes (rotation, dark-mode toggle, font scale, locale) and
  transient activity recreation no longer trigger a spurious
  re-authentication. The observer is added in `onCreate` and removed
  in `onDestroy` so it does not leak. The previous code worked by
  accident (activity instance was recreated) and its comments
  incorrectly claimed it excluded configuration changes; both the
  implementation and the comments now match reality.
- **Change-passcode error handling**: a failed `changePasscode()`
  no longer misreports a secure-storage failure as "Incorrect
  passcode." The repository now returns a typed
  `ChangePasscodeResult` (`Success`, `IncorrectCurrent`,
  `StorageFailure`, `Unexpected`) and the ViewModel maps each case
  to a distinct `ErrorKind` (`INCORRECT`, `STORAGE`, `UNEXPECTED`).
- **Remove-passcode error handling**: same fix applied to
  `removePasscode()` via a `RemovePasscodeResult` sealed class.

### Added
- New `ErrorKind.UNEXPECTED` mapped to a dedicated user-facing
  message ("An unexpected error occurred…").
- New unit tests:
  - `AppViewModelPasscodeMutationTest` — verifies that
    `changePasscode` and `removePasscode` distinguish INCORRECT
    vs STORAGE vs UNEXPECTED, and that storage failures never
    appear as "Incorrect passcode."
  - `AppViewModelLifecycleLockTest` — verifies
    `onProcessBackgrounded` re-locks only when appropriate and
    clears transient UI state.

### Security
- No behavioural change to PBKDF2 / constant-time verification /
  random salt / encrypted persistence. The hardening is around
  *error mapping*: a wrong passcode and a storage failure are now
  reported as distinct, accurate errors instead of being collapsed.

---

## [1.0.1] - 2026-08-12

### Fixed
- **Validation / UI mismatch**: the Compose `PasscodeField` previously
  capped user input to 16 digits while `PasscodeValidator` accepted up
  to 32. The field now uses `PasscodeValidator.MAX_LENGTH` (32) as its
  cap, so the UI and the validator enforce the same boundary.
- **Wrong error messages**: `ErrorMessages` was mapping
  `TOO_LONG → "too short"` and `NOT_NUMERIC → "empty"`. Each
  `ErrorKind` now resolves to its own dedicated string. New string
  resources: `error_too_long`, `error_not_numeric`.
- **Secure storage fallback**: `PasscodeStore` previously fell back
  silently to an in-memory `SharedPreferences` when
  `EncryptedSharedPreferences` could not be initialised. It now throws
  `SecureStorageUnavailableException`, which the ViewModel surfaces as
  `ErrorKind.STORAGE` and the UI as a clear, actionable error. The
  passcode is never persisted insecurely.
- **Lifecycle / background lock**: `MainActivity` now re-locks the app
  when the user brings it back from a fully-stopped state (the system
  "Stop" event), without looping on recomposition or rotation. Documented
  in the README.

### Added
- New unit tests for: 3/4/32/33-digit boundaries, letters, symbols,
  whitespace, 32-digit hash round-trip, distinct error messages per
  `ErrorKind`, and `STORAGE` error surfacing from the ViewModel.
- `SecureStorageUnavailableException` typed exception.

### Security
- No behavioural change to PBKDF2 / constant-time verification / salt
  generation. The hardening is around *failure* handling: insecure
  fallbacks removed.

---

## [1.0.0] - 2026-08-12

### Added
- First-launch passcode **Setup** screen with confirmation and validation.
- **App lock** screen on every cold start when a passcode exists.
- **Main dashboard** with status card, ESP32 placeholder, and
  change/remove/lock actions.
- **Change passcode** dialog (re-auth + new + confirm).
- **Remove passcode** dialog (confirmation + re-auth).
- **Encrypted passcode storage** using `EncryptedSharedPreferences`
  + PBKDF2-HMAC-SHA256 (120 000 iterations, 16-byte random salt,
  256-bit derived key).
- **Bluetooth abstraction** (`Esp32Authenticator`) and a placeholder
  implementation that reports "ESP32 connection: Not configured".
- **Unit tests** for hashing, validation, repository behaviour, and
  ViewModel state transitions.
- **README.md** and **CHANGELOG.md**.

### Security
- Plain-text passcode is never written to disk or logs.
- PBKDF2 input `char[]` is wiped after derivation.
- Constant-time comparison of stored vs candidate hash.
- No default passcode, no hard-coded secrets, no analytics.

### Known limitations
- v1 does not implement the actual Bluetooth / ESP32 connection.
- v1 does not implement rate limiting or account lockout.
- v1 does not modify the Android system lock screen.
