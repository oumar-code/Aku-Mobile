# Mobile — Aku-Mobile

> **Repository:** [oumar-code/Aku-Mobile](https://github.com/oumar-code/Aku-Mobile)
> The KMP mobile codebase has been migrated from Akulearn_docs into its own dedicated
> repository. See the [README](https://github.com/oumar-code/Aku-Mobile#readme) for
> getting-started instructions.

---

## Overview

Aku-Mobile delivers the Android and iOS applications for the Akulearn platform using
**Kotlin Multiplatform (KMP)**. Shared business logic (authentication, API clients) lives
in the `shared` module while each platform hosts its own thin UI layer.

## Structure

| Layer | Location | Description |
|-------|----------|-------------|
| Shared logic | `KOTLIN MULTIPLATFORM/shared/` | KMP common code, auth, API |
| Android app | `KOTLIN MULTIPLATFORM/androidApp/` | Android entry point |
| iOS targets | `KOTLIN MULTIPLATFORM/shared/src/iosMain/` | iOS-specific implementations |

## Key Modules

- **`com.akuplatform.shared.api`** — `Wave3ApiClient` for REST calls.
- **`com.akuplatform.shared.auth`** — `AuthRepository`, `SessionManager`, `TokenStorage`, `AuthToken`.

## Getting Started

See the root [README](https://github.com/oumar-code/Aku-Mobile#getting-started) for
prerequisites and build instructions.
