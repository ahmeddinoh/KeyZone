# 🔐 CipherVault

An offline, zero-knowledge Android file encryption vault and storage manager built with **Kotlin**, **Jetpack Compose**, and **Material 3**.

---

## 🌟 Key Architecture & Security Features

### 1. Key-Derived Encryption (Not Just a Lock Screen)
- **Zero-Knowledge Architecture:** Your PIN or password **IS** the encryption key. Data cannot be decrypted or recovered without it.
- **PBKDF2 Key Derivation:** Uses `PBKDF2WithHmacSHA256` with **150,000 iterations** and a 32-byte cryptographically secure random salt to derive a 256-bit AES wrapping key.
- **AES-256-GCM Master Key:** A high-entropy 256-bit master key is generated for file encryption and stored wrapped (encrypted) with the user's derived key.
- **Verification Token:** Uses an authenticated ciphertext token to immediately verify the PIN/password on unlock without touching stored files.
- **Key Rotation:** Change your PIN/password at any time — re-wraps the master key without needing to re-encrypt all files.
- **Memory Security:** Plaintext keys reside only in ephemeral memory while unlocked and are actively wiped (`fill(0)`) when the vault is locked.

---

### 2. Standalone Offline RFC 6238 TOTP 2FA Engine
- **100% Offline Operation:** Operates completely air-gapped with zero network calls. All calculations rely strictly on the local device system clock.
- **Compatible with Google Authenticator:** Compliant with RFC 6238 and RFC 4226.
  - Secret: 160-bit (20-byte) cryptographically secure random seed encoded in RFC 4648 Base32.
  - Standard URI format: `otpauth://totp/CipherVault:Vault?secret={SECRET}&issuer=CipherVault&algorithm=SHA1&digits=6&period=30`.
- **Integrated QR Code Generator:** Uses ZXing entirely offline to render the QR code directly onto a Compose Canvas / Bitmap for instant scanning.
- **Clock Drift Tolerance:** Enforces a ±1 time step tolerance window (current step, -30s, +30s) to account for minor clock discrepancies.
- **Dynamic Truncation:** RFC 4226 §5.4 HMAC-SHA1 dynamic 4-byte extraction reduced to a 6-digit decimal code.
- **Replay Protection:** Tracks the last successfully verified counter/timestamp to prevent reused codes within the same time window.
- **Brute-Force Rate Limiting:** Consecutive failed verification attempts trigger an automatic 30-second lockout after 5 failures.
- **Hardware-Backed Secret Storage:** Local TOTP secret is stored encrypted inside the **Android KeyStore** hardware security module.
- **Emergency Recovery Codes:** Generates 8 single-use backup recovery codes (`XXXXX-XXXXX`). Only SHA-256 hashes are stored; codes are permanently consumed upon use.

---

### 3. Multi-Device Vault Backup & Import Flow
- **Export (.cvault Archive):** Exports all encrypted files alongside a structured manifest and the master-key-encrypted TOTP secret for cross-device portability.
- **Import Flow:**
  1. **Select Backup File:** Inspects `.cvault` metadata.
  2. **Enter Vault Key:** Asks for the PIN or Password protecting the vault and verifies derivation.
  3. **Verify 2FA:** Prompts for the 6-digit code from Google Authenticator (or an emergency recovery code).
  4. **Permanent Activation:** Once validated, the vault is imported. On future launches, the app **only asks for the PIN/password key** to unlock the vault, not the Authenticator 2FA code.

---

## 📱 App Screens & Navigation

```
Welcome (First Run)
 ├── Create Vault ──> Setup Screen (Choose PIN or Password) ──> Main App
 └── Import Vault ──> Import Screen (Select .cvault -> Key -> 2FA) ──> Main App

Locked (Subsequent Runs)
 └── Lock Screen (Enter PIN or Password Key) ──> Main App

Main App (Authenticated)
 ├── Home (Dashboard, stats, quick actions, recent files)
 ├── Vault (Searchable encrypted files, decrypt & open, delete)
 ├── Encrypt (System file picker, batch selection & AES-256-GCM encryption)
 ├── Settings (Change Key, Clear Cache, Lock Vault)
 └── Backup & 2FA (TOTP enrollment QR code, verify 2FA, export .cvault)
```

---

## 🛠 Tech Stack

- **Language:** Kotlin 1.9.22
- **UI:** Jetpack Compose + Material 3 (Dark Cybersecurity Theme)
- **Database:** Room 2.6.1 (metadata storage)
- **Crypto:** Android KeyStore, `javax.crypto` (AES-256-GCM, PBKDF2WithHmacSHA256, HmacSHA1)
- **2FA Engine:** Custom standalone RFC 6238 TOTP + RFC 4648 Base32 + ZXing QR
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 (Android 14)

---

## 🏗 Building & Running

1. Open Android Studio (Hedgehog 2023.1.1+ recommended).
2. Open the project directory: `scratch/CipherVault`.
3. Allow Gradle to sync.
4. Run on an Android device or emulator with API 26+.

```bash
./gradlew assembleDebug
```
