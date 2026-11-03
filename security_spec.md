# Jayra Firestore Security Specification (Phase 15)

## 1. Data Invariants
1. **Default-Deny Catch-All**: Every path not explicitly matched is denied (`allow read, write: if false`).
2. **PII Ownership Isolation (`/users/{userId}`)**: User documents store PII (`email`, `phoneNumber`, `country`). Only `request.auth.uid == userId` can `get`, `create`, `update`, or `delete`, and `list` requires `resource.data.userId == request.auth.uid`.
3. **Immutable Identity & Creation Timestamp**: `userId` and `createdAt` cannot be modified after creation; `createdAt` and `updatedAt` must be valid Firestore `timestamp` values `<= request.time` (`FieldValue.serverTimestamp()`).
4. **Strict Key Allowlists & Size Bounds**: Every `create` and `update` validates allowed keys (`hasOnly`) and bounds every string/list `.size()` to prevent shadow fields and Denial-of-Wallet payloads.
5. **Relational & Participant Boundaries**: Chats, messages, groups, channels, calls, call signals, sessions, and assistant chat messages enforce strict participant or owner UID checks without blanket `isSignedIn()` reads.

## 2. Dirty Dozen Adversarial Payloads
1. **Unauthenticated Read**: `GET /users/alice_123` with `auth = null` -> REJECT.
2. **Cross-User PII Read**: `GET /users/bob_456` as `alice_123` -> REJECT.
3. **Identity Spoofing on Create**: `CREATE /users/alice_123` with `{ userId: "bob_456", ... }` -> REJECT.
4. **Path ID Poisoning**: `CREATE /users/bad$id!` -> REJECT via `isValidId`.
5. **Shadow / Ghost Field Injection**: `CREATE /users/alice_123` with `{ ..., isAdmin: true }` -> REJECT via `hasOnly`.
6. **Shadow Update Injection**: `UPDATE /users/alice_123` adding `{ isVerified: true }` -> REJECT via `affectedKeys().hasOnly`.
7. **Immutable Field Tampering**: `UPDATE /users/alice_123` mutating `createdAt` or `userId` -> REJECT.
8. **Type / Value Poisoning**: `UPDATE /users/alice_123` setting `phoneNumber: 12345` or 10KB string -> REJECT via `isValidUserAccount`.
9. **Epoch Timestamp Spoofing**: `CREATE /users/alice_123` with `createdAt: 1700000000000` (int instead of server Timestamp) -> REJECT.
10. **Unfiltered Collection Scraping**: `LIST /users` without `.where("userId", "==", uid)` -> REJECT.
11. **Cross-User Chat Scraping**: `GET /chats/bob_chat` where `alice_123` is not in `participants` -> REJECT.
12. **Cross-User Assistant History Write**: `CREATE /assistant_chats/bob_456/messages/m1` as `alice_123` -> REJECT.
